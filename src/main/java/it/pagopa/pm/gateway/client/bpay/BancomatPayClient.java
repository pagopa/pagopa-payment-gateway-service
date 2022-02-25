package it.pagopa.pm.gateway.client.bpay;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;
import java.lang.Exception;
import java.math.BigDecimal;

import static it.pagopa.pm.gateway.client.util.Constants.*;

public class BancomatPayClient {


    @Autowired
    private WebServiceTemplate webServiceTemplate;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Async
    public InserimentoRichiestaPagamentoPagoPaResponse sendPaymentRequest(BancomatPayPaymentRequest request) throws BancomatPayClientException {
        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid(GUID);
        contestoVO.setToken(TOKEN);
        contestoVO.setLingua(LinguaEnum.fromValue(request.getLanguage()));

        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente(USER_CODE);
        utenteVO.setCodGruppo(GROUP_CODE);
        utenteVO.setCodIstituto(INSTITUTE_CODE);
        contestoVO.setUtenteAttivo(utenteVO);

        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(contestoVO);

        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP(request.getIdPsp());
        richiestaPagamentoPagoPaVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(request.getAmount()));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getCryptedTelephoneNumber());
        richiestaPagamentoPagoPaVO.setCausale(request.getSubject());
        richiestaPagamentoPagoPaVO.setTag(TAG);

        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);
        inserimentoRichiestaPagamentoPagoPa.setArg0(requestInserimentoRichiestaPagamentoPagoPaVO);

        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement;
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse;
        try {
            inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
            inserimentoRichiestaPagamentoPagoPaResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();
        } catch (Exception e){
            throw new BancomatPayClientException(ExceptionsEnum.BPAY_SERVICE_REQUEST_ERROR.getRestApiCode(), ExceptionsEnum.BPAY_SERVICE_REQUEST_ERROR.getDescription());
        }
        return inserimentoRichiestaPagamentoPagoPaResponse;
    }

}
