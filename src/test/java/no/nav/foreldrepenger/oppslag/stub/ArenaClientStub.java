package no.nav.foreldrepenger.oppslag.stub;

import no.nav.foreldrepenger.oppslag.lookup.ws.ytelser.arena.ArenaClient;
import no.nav.foreldrepenger.oppslag.lookup.ws.person.Fodselsnummer;
import no.nav.foreldrepenger.oppslag.lookup.ws.ytelser.Ytelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ArenaClientStub implements ArenaClient {

    private static final Logger LOG = LoggerFactory.getLogger(ArenaClientStub.class);


    @Override
    public void ping() {
        LOG.debug("PONG");
    }

    @Override
    public List<Ytelse> ytelser(Fodselsnummer fnr, LocalDate from, LocalDate to) {
        return new ArrayList<>();
    }
}