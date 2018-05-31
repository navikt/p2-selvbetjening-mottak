package no.nav.foreldrepenger.oppslag.lookup.ws;

import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.springframework.stereotype.Component;

import no.nav.foreldrepenger.oppslag.lookup.UUIDCallIdGenerator;

@Component
public class WsClient<T> {

    @Inject
    private EndpointSTSClientConfig endpointStsClientConfig;

    @Inject
    UUIDCallIdGenerator generator;
    @Inject
    private OnBehalfOfOutInterceptor onBehalfOfOutInterceptor;

    @SuppressWarnings("unchecked")
    public T createPort(String serviceUrl, Class<?> portType, PhaseInterceptor<? extends Message>... interceptors) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(portType);
        jaxWsProxyFactoryBean.setAddress(Objects.requireNonNull(serviceUrl));
        T port = (T) jaxWsProxyFactoryBean.create();
        Client client = ClientProxy.getClient(port);
        Arrays.stream(interceptors).forEach(client.getOutInterceptors()::add);
        client.getOutInterceptors().add(new CallIdHeader(generator));

        endpointStsClientConfig.configureRequestSamlTokenOnBehalfOfOidc(port, onBehalfOfOutInterceptor);
        return port;
    }

}