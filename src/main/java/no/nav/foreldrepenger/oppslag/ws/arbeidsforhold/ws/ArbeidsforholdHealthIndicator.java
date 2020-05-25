package no.nav.foreldrepenger.oppslag.ws.arbeidsforhold.ws;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import no.nav.foreldrepenger.oppslag.rest.PingableHealthIndicator;
import no.nav.foreldrepenger.oppslag.ws.arbeidsforhold.ArbeidsforholdTjeneste;

@Component
public class ArbeidsforholdHealthIndicator extends PingableHealthIndicator {

    private final ArbeidsforholdTjeneste client;

    public ArbeidsforholdHealthIndicator(ArbeidsforholdTjeneste client,
            @Value("${VIRKSOMHET_ARBEIDSFORHOLD_V3_ENDPOINTURL}") URI serviceUrl) {
        super(serviceUrl);
        this.client = client;
    }

    @Override
    public void ping() {
        client.ping();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[client=" + client + "]";
    }
}