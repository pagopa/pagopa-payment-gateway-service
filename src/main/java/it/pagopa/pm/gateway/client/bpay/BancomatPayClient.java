package it.pagopa.pm.gateway.client.bpay;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.*;
import it.pagopa.pm.gateway.utils.ClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.xml.bind.*;
import java.math.BigDecimal;

@Slf4j
public class BancomatPayClient {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Value("${bancomatPay.client.group.code}")
    public String GROUP_CODE;
    @Value("${bancomatPay.client.institute.code}")
    public String INSTITUTE_CODE;
    @Value("${bancomatPay.client.tag}")
    public String TAG;
    @Value("${bancomatPay.client.token}")
    public String TOKEN;

    private final ObjectFactory objectFactory = new ObjectFactory();

    public InserimentoRichiestaPagamentoPagoPaResponse sendPaymentRequest(BPayPaymentRequest request, String guid) {
        log.info("START sendPaymentRequest");
        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(createContesto(guid, request.getLanguage()));
        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP(request.getIdPsp()!=null? ClientUtil.intesaSPCodiceAbi:null);
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
        inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
        inserimentoRichiestaPagamentoPagoPaResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();
        log.info("END sendPaymentRequest");
        return inserimentoRichiestaPagamentoPagoPaResponse;
    }

    public ResponseStornoPagamentoVO sendRefundRequest(BPayRefundRequest request, String guid) {
        log.info("START sendRefundRequest");
        RequestStornoPagamentoVO requestStornoPagamentoVO = new RequestStornoPagamentoVO();
        requestStornoPagamentoVO.setContesto(createContesto(guid, request.getLanguage()));
        requestStornoPagamentoVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        requestStornoPagamentoVO.setCausale(request.getSubject());
        requestStornoPagamentoVO.setEndToEndId(request.getCorrelationId());
        ResponseStornoPagamentoVO responseStornoPagamentoVO = (ResponseStornoPagamentoVO) webServiceTemplate.marshalSendAndReceive(requestStornoPagamentoVO);
        log.info("END sendRefundRequest");
        return responseStornoPagamentoVO;
    }

    private ContestoVO createContesto(String guid, String language) {
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid(guid);
        contestoVO.setToken(TOKEN);
        contestoVO.setLingua(LinguaEnum.fromValue(ClientUtil.getLanguageCode(language)));
        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente(null);
        utenteVO.setCodGruppo(GROUP_CODE);
        utenteVO.setCodIstituto(INSTITUTE_CODE);
        contestoVO.setUtenteAttivo(utenteVO);
        return contestoVO;
    }

}
