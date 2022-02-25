package it.pagopa.pm.gateway.client.bpay;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.exception.BancomatPayClientException;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;
import java.lang.Exception;
import java.math.BigDecimal;

public class BancomatPayClient {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Value("${bancomatPay.client.user.code}")
    public static String USER_CODE;
    @Value("${bancomatPay.client.group.code}")
    public static String GROUP_CODE;
    @Value("${bancomatPay.client.institute.code}")
    public static String INSTITUTE_CODE;
    @Value("${bancomatPay.client.tag}")
    public static String TAG;
    @Value("${bancomatPay.client.guid}")
    public static String GUID;
    @Value("${bancomatPay.client.token}")
    public static String TOKEN;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Async
    public InserimentoRichiestaPagamentoPagoPaResponse sendPaymentRequest(BPayPaymentRequest request) throws BancomatPayClientException {
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
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getEncryptedTelephoneNumber());
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
