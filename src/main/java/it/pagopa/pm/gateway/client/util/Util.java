package it.pagopa.pm.gateway.client.util;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;


public class Util {

    private static String JIFFY_HOSTNAME = "JIFFY_HOSTNAME";

    public Jaxb2Marshaller jaxb2Marshaller() throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setContextPath("it.sempla.p2p.srv.cc");
        jaxb2Marshaller.afterPropertiesSet();

        return jaxb2Marshaller;
    }


    public WebServiceTemplate webServiceTemplate() throws Exception {
        String path = "http://p2b.gft.it/srv/pp/inserimentoRichiestaPagamentoPagoPa";
        if (path == null) {
            throw new Exception("System properties JIFFY_HOSTNAME not found");
        }

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(path);
        return webServiceTemplate;
    }
}
