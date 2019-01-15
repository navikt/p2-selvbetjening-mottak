package no.nav.foreldrepenger.lookup.rest.sak;

import static java.util.stream.Collectors.toCollection;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;

import io.github.resilience4j.retry.Retry;
import no.nav.foreldrepenger.lookup.TokenHandler;
import no.nav.foreldrepenger.lookup.util.RetryUtil;
import no.nav.security.spring.oidc.validation.interceptor.BearerTokenClientHttpRequestInterceptor;

@Configuration
public class SakConfiguration {

    private static final String SAK_RETRY_CONFIG = "sakRetryConfig";
    private static final String STS_RETRY_CONFIG = "stsRetryConfig";

    @Value("${SAK_SAKER_URL}")
    private URI sakBaseUrl;

    @Value("${SECURITYTOKENSERVICE_URL}")
    private URI stsUrl;

    @Value("${FPSELVBETJENING_USERNAME}")
    private String serviceUser;

    @Value("${FPSELVBETJENING_PASSWORD}")
    private String servicePwd;

    @Bean
    public SakClientHttp sakClient(RestOperations restOperations, StsClient stsClient, TokenHandler tokenHandler,
            @Qualifier(SAK_RETRY_CONFIG) Retry retry) {
        return new SakClientHttp(sakBaseUrl, restOperations, stsClient, tokenHandler, retry);
    }

    @Bean
    public RestOperations restTemplateSak(TokenHandler tokenHandler, ClientHttpRequestInterceptor... interceptors) {
        List<ClientHttpRequestInterceptor> interceptorListWithoutAuth = Arrays.stream(interceptors)
                // We'll add our own auth header with SAML elsewhere
                .filter(i -> !(i instanceof BearerTokenClientHttpRequestInterceptor))
                .collect(toCollection(ArrayList::new));

        ClientHttpRequestInterceptor[] interceptorsAsArray = interceptorListWithoutAuth.stream()
                .toArray(ClientHttpRequestInterceptor[]::new);

        return new RestTemplateBuilder()
                .interceptors(interceptorsAsArray)
                .build();
    }

    @Bean
    public StsClient stsClient(RestOperations restTemplate, @Qualifier(STS_RETRY_CONFIG) Retry retry) {
        return new StsClient(restTemplate, stsUrl, serviceUser, servicePwd, retry);
    }

    @Bean
    @Qualifier(SAK_RETRY_CONFIG)
    public Retry sakRetry(@Value("${retry.sak.max:2}") int max) {
        return RetryUtil.retry(max, "saker", HttpServerErrorException.class,
                LoggerFactory.getLogger(SakClientHttp.class));
    }

    @Bean
    @Qualifier(STS_RETRY_CONFIG)
    public Retry stsRetry(@Value("${retry.sts.max:2}") int max) {
        return RetryUtil.retry(max, "STS", HttpServerErrorException.class, LoggerFactory.getLogger(StsClient.class));
    }

}
