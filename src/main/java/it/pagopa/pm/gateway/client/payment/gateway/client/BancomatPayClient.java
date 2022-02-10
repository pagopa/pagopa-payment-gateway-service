package it.pagopa.pm.gateway.client.payment.gateway.client;

import it.pagopa.pm.gateway.client.ObjectFactory;
import it.pagopa.pm.gateway.client.util.Util;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;

public class BancomatPayClient {


    public void call() throws Exception {

   /* ObjectFactory objectFactory = new ObjectFactory();

    RecuperaInfoUtenze recuperaInfoUtenze = new RecuperaInfoUtenze();
    RequestRecuperaInfoUtenzeVO requestRecuperaInfoUtenzeVO = new RequestRecuperaInfoUtenzeVO();
        requestRecuperaInfoUtenzeVO.setCodiceFiscale("546132156486");
        recuperaInfoUtenze.setArg0(requestRecuperaInfoUtenzeVO);

    JAXBElement<RecuperaInfoUtenze> objectFactoryRecuperaInfoUtenze = objectFactory.createRecuperaInfoUtenze(recuperaInfoUtenze);

    Util util = new Util();
    WebServiceTemplate webServiceTemplate = util.webServiceTemplate();

    JAXBElement<RecuperaInfoUtenzeResponse> recuperaInfoUtenzeResponseJAXBElement = (JAXBElement<RecuperaInfoUtenzeResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryRecuperaInfoUtenze);
    RecuperaInfoUtenzeResponse recuperaInfoUtenzeResponse = recuperaInfoUtenzeResponseJAXBElement.getValue(); */

}


}
