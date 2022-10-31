package it.pagopa.pm.gateway.client.bpay;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayPaymentRequest;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayRefundRequest;
import it.pagopa.pm.gateway.utils.ClientUtils;
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
    public String groupCode;
    @Value("${bancomatPay.client.institute.code}")
    public String instituteCode;
    @Value("${bancomatPay.client.tag}")
    public String tag;
    @Value("${bancomatPay.client.token}")
    public String token;

    private final ObjectFactory objectFactory = new ObjectFactory();

    public InserimentoRichiestaPagamentoPagoPaResponse sendPaymentRequest(BPayPaymentRequest request, String guid) {
        log.info("START sendPaymentRequest");
        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(createContesto(guid, request.getLanguage()));
        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP(request.getIdPsp()!=null? ClientUtils.INTESA_SP_CODICE_ABI :null);
        richiestaPagamentoPagoPaVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(request.getAmount()));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getEncryptedTelephoneNumber());
        richiestaPagamentoPagoPaVO.setCausale(request.getSubject());
        richiestaPagamentoPagoPaVO.setTag(tag);
        requestInserimentoRichiestaPagamentoPagoPaVO.setRichiestaPagamentoPagoPa(richiestaPagamentoPagoPaVO);
        inserimentoRichiestaPagamentoPagoPa.setArg0(requestInserimentoRichiestaPagamentoPagoPaVO);
        log.info("Payment request to be sent to BPay: " + inserimentoRichiestaPagamentoPagoPa);
        JAXBElement<InserimentoRichiestaPagamentoPagoPa> objectFactoryInserimentoRichiestaPagamentoPagoPa = objectFactory.createInserimentoRichiestaPagamentoPagoPa(inserimentoRichiestaPagamentoPagoPa);
        JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse> inserimentoRichiestaPagamentoPagoPaResponseJAXBElement;
        inserimentoRichiestaPagamentoPagoPaResponseJAXBElement = (JAXBElement<InserimentoRichiestaPagamentoPagoPaResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInserimentoRichiestaPagamentoPagoPa);
        InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse = inserimentoRichiestaPagamentoPagoPaResponseJAXBElement.getValue();
        log.info("END sendPaymentRequest");
        return inserimentoRichiestaPagamentoPagoPaResponse;
    }

    public StornoPagamentoResponse sendRefundRequest(BPayRefundRequest request, String guid) {
        log.info("START sendRefundRequest");
        RequestStornoPagamentoVO requestStornoPagamentoVO = new RequestStornoPagamentoVO();
        requestStornoPagamentoVO.setContesto(createContesto(guid, request.getLanguage()));
        requestStornoPagamentoVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        requestStornoPagamentoVO.setCausale(request.getSubject());
        Integer refundAttempt = request.getRefundAttempt();
        requestStornoPagamentoVO.setTipoStorno(refundAttempt == null || refundAttempt == 0 ? "01" : null);
        StornoPagamento stornoPagamento = new StornoPagamento();
        stornoPagamento.setArg0(requestStornoPagamentoVO);
        log.info("Refund request to be sent to BPay: " + stornoPagamento);
        JAXBElement<StornoPagamento> stornoPagamentoJAXBElement = objectFactory.createStornoPagamento(stornoPagamento);
        JAXBElement<StornoPagamentoResponse> responseStornoPagamento = (JAXBElement<StornoPagamentoResponse>) webServiceTemplate.marshalSendAndReceive(stornoPagamentoJAXBElement);
        log.info("END sendRefundRequest");
        return responseStornoPagamento.getValue();
    }

    private ContestoVO createContesto(String guid, String language) {
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid(guid);
        contestoVO.setToken(token);
        contestoVO.setLingua(LinguaEnum.fromValue(ClientUtils.getLanguageCode(language)));
        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente(null);
        utenteVO.setCodGruppo(groupCode);
        utenteVO.setCodIstituto(instituteCode);
        contestoVO.setUtenteAttivo(utenteVO);
        return contestoVO;
    }

    public InquiryTransactionStatusResponse sendInquiryRequest(BPayRefundRequest bPayRefundRequest, String guid) {
        log.info("START sendInquiryRequest");
        InquiryTransactionStatus inquiryTransactionStatus = new InquiryTransactionStatus();
        RequestInquiryTransactionStatusVO requestInquiryTransactionStatusVO = new RequestInquiryTransactionStatusVO();
        requestInquiryTransactionStatusVO.setCorrelationId(null);
        requestInquiryTransactionStatusVO.setIdPagoPa(bPayRefundRequest.getIdPagoPa().toString());
        ContestoVO contestoVO = createContesto(guid, bPayRefundRequest.getLanguage());
        requestInquiryTransactionStatusVO.setContesto(contestoVO);
        inquiryTransactionStatus.setArg0(requestInquiryTransactionStatusVO);
        log.info("Inquiry transaction status request to be sent to BPay: " + inquiryTransactionStatus);
        JAXBElement<InquiryTransactionStatus> objectFactoryInquiryTransactionStatus = objectFactory.createInquiryTransactionStatus(inquiryTransactionStatus);
        JAXBElement<InquiryTransactionStatusResponse> inquiryTransactionStatusResponseJAXBElement;
        inquiryTransactionStatusResponseJAXBElement = (JAXBElement<InquiryTransactionStatusResponse>) webServiceTemplate.marshalSendAndReceive(objectFactoryInquiryTransactionStatus);
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse = inquiryTransactionStatusResponseJAXBElement.getValue();
        log.info("END sendInquiryRequest");
        return inquiryTransactionStatusResponse;

    }



}
