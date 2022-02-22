package it.pagopa.pm.gateway.client.payment.gateway.client;

import it.pagopa.pm.gateway.client.wsdl.generated.files.*;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import java.lang.Exception;
import java.math.BigDecimal;

import static it.pagopa.pm.gateway.client.util.Constants.*;

public class BancomatPayClientV2 {

    @Autowired
    private WebServiceTemplate bancomatPayWebServiceTemplate;

    @Async
    public InserimentoRichiestaPagamentoPagoPaResponse getInserimentoRichiestaPagamentoPagoPaResponse(BancomatPayPaymentRequest request) throws BancomatPayClientException {

        ObjectFactory objectFactory = new ObjectFactory();

        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid(guid);
        contestoVO.setToken(token);
        contestoVO.setLingua(LinguaEnum.fromValue(request.getLanguage()));

        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente(userCode);
        utenteVO.setCodGruppo(groupCode);
        utenteVO.setCodIstituto(instituteCode);
        contestoVO.setUtenteAttivo(utenteVO);

        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(contestoVO);

        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP(request.getIdPsp());
        richiestaPagamentoPagoPaVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(request.getAmount()));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getCryptedTelephoneNumber());
        richiestaPagamentoPagoPaVO.setCausale(request.getSubject());
        richiestaPagamentoPagoPaVO.setTag(tag);

        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);
        inserimentoRichiestaPagamentoPagoPa.setArg0(requestInserimentoRichiestaPagamentoPagoPaVO);

        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = null;
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse = null;

        try {
            inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) bancomatPayWebServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
            inserimentoRichiestaPagamentoPagoPaResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();
        } catch (Exception e){
                throw new BancomatPayClientException(ExceptionsEnum.BPAY_SERVICE_REQUEST_ERROR.getRestApiCode(), ExceptionsEnum.BPAY_SERVICE_REQUEST_ERROR.getDescription()) ;
        }

        return inserimentoRichiestaPagamentoPagoPaResponse;


    }

    public WebServiceTemplate getBancomatPayWebServiceTemplate(){
        return bancomatPayWebServiceTemplate;
    }


}
