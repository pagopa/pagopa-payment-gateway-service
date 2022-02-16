package it.pagopa.pm.gateway.client.payment.gateway.client;

import it.pagopa.pm.gateway.client.*;
import it.pagopa.pm.gateway.dto.BancomatPayPaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.JAXBElement;
import java.lang.Exception;
import java.math.BigDecimal;

public class BancomatPayClientV2 {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Value("${bancomatPay.client.user.code}")
    private String userCode;

    @Value("${bancomatPay.client.group.code}")
    private String groupCode;

    @Value("${bancomatPay.client.institute.code}")
    private String instituteCode;

    @Value("${bancomatPay.client.tag}")
    private String tag;

    @Value("${bancomatPay.client.guid}")
    private String guid;

    @Value("${bancomatPay.client.token}")
    private String token;

    @Async
    public InserimentoRichiestaPagamentoPagoPaResponse getInserimentoRichiestaPagamentoPagoPaResponse(BancomatPayPaymentRequest request) {

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
        richiestaPagamentoPagoPaVO.setIdPagoPa(request.getIdPagoPa());
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(request.getAmount()));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getCryptedTelephoneNumber());
        richiestaPagamentoPagoPaVO.setCausale(request.getSubject());
        richiestaPagamentoPagoPaVO.setTag(tag);

        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);
        inserimentoRichiestaPagamentoPagoPa.setArg0(requestInserimentoRichiestaPagamentoPagoPaVO);

        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);

        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();

        return inserimentoRichiestaPagamentoPagoPaResponse;


    }

}
