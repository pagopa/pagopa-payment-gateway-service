package it.pagopa.pm.gateway.client.bpay;

import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.dto.BPayPaymentRequest;
import it.pagopa.pm.gateway.dto.BPayRefundRequest;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import it.pagopa.pm.gateway.utils.ClientUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.ws.client.core.WebServiceTemplate;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.ClientConfigs.*;


@Slf4j
public class BancomatPayClient {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    @Autowired
    private Environment environment;

    private final ObjectFactory objectFactory = new ObjectFactory();

    @Value("${bancomatPay.client.config}")
    private String BANCOMAT_CLIENT_CONFIG;

    private Map<String, String> configValues;

    private void initConfigValues() throws RestApiException
    {
        if (!MapUtils.isEmpty(configValues)){
            return;
        }

        if (StringUtils.isEmpty(BANCOMAT_CLIENT_CONFIG)) {
            log.error("Error while retrieving 'bancomatPay.client.config' environment variable. Value is blank");
             throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }
        List<String> listConfig = Arrays.asList(BANCOMAT_CLIENT_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(GROUP_CODE, listConfig.get(0));
        configsMap.put(INSTITUTE_CODE, listConfig.get(1));
        configsMap.put(TAG, listConfig.get(2));
        configsMap.put(TOKEN, listConfig.get(3));

        configValues = configsMap;
    }

    public InserimentoRichiestaPagamentoPagoPaResponse sendPaymentRequest(BPayPaymentRequest request, String guid) throws RestApiException {
        initConfigValues();

        log.info("START sendPaymentRequest");
        InserimentoRichiestaPagamentoPagoPa inserimentoRichiestaPagamentoPagoPa = new InserimentoRichiestaPagamentoPagoPa();
        RequestInserimentoRichiestaPagamentoPagoPaVO requestInserimentoRichiestaPagamentoPagoPaVO = new RequestInserimentoRichiestaPagamentoPagoPaVO();
        requestInserimentoRichiestaPagamentoPagoPaVO.setContesto(createContesto(guid, request.getLanguage()));
        RichiestaPagamentoPagoPaVO richiestaPagamentoPagoPaVO = new RichiestaPagamentoPagoPaVO();
        richiestaPagamentoPagoPaVO.setIdPSP(request.getIdPsp() != null ? ClientUtils.INTESA_SP_CODICE_ABI : null);
        richiestaPagamentoPagoPaVO.setIdPagoPa(String.valueOf(request.getIdPagoPa()));
        richiestaPagamentoPagoPaVO.setImporto(BigDecimal.valueOf(request.getAmount()));
        richiestaPagamentoPagoPaVO.setNumeroTelefonicoCriptato(request.getEncryptedTelephoneNumber());
        richiestaPagamentoPagoPaVO.setCausale(request.getSubject());
        richiestaPagamentoPagoPaVO.setTag(configValues.get(TAG));
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

    public StornoPagamentoResponse sendRefundRequest(BPayRefundRequest request, String guid) throws RestApiException {
        initConfigValues();

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
        contestoVO.setToken(configValues.get(TOKEN));
        contestoVO.setLingua(LinguaEnum.fromValue(ClientUtils.getLanguageCode(language)));
        UtenteAttivoVO utenteVO = new UtenteAttivoVO();
        utenteVO.setCodUtente(null);
        utenteVO.setCodGruppo(configValues.get(GROUP_CODE));
        utenteVO.setCodIstituto(configValues.get(INSTITUTE_CODE));
        contestoVO.setUtenteAttivo(utenteVO);
        return contestoVO;
    }

    public InquiryTransactionStatusResponse sendInquiryRequest(BPayRefundRequest bPayRefundRequest, String guid) throws RestApiException {
        initConfigValues();

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
