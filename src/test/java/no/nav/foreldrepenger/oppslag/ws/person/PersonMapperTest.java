package no.nav.foreldrepenger.oppslag.ws.person;

import static no.nav.foreldrepenger.oppslag.ws.person.PersonMapper.person;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.oppslag.util.DateUtil;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.BankkontoNorge;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.BankkontoUtland;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bankkontonummer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.BankkontonummerUtland;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Spraak;

public class PersonMapperTest {

    @Test
    public void basePerson() {
        var id = new Fødselsnummer("123456378910");
        no.nav.foreldrepenger.oppslag.ws.person.Person mapped = person(id, createBasePerson());
        assertEquals("123456378910", mapped.getFnr().getFnr());
        assertEquals("Diego", mapped.getNavn().getFornavn());
        assertEquals("Armando", mapped.getNavn().getMellomnavn());
        assertEquals("Maradona", mapped.getNavn().getEtternavn());
    }

    @Test
    public void withMålform() {
        var id = new Fødselsnummer("123456378910");
        Bruker tpsPerson = personWithMålform();
        no.nav.foreldrepenger.oppslag.ws.person.Person mapped = person(id, tpsPerson);
        assertEquals("Turkmenistansk", mapped.getMålform());
    }

    @Test
    public void withNorskKonto() {
        var id = new Fødselsnummer("123456378910");
        Bruker tpsPerson = personWithNorskKonto();
        no.nav.foreldrepenger.oppslag.ws.person.Person mapped = person(id, tpsPerson);
        Bankkonto expected = new Bankkonto("1234567890", "Ripoff inc.");
        assertEquals(expected, mapped.getBankkonto());
    }

    @Test
    public void withUtenlandskKonto() {
        var id = new Fødselsnummer("123456378910");
        Bruker tpsPerson = personWithUtenlandskKonto();
        Person mapped = person(id, tpsPerson);
        Bankkonto expected = new Bankkonto("swiftster", "bankkode");
        assertEquals(expected, mapped.getBankkonto());
    }

    private static Bruker createBasePerson() {
        Bruker person = new Bruker();

        Kjoenn kjoenn = new Kjoenn();
        Kjoennstyper kjoennstyper = new Kjoennstyper();
        kjoennstyper.setValue("K");
        kjoenn.setKjoenn(kjoennstyper);
        person.setKjoenn(kjoenn);

        Personnavn navn = new Personnavn();
        navn.setFornavn("Diego");
        navn.setMellomnavn("Armando");
        navn.setEtternavn("Maradona");
        person.setPersonnavn(navn);

        Gateadresse gateAdresse = new Gateadresse();
        gateAdresse.setGatenavn("Veien");
        gateAdresse.setHusnummer(42);
        gateAdresse.setTilleggsadresseType("OFFISIELL ADRESSE");
        Postnummer postnummer = new Postnummer();
        postnummer.setValue("0175");
        gateAdresse.setPoststed(postnummer);
        Landkoder landkoder = new Landkoder();
        landkoder.setValue("NO");
        gateAdresse.setLandkode(landkoder);
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setStrukturertAdresse(gateAdresse);
        person.setBostedsadresse(bostedsadresse);

        Foedselsdato foedselsdato = new Foedselsdato();
        foedselsdato.setFoedselsdato(DateUtil.toXMLGregorianCalendar(LocalDate.now()));
        person.setFoedselsdato(foedselsdato);

        return person;
    }

    private static Bruker personWithMålform() {
        Bruker basePerson = createBasePerson();
        Spraak spraak = new Spraak();
        spraak.setValue("Turkmenistansk");
        basePerson.setMaalform(spraak);
        return basePerson;
    }

    private static Bruker personWithNorskKonto() {
        Bruker basePerson = createBasePerson();
        basePerson.setBankkonto(norskKonto());
        return basePerson;
    }

    private static Bruker personWithUtenlandskKonto() {
        Bruker basePerson = createBasePerson();
        basePerson.setBankkonto(utenlandskKonto());
        return basePerson;
    }

    private static no.nav.tjeneste.virksomhet.person.v3.informasjon.Bankkonto norskKonto() {
        BankkontoNorge bankkonto = new BankkontoNorge();
        Bankkontonummer kontonr = new Bankkontonummer();
        kontonr.setBankkontonummer("1234567890");
        kontonr.setBanknavn("Ripoff inc.");
        bankkonto.setBankkonto(kontonr);
        return bankkonto;
    }

    private static no.nav.tjeneste.virksomhet.person.v3.informasjon.Bankkonto utenlandskKonto() {
        BankkontoUtland bankkonto = new BankkontoUtland();
        BankkontonummerUtland kontonr = new BankkontonummerUtland();
        kontonr.setSwift("swiftster");
        kontonr.setBankkode("bankkode");
        bankkonto.setBankkontoUtland(kontonr);
        return bankkonto;
    }

}
