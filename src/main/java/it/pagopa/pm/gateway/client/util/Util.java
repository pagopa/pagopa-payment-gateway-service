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

        //ENDPOINT UAT https://app-te.vaservices.eu:443/p2penginectx/F1/services/ConsultazioneCrossServices
        //ENDPOINT SIT https://app-te.vaservices.eu/sit-p2penginectx/F1/services/ConsultazioneCrossServices
        String customizedBasePath = System.getProperty(JIFFY_HOSTNAME);
        if (customizedBasePath == null) {
            throw new Exception("System properties JIFFY_HOSTNAME not found");
        }

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(jaxb2Marshaller());
        webServiceTemplate.setUnmarshaller(jaxb2Marshaller());
        webServiceTemplate.setDefaultUri(customizedBasePath);
        //webServiceTemplate.setDefaultUri(customizedBasePath + "sit-p2penginectx/F1/services/ConsultazioneCrossServices");

        // register the LogHttpHeaderClientInterceptor
       // ClientInterceptor[] interceptors =
         //       new ClientInterceptor[] {new CustomJiffyInterceptor(), new LogHttpHeaderClientInterceptor()};

       // webServiceTemplate.setInterceptors(interceptors);

        return webServiceTemplate;
    }
}
