package no.nav.foreldrepenger.oppslag.ws.arbeidsforhold;

import static io.github.resilience4j.retry.Retry.decorateSupplier;
import static java.util.stream.Collectors.joining;
import static no.nav.foreldrepenger.oppslag.util.RetryUtil.DEFAULT_RETRIES;

import java.util.Optional;

import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.retry.Retry;
import no.nav.foreldrepenger.oppslag.error.TokenExpiredException;
import no.nav.foreldrepenger.oppslag.util.RetryUtil;
import no.nav.foreldrepenger.oppslag.util.TokenUtil;
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5;
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentOrganisasjonResponse;

public class OrganisasjonClientWs implements OrganisasjonClient {

    private static final Logger LOG = LoggerFactory.getLogger(OrganisasjonClientWs.class);

    private OrganisasjonV5 organisasjonV5;
    private OrganisasjonV5 healthIndicator;
    private final TokenUtil tokenHandler;
    private final Retry retry;

    OrganisasjonClientWs(OrganisasjonV5 organisasjonV5, OrganisasjonV5 healthIndicator,
            TokenUtil tokenHandler) {
        this(organisasjonV5, healthIndicator, tokenHandler, retry());
    }

    public OrganisasjonClientWs(OrganisasjonV5 organisasjonV5, OrganisasjonV5 healthIndicator,
            TokenUtil tokenHandler, Retry retry) {
        this.organisasjonV5 = organisasjonV5;
        this.healthIndicator = healthIndicator;
        this.tokenHandler = tokenHandler;
        this.retry = retry;
    }

    @Override
    public void ping() {
        LOG.info("Pinger organisasjonstjenesten");
        healthIndicator.ping();
    }

    @Override
    public Optional<String> nameFor(String orgnr) {
        if (orgnr.length() != 9) {
            LOG.warn("{} ser ikke ut som et organisasjonsnummer, slår ikke opp navn", orgnr);
            return Optional.empty();
        }
        return decorateSupplier(retry, () -> doGetNameFor(orgnr)).get();
    }

    private Optional<String> doGetNameFor(String orgnr) {
        try {
            HentOrganisasjonRequest request = new HentOrganisasjonRequest();
            request.setOrgnummer(orgnr);
            final HentOrganisasjonResponse response = organisasjonV5.hentOrganisasjon(request);
            return Optional.ofNullable(name(response.getOrganisasjon().getNavn()));
        } catch (HentOrganisasjonUgyldigInput e) {
            LOG.warn("Ugyldig input ved oppslag av navn for organisasjon {}", orgnr, e);
            return Optional.empty();
        } catch (HentOrganisasjonOrganisasjonIkkeFunnet e) {
            LOG.warn("Fant ikke navn for organisasjon {}", orgnr, e);
            return Optional.empty();
        } catch (SOAPFaultException e) {
            if (tokenHandler.isExpired()) {
                throw new TokenExpiredException(tokenHandler.getExpiryDate(), e);
            }
            throw e;
        }
    }

    private static String name(SammensattNavn sammensattNavn) {
        return ((UstrukturertNavn) sammensattNavn).getNavnelinje()
                .stream()
                .filter(OrganisasjonClientWs::isNotEmpty)
                .collect(joining(", "));
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private static Retry retry() {
        return RetryUtil.retry(DEFAULT_RETRIES, "organisasjon", SOAPFaultException.class, LOG);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [organisasjonV5=" + organisasjonV5 + ", healthIndicator="
                + healthIndicator + ", tokenHandler=" + tokenHandler + ", retry=" + retry + "]";
    }

}