package no.nav.foreldrepenger.lookup.ws.person;

import static io.github.resilience4j.retry.Retry.decorateSupplier;
import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.lookup.util.EnvUtil.CONFIDENTIAL;
import static no.nav.foreldrepenger.lookup.util.RetryUtil.DEFAULT_RETRIES;
import static no.nav.foreldrepenger.lookup.ws.person.PersonMapper.barn;
import static no.nav.foreldrepenger.lookup.ws.person.PersonMapper.person;
import static no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.BANKKONTO;
import static no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.FAMILIERELASJONER;
import static no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov.KOMMUNIKASJON;

import java.util.List;
import java.util.Objects;

import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.retry.Retry;
import io.micrometer.core.annotation.Timed;
import no.nav.foreldrepenger.errorhandling.NotFoundException;
import no.nav.foreldrepenger.errorhandling.TokenExpiredException;
import no.nav.foreldrepenger.errorhandling.UnauthorizedException;
import no.nav.foreldrepenger.lookup.util.RetryUtil;
import no.nav.foreldrepenger.lookup.util.TokenUtil;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

public class PersonClientTpsWs implements PersonClient {

    private static final Logger LOG = LoggerFactory.getLogger(PersonClientTpsWs.class);

    private final PersonV3 person;
    private final PersonV3 healthIndicator;
    private final Barnutvelger barnutvelger;
    private final TokenUtil tokenUtil;
    private final Retry retryConfig;

    PersonClientTpsWs(PersonV3 person, PersonV3 healthIndicator, TokenUtil tokenUtil,
            Barnutvelger barnutvelger) {
        this(person, healthIndicator, tokenUtil, barnutvelger, defaultRetryConfig());
    }

    public PersonClientTpsWs(PersonV3 person, PersonV3 healthIndicator, TokenUtil tokenUtil,
            Barnutvelger barnutvelger, Retry retryConfig) {
        this.person = Objects.requireNonNull(person);
        this.healthIndicator = healthIndicator;
        this.tokenUtil = tokenUtil;
        this.barnutvelger = Objects.requireNonNull(barnutvelger);
        this.retryConfig = retryConfig;
    }

    @Override
    public void ping() {
        try {
            LOG.info("Pinger TPS");
            healthIndicator.ping();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    @Timed("lookup.person")
    public Person hentPersonInfo(ID id) {
        HentPersonRequest request = PersonRequestUtil.request(id.getFnr(), KOMMUNIKASJON, BANKKONTO, FAMILIERELASJONER);
        LOG.info("Slår opp person");
        LOG.info(CONFIDENTIAL, "Fra ID {}", id);
        no.nav.tjeneste.virksomhet.person.v3.informasjon.Person tpsPerson = tpsPersonWithRetry(request);
        Person p = person(id, tpsPerson, barnFor(tpsPerson));
        LOG.info("Slo opp person OK");
        LOG.info(CONFIDENTIAL, "Person er {}", p);
        return p;
    }

    private no.nav.tjeneste.virksomhet.person.v3.informasjon.Person tpsPersonWithRetry(HentPersonRequest request) {
        return decorateSupplier(retryConfig, () -> hentPerson(request)).get().getPerson();
    }

    private List<Barn> barnFor(no.nav.tjeneste.virksomhet.person.v3.informasjon.Person person) {
        PersonIdent id = (PersonIdent) person.getAktoer();
        String idType = id.getIdent().getType().getValue();
        switch (idType) {
        case PersonRequestUtil.FNR:
        case PersonRequestUtil.DNR:
            Fødselsnummer fnrSøker = new Fødselsnummer(id.getIdent().getIdent());
            return person.getHarFraRolleI().stream()
                    .filter(this::isBarn)
                    .map(s -> hentBarn(s, fnrSøker))
                    .filter(Objects::nonNull)
                    .filter(barn -> barnutvelger.erStonadsberettigetBarn(fnrSøker, barn))
                    .collect(toList());
        default:
            throw new IllegalStateException("ID type " + idType + " ikke støttet");
        }
    }

    private boolean isBarn(Familierelasjon rel) {
        return rel.getTilRolle().getValue().equals(PersonRequestUtil.BARN);
    }

    private boolean isForelder(Familierelasjon rel) {
        String rolle = rel.getTilRolle().getValue();
        return rolle.equals(PersonRequestUtil.MOR) || rolle.equals(PersonRequestUtil.FAR);
    }

    private Barn hentBarn(Familierelasjon rel, Fødselsnummer fnrSøker) {
        NorskIdent id = ((PersonIdent) rel.getTilPerson().getAktoer()).getIdent();
        if (PersonRequestUtil.isFnr(id)) {
            Fødselsnummer fnrBarn = new Fødselsnummer(id.getIdent());
            no.nav.tjeneste.virksomhet.person.v3.informasjon.Person tpsBarn = tpsPersonWithRetry(
                    PersonRequestUtil.request(fnrBarn, FAMILIERELASJONER));

            AnnenForelder annenForelder = tpsBarn.getHarFraRolleI().stream()
                    .filter(this::isForelder)
                    .map(this::toFødselsnummer)
                    .filter(Objects::nonNull)
                    .filter(fnr -> !fnr.equals(fnrSøker))
                    .map(fnr -> tpsPersonWithRetry(PersonRequestUtil.request(fnr)))
                    .map(PersonMapper::annenForelder)
                    .findFirst()
                    .orElse(null);

            return barn(id, fnrSøker, tpsBarn, annenForelder);
        }
        return null;
    }

    private Fødselsnummer toFødselsnummer(Familierelasjon rel) {
        NorskIdent id = ((PersonIdent) rel.getTilPerson().getAktoer()).getIdent();
        if (PersonRequestUtil.isFnr(id)) {
            return new Fødselsnummer(id.getIdent());
        }
        return null;
    }

    private HentPersonResponse hentPerson(HentPersonRequest request) {
        try {
            return person.hentPerson(request);
        } catch (SOAPFaultException e) {
            if (tokenUtil.isExpired()) {
                throw new TokenExpiredException(tokenUtil.getExp(), e);
            }
            throw e;
        } catch (HentPersonPersonIkkeFunnet e) {
            LOG.warn("Fant ikke person", e);
            throw new NotFoundException(e);
        } catch (HentPersonSikkerhetsbegrensning e) {
            LOG.warn("Sikkerhetsbegrensning ved oppslag", e);
            throw new UnauthorizedException(e);
        }
    }

    private static Retry defaultRetryConfig() {
        return RetryUtil.retry(DEFAULT_RETRIES, "person", SOAPFaultException.class, LOG);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [person=" + person + ", healthIndicator=" + healthIndicator
                + ", barnutvelger=" + barnutvelger + ", tokenUtil=" + tokenUtil + ", retryConfig=" + retryConfig + "]";
    }

}
