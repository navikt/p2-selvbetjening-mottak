package no.nav.foreldrepenger.oppslag.aareg;

import no.nav.foreldrepenger.oppslag.ws.WsClient;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = { "no.nav.foreldrepenger.oppslag" })
public class AaregConfiguration {

	@SuppressWarnings("unchecked")
	@Bean
	public ArbeidsforholdV3 arbeidsforholdV3(
	        @Value("${VIRKSOMHET_ARBEIDSFORHOLD_V3_ENDPOINTURL}") String serviceUrl) {
		return new WsClient<ArbeidsforholdV3>().createPort(serviceUrl, ArbeidsforholdV3.class);
	}
}