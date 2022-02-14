package it.pagopa.pm.gateway.client.payment.gateway.client;

import it.pagopa.pm.gateway.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import java.lang.Exception;
import java.math.BigDecimal;

@Component
public class BancomatPayClientV2 {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    public InserimentoRichiestaPagamentoPagoPaResponse getInserimentoRichiestaPagamentoPagoPaResponse( )
            throws Exception {

        ObjectFactory objectFactory = new ObjectFactory();

        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid("guid");
        contestoVO.setToken("token");
        contestoVO.setLingua(LinguaEnum.IT);

        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente("codice Utente");
        utenteVO.setCodGruppo("codice Gruppo");
        utenteVO.setCodIstituto("codice Istituto");
        contestoVO.setUtenteAttivo(utenteVO);

        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(contestoVO);

        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP("idPsp");
        richiestaPagamentoPagoPaVO.setIdPagoPa("PROVOST");
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(100.0));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato("CRYPTO_TEL");
        richiestaPagamentoPagoPaVO.setCausale("CAUSALE");
        richiestaPagamentoPagoPaVO.setTag("TAG");


        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);
        inserimentoRichiestaPagamentoPagoPa.setArg0(requestInserimentoRichiestaPagamentoPagoPaVO);

        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);

        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponseResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();

        return inserimentoRichiestaPagamentoPagoPaResponseResponse;


    }

}
