package it.pagopa.pm.gateway.client.payment.gateway.client;

import it.pagopa.pm.gateway.client.*;
import it.pagopa.pm.gateway.client.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.xml.bind.JAXBElement;
import java.lang.Exception;

public class BancomatPayClientV2 extends WebServiceGatewaySupport {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    public InserimentoRichiestaPagamentoPagoPaResponse getInserimentoRichiestaPagamentoPagoPaResponse() throws Exception {

        ObjectFactory objectFactory = new ObjectFactory();

        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP("idPsp");
        richiestaPagamentoPagoPaVO.setIdPagoPa("idPagopa");
        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);

        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);

        //Util util = new Util();
        //WebServiceTemplate webServiceTemplate = util.webServiceTemplate();

        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponseResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();

        return inserimentoRichiestaPagamentoPagoPaResponseResponse;


    }

}
