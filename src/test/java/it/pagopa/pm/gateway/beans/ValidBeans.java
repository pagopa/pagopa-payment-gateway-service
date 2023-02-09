package it.pagopa.pm.gateway.beans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.client.bpay.generated.*;
import it.pagopa.pm.gateway.client.vpos.HttpClientResponse;
import it.pagopa.pm.gateway.dto.ACKMessage;
import it.pagopa.pm.gateway.dto.AuthMessage;
import it.pagopa.pm.gateway.dto.PatchRequest;
import it.pagopa.pm.gateway.dto.TransactionUpdateRequest;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayInfoResponse;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayOutcomeResponse;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayPaymentRequest;
import it.pagopa.pm.gateway.dto.bancomatpay.BPayRefundRequest;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.config.VposClientConfig;
import it.pagopa.pm.gateway.dto.config.XpayClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.enums.OutcomeEnum;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.dto.postepay.*;
import it.pagopa.pm.gateway.dto.vpos.*;
import it.pagopa.pm.gateway.dto.xpay.*;
import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.utils.VPosMacBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.openapitools.client.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static it.pagopa.pm.gateway.constant.ApiPaths.REQUEST_PAYMENTS_XPAY;
import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.constant.XPayParams.*;
import static it.pagopa.pm.gateway.dto.enums.CardCircuit.MASTERCARD;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.*;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.KO;
import static it.pagopa.pm.gateway.dto.enums.RefundOutcome.OK;
import static it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum.*;
import static org.openapitools.client.model.AuthorizationType.IMMEDIATA;

public class ValidBeans {
    private static final String EURO_ISO_CODE = "978";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String RESPONSE_URL_OK_KO = "https://portal.test.pagopa.gov.it/pmmockserviceapi/home";
    public static final String NOTIFICATION_URL = "${postepay.notificationURL}";

    public static BPayPaymentRequest bPayPaymentRequest() {
        BPayPaymentRequest request = new BPayPaymentRequest();
        request.setIdPsp("Id_psp");
        request.setIdPagoPa(1L);
        request.setAmount(100d);
        request.setSubject("causale");
        request.setEncryptedTelephoneNumber("pqimx8en49fbf");
        request.setLanguage("IT");
        return request;
    }

    public static InserimentoRichiestaPagamentoPagoPaResponse inserimentoRichiestaPagamentoPagoPaResponse() {
        InserimentoRichiestaPagamentoPagoPaResponse response = new InserimentoRichiestaPagamentoPagoPaResponse();
        ResponseInserimentoRichiestaPagamentoPagoPaVO responseData = new ResponseInserimentoRichiestaPagamentoPagoPaVO();
        responseData.setCorrelationId("id");
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setEsito(true);
        esitoVO.setCodice("0");
        esitoVO.setMessaggio("messaggio");
        responseData.setEsito(esitoVO);
        ContestoVO contestoVO = new ContestoVO();
        contestoVO.setGuid("7c7c201a-c198-31c7-864c-21d61b2bd810");
        responseData.setContesto(contestoVO);
        response.setReturn(responseData);
        return response;
    }

    public static BPayOutcomeResponse bPayPaymentOutcomeResponseToReturn() {
        BPayOutcomeResponse response = new BPayOutcomeResponse();
        response.setOutcome(true);
        return response;
    }

    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToSave() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        return entity;
    }

    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToSave_2() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("client-guid");
        entity.setIsProcessed(true);
        return entity;
    }


    public static BPayPaymentResponseEntity bPayPaymentResponseEntityToFind() {
        BPayPaymentResponseEntity entity = new BPayPaymentResponseEntity();
        entity.setIdPagoPa(1L);
        entity.setOutcome(true);
        entity.setCorrelationId("id");
        entity.setMessage("messaggio");
        entity.setErrorCode("0");
        entity.setClientGuid("client-guid");
        return entity;
    }

    public static AuthMessage authMessage(OutcomeEnum outcomeEnum) {
        AuthMessage authMessage = new AuthMessage();
        authMessage.setAuthCode("authCode");
        authMessage.setAuthOutcome(outcomeEnum);
        return authMessage;
    }

    public static ACKMessage ackMessageResponse(OutcomeEnum outcomeEnum) {
        ACKMessage ackMessage = new ACKMessage();
        ackMessage.setOutcome(outcomeEnum);
        return ackMessage;
    }

    public static TransactionUpdateRequest transactionUpdateRequest() {
        TransactionUpdateRequest transactionUpdateRequest = new TransactionUpdateRequest();
        transactionUpdateRequest.setStatus(21L);
        transactionUpdateRequest.setAuthCode("authCode");
        transactionUpdateRequest.setPgsOutcome("0");
        transactionUpdateRequest.setCorrelationId("id");
        return transactionUpdateRequest;
    }

    public static BPayRefundRequest bPayRefundRequest() {
        BPayRefundRequest bPayRefundRequest = new BPayRefundRequest();
        bPayRefundRequest.setIdPagoPa(1L);
        bPayRefundRequest.setSubject(null);
        bPayRefundRequest.setLanguage("IT");
        bPayRefundRequest.setRefundAttempt(0);

        return bPayRefundRequest;

    }

    public static InquiryTransactionStatusResponse inquiryTransactionStatusResponse(boolean hasReturn) {
        InquiryTransactionStatusResponse inquiryTransactionStatusResponse = new InquiryTransactionStatusResponse();
        ResponseInquiryTransactionStatusVO responseInquiryTransactionStatusVO = new ResponseInquiryTransactionStatusVO();
        responseInquiryTransactionStatusVO.setEsitoPagamento("EFF");
        if (hasReturn)
            inquiryTransactionStatusResponse.setReturn(responseInquiryTransactionStatusVO);

        return inquiryTransactionStatusResponse;

    }

    public static StornoPagamentoResponse stornoPagamentoResponse(boolean hasReturn, boolean esito) {
        StornoPagamentoResponse stornoPagamentoResponse = new StornoPagamentoResponse();
        ResponseStornoPagamentoVO responseStornoPagamentoVO = new ResponseStornoPagamentoVO();
        EsitoVO esitoVO = new EsitoVO();
        esitoVO.setEsito(esito);
        responseStornoPagamentoVO.setEsito(esitoVO);
        if (hasReturn)
            stornoPagamentoResponse.setReturn(responseStornoPagamentoVO);

        return stornoPagamentoResponse;
    }

    public static CreatePaymentRequest createPaymentRequest(PaymentChannel paymentChannel) {
        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest();
        String shopId = paymentChannel.equals(PaymentChannel.APP) ? "shopIdTmp_APP" : "shopIdTmp_WEB";
        createPaymentRequest.setMerchantId("merchantId");
        createPaymentRequest.setShopId(shopId);
        createPaymentRequest.setShopTransactionId("1");
        createPaymentRequest.setAmount("1234");
        createPaymentRequest.setDescription("Pagamento bollo auto");
        createPaymentRequest.setCurrency(EURO_ISO_CODE);
        createPaymentRequest.setBuyerName("Mario Rossi");
        createPaymentRequest.setBuyerEmail("mario.rossi@gmail.com");
        createPaymentRequest.setPaymentChannel(paymentChannel);
        createPaymentRequest.setAuthType(IMMEDIATA);

        ResponseURLs responseUrls = new ResponseURLs();
        String urlOkKo = paymentChannel.equals(PaymentChannel.APP) ? StringUtils.EMPTY : RESPONSE_URL_OK_KO;
        responseUrls.setResponseUrlKo(urlOkKo);
        responseUrls.setResponseUrlOk(urlOkKo);
        responseUrls.setServerNotificationUrl(NOTIFICATION_URL);

        createPaymentRequest.setResponseURLs(responseUrls);
        return createPaymentRequest;
    }

    public static OnboardingRequest createOnboardingRequest(PaymentChannel paymentChannel) {
        ResponseURLs responseUrls = new ResponseURLs();
        String urlOkKo = paymentChannel.equals(PaymentChannel.APP) ? StringUtils.EMPTY : RESPONSE_URL_OK_KO;
        responseUrls.setResponseUrlKo(urlOkKo);
        responseUrls.setResponseUrlOk(urlOkKo);
        responseUrls.setServerNotificationUrl(NOTIFICATION_URL);

        OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setOnboardingTransactionId("1");
        onboardingRequest.setMerchantId("merchantId");
        onboardingRequest.setPaymentChannel(paymentChannel);
        onboardingRequest.setShopId("shopIdTmp_APP");
        onboardingRequest.setResponseURLs(responseUrls);

        return onboardingRequest;
    }


    public static PostePayAuthRequest postePayAuthRequest(boolean isValid) {
        PostePayAuthRequest postePayAuthRequest = new PostePayAuthRequest();
        postePayAuthRequest.setGrandTotal(1234);
        postePayAuthRequest.setIdTransaction(isValid ? "1" : null);
        postePayAuthRequest.setName("Mario Rossi");
        postePayAuthRequest.setEmailNotice("mario.rossi@gmail.com");
        postePayAuthRequest.setDescription("Pagamento bollo auto");
        return postePayAuthRequest;
    }

    public static MicrosoftAzureLoginResponse microsoftAzureLoginResponse() {
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse = new MicrosoftAzureLoginResponse();
        microsoftAzureLoginResponse.setAccess_token("access_token");
        microsoftAzureLoginResponse.setExpires_in(100);
        microsoftAzureLoginResponse.setToken_type("TOKEN_TYPE");
        microsoftAzureLoginResponse.setExt_expires_in(100);
        return microsoftAzureLoginResponse;

    }

    public static PostePayAuthResponse postePayAuthResponse(String clientId, boolean isError, String errorMessage) {
        PostePayAuthResponse postePayAuthResponse = new PostePayAuthResponse();
        postePayAuthResponse.setChannel(clientId);
        if (isError) {
            postePayAuthResponse.setError(errorMessage);
        } else {
            postePayAuthResponse.setUrlRedirect("${postepay.pgs.response.urlredirect}");
        }

        return postePayAuthResponse;

    }

    public static CreatePaymentResponse getOkResponse() {
        CreatePaymentResponse inlineResponse200 = new CreatePaymentResponse();
        inlineResponse200.setPaymentID("1234");
        inlineResponse200.setUserRedirectURL("www.userRedirectUrl.com");
        return inlineResponse200;
    }

    public static OnboardingResponse getOKResponseForOnboarding() {
        OnboardingResponse response = new OnboardingResponse();
        response.setOnboardingID("1234");
        response.setUserRedirectURL("www.userRedirectUrl.com");
        return response;
    }


    public static PaymentRequestEntity paymentRequestEntity(PostePayAuthRequest postePayAuthRequest, Boolean authorizationOutcome, String clientId) {
        String authRequestJson = null;


        if (Objects.nonNull(postePayAuthRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayAuthRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setAuthorizationUrl("www.userRedirectUrl.com");
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        paymentRequestEntity.setIsProcessed(false);
        paymentRequestEntity.setCorrelationId("1234");
        paymentRequestEntity.setAuthorizationCode(null);
        paymentRequestEntity.setIdTransaction("1");
        paymentRequestEntity.setGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        paymentRequestEntity.setId(null);
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setMdcInfo(null);
        paymentRequestEntity.setResourcePath(null);
        paymentRequestEntity.setRequestEndpoint("/request-payments/postepay");
        paymentRequestEntity.setResourcePath("${postepay.logo.url}");
        paymentRequestEntity.setIsOnboarding(false);
        paymentRequestEntity.setStatus("CREATED");
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityOnboarding(PostePayOnboardingRequest postePayOnboardingRequest, Boolean authorizationOutcome, String clientId) {
        String authRequestJson = null;


        if (Objects.nonNull(postePayOnboardingRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(postePayOnboardingRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setAuthorizationUrl("www.userRedirectUrl.com");
        paymentRequestEntity.setAuthorizationOutcome(authorizationOutcome);
        paymentRequestEntity.setIsProcessed(false);
        paymentRequestEntity.setCorrelationId("1234");
        paymentRequestEntity.setAuthorizationCode(null);
        paymentRequestEntity.setIdTransaction("1");
        paymentRequestEntity.setGuid("8d8b30e3-de52-4f1c-a71c-9905a8043dac");
        paymentRequestEntity.setId(null);
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setMdcInfo(null);
        paymentRequestEntity.setResourcePath(null);
        paymentRequestEntity.setRequestEndpoint("/request-payments/postepay");
        paymentRequestEntity.setResourcePath("${postepay.logo.url}");
        paymentRequestEntity.setIsOnboarding(true);
        paymentRequestEntity.setStatus("CREATED");
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityOnboardingFalse(PostePayAuthRequest postePayAuthRequest, Boolean authorizationOutcome, String clientId) {
        PaymentRequestEntity paymentRequestEntity = paymentRequestEntity(postePayAuthRequest, authorizationOutcome, clientId);
        paymentRequestEntity.setIsOnboarding(false);
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityWithRefundData(String clientId, String authorizationCode, Boolean isRefunded, Boolean changeRequestEndPoint) {
        PaymentRequestEntity paymentRequestEntity = paymentRequestEntity(null, true, clientId);
        paymentRequestEntity.setAuthorizationCode(authorizationCode);
        paymentRequestEntity.setIsRefunded(isRefunded);
        if (changeRequestEndPoint) {
            paymentRequestEntity.setRequestEndpoint("/invalidEndpoint");
        }
        return paymentRequestEntity;

    }

    public static PostePayPollingResponse postePayPollingResponse() {
        PostePayPollingResponse postePayPollingResponse = new PostePayPollingResponse();
        postePayPollingResponse.setChannel(PaymentChannel.APP.getValue());
        postePayPollingResponse.setUrlRedirect("www.userRedirectUrl.com");
        postePayPollingResponse.setAuthOutcome(OutcomeEnum.OK);
        postePayPollingResponse.setClientResponseUrl("www.clientResponseUrl.com");
        postePayPollingResponse.setError(StringUtils.EMPTY);
        postePayPollingResponse.setLogoResourcePath("${postepay.logo.url}");
        return postePayPollingResponse;
    }


    public static PostePayPollingResponse postePayPollingResponseError(String error, OutcomeEnum outcomeEnum) {
        PostePayPollingResponse postePayPollingResponse = new PostePayPollingResponse();
        postePayPollingResponse.setChannel(PaymentChannel.APP.getValue());
        postePayPollingResponse.setUrlRedirect("www.userRedirectUrl.com");
        postePayPollingResponse.setAuthOutcome(outcomeEnum);
        postePayPollingResponse.setClientResponseUrl(null);
        postePayPollingResponse.setError(error);
        return postePayPollingResponse;
    }

    public static PostePayRefundResponse postePayRefundResponse(String requestId, String paymentId, String refundOutcome, String error) {
        PostePayRefundResponse postePayRefundResponse = new PostePayRefundResponse();
        postePayRefundResponse.setRequestId(requestId);
        postePayRefundResponse.setPaymentId(paymentId);
        postePayRefundResponse.setRefundOutcome(refundOutcome);
        postePayRefundResponse.setError(error);

        return postePayRefundResponse;

    }

    public static DetailsPaymentResponse detailsPaymentResponse(Esito esito) {
        DetailsPaymentResponse detailsPaymentResponse = new DetailsPaymentResponse();
        detailsPaymentResponse.setStatus(esito);
        return detailsPaymentResponse;

    }

    public static RefundPaymentResponse refundPaymentResponse(EsitoStorno esitoStorno) {
        RefundPaymentResponse refundPaymentResponse = new RefundPaymentResponse();
        refundPaymentResponse.setTransactionResult(esitoStorno);
        return refundPaymentResponse;
    }

    public static RefundPaymentRequest refundPaymentRequest(String authNumber) {
        RefundPaymentRequest refundPaymentRequest = new RefundPaymentRequest();
        refundPaymentRequest.setMerchantId("merchantId");
        refundPaymentRequest.setShopId("shopIdTmp_APP");
        refundPaymentRequest.setShopTransactionId("1");
        refundPaymentRequest.setCurrency("978");
        refundPaymentRequest.setPaymentID("1234");
        refundPaymentRequest.setAuthNumber(authNumber);

        return refundPaymentRequest;
    }


    public static DetailsPaymentRequest detailsPaymentRequest() {
        DetailsPaymentRequest detailsPaymentRequest = new DetailsPaymentRequest();
        detailsPaymentRequest.setPaymentID("1234");
        detailsPaymentRequest.setShopTransactionId("1");
        detailsPaymentRequest.setShopId("shopIdTmp_APP");

        return detailsPaymentRequest;
    }

    public static PatchRequest patchRequest() {
        return new PatchRequest(21L, "authCode");
    }


    public static PostePayOnboardingRequest createPostePayOnboardingRequest(String onboardingTransactionId) {
        PostePayOnboardingRequest postePayOnboardingRequest = new PostePayOnboardingRequest();

        postePayOnboardingRequest.setOnboardingTransactionId(onboardingTransactionId);
        return postePayOnboardingRequest;
    }

    public static XPayAuthRequest createXPayAuthRequest(boolean isValid) {
        XPayAuthRequest xPayAuthRequest = new XPayAuthRequest();
        xPayAuthRequest.setIdTransaction("2");
        xPayAuthRequest.setCvv("123");
        xPayAuthRequest.setPan("1548965265");
        xPayAuthRequest.setExpiryDate("202302");
        xPayAuthRequest.setGrandTotal(isValid ? BigInteger.valueOf(1234) : BigInteger.valueOf(0));
        return xPayAuthRequest;
    }

    public static XPayAuthResponse xPayAuthResponse(boolean isError, String errorMessage, String requestId, boolean isDenied) {
        XPayAuthResponse xPayAuthResponse = new XPayAuthResponse();
        xPayAuthResponse.setRequestId(requestId);
        if (isError) {
            xPayAuthResponse.setError(errorMessage);
        } else if (isDenied) {
            xPayAuthResponse.setStatus("DENIED");
            xPayAuthResponse.setUrlRedirect("http://localhost:8080/payment-gateway/" + requestId);
        } else {
            xPayAuthResponse.setStatus("CREATED");
            xPayAuthResponse.setUrlRedirect("http://localhost:8080/payment-gateway/" + requestId);
        }
        return xPayAuthResponse;
    }

    public static PaymentRequestEntity paymentRequestEntityxPay(XPayAuthRequest XPayAuthRequest, String clientId,
                                                                Boolean isValid, PaymentRequestStatusEnum statusEnum,
                                                                boolean isRefunded) {
        String authRequestJson = null;


        if (Objects.nonNull(XPayAuthRequest)) {
            try {
                authRequestJson = OBJECT_MAPPER.writeValueAsString(XPayAuthRequest);
            } catch (JsonProcessingException jspe) {
                jspe.printStackTrace();
            }
        }
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(XPayAuthRequest.getIdTransaction());
        paymentRequestEntity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        paymentRequestEntity.setJsonRequest(authRequestJson);
        paymentRequestEntity.setStatus(statusEnum.name());
        paymentRequestEntity.setIsRefunded(isRefunded);
        if (BooleanUtils.toBoolean(isValid)) {
            paymentRequestEntity.setXpayHtml("<html><body></body></html>");
        }
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityxPayWithoutHtml(XPayAuthRequest XPayAuthRequest, String clientId) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(XPayAuthRequest.getIdTransaction());
        paymentRequestEntity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        paymentRequestEntity.setStatus(CREATED.name());
        return paymentRequestEntity;
    }

    public static PaymentRequestEntity paymentRequestEntityxPayWithError(XPayAuthRequest XPayAuthRequest, String clientId) {
        PaymentRequestEntity paymentRequestEntity = new PaymentRequestEntity();
        paymentRequestEntity.setClientId(clientId);
        paymentRequestEntity.setGuid(UUID.randomUUID().toString());
        paymentRequestEntity.setRequestEndpoint(REQUEST_PAYMENTS_XPAY);
        paymentRequestEntity.setIdTransaction(XPayAuthRequest.getIdTransaction());
        paymentRequestEntity.setTimeStamp(String.valueOf(System.currentTimeMillis()));
        paymentRequestEntity.setStatus(DENIED.name());
        paymentRequestEntity.setErrorMessage("Error message Test");
        paymentRequestEntity.setErrorCode(String.valueOf(1L));
        return paymentRequestEntity;
    }


    public static AuthPaymentXPayRequest createAuthPaymentRequest(XPayAuthRequest xPayAuthRequest) throws NoSuchAlgorithmException {
        AuthPaymentXPayRequest authPaymentXPayRequest = new AuthPaymentXPayRequest();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        authPaymentXPayRequest.setApiKey("ExampleApiKey");
        authPaymentXPayRequest.setPan(xPayAuthRequest.getPan());
        authPaymentXPayRequest.setScadenza(xPayAuthRequest.getExpiryDate());
        authPaymentXPayRequest.setCodiceTransazione(xPayAuthRequest.getIdTransaction());
        authPaymentXPayRequest.setImporto(xPayAuthRequest.getGrandTotal());
        authPaymentXPayRequest.setCvv(xPayAuthRequest.getCvv());
        authPaymentXPayRequest.setUrlRisposta("localhost");
        authPaymentXPayRequest.setDivisa("978");
        authPaymentXPayRequest.setTimeStamp(timeStamp);
        authPaymentXPayRequest.setMac(createMac(xPayAuthRequest.getGrandTotal(), timeStamp));
        return authPaymentXPayRequest;
    }

    public static AuthPaymentXPayResponse createXPayAuthResponse(AuthPaymentXPayRequest authPaymentXPayRequest) {
        AuthPaymentXPayResponse authPaymentXPayResponse = new AuthPaymentXPayResponse();
        authPaymentXPayResponse.setHtml("<html><body></body></html>");
        authPaymentXPayResponse.setEsito(EsitoXpay.OK);
        authPaymentXPayResponse.setTimeStamp(System.currentTimeMillis());
        authPaymentXPayResponse.setMac(authPaymentXPayRequest.getMac());
        return authPaymentXPayResponse;
    }

    public static AuthPaymentXPayResponse createBadXPayAuthResponse(AuthPaymentXPayRequest authPaymentXPayRequest) {
        AuthPaymentXPayResponse authPaymentXPayResponse = new AuthPaymentXPayResponse();
        authPaymentXPayResponse.setHtml("<html><body></body></html>");
        authPaymentXPayResponse.setEsito(EsitoXpay.KO);
        authPaymentXPayResponse.setTimeStamp(System.currentTimeMillis());
        authPaymentXPayResponse.setMac(authPaymentXPayRequest.getMac());
        XpayError error = new XpayError();
        error.setMessaggio("messaggio");
        error.setCodice(1L);
        authPaymentXPayResponse.setErrore(error);
        return authPaymentXPayResponse;
    }

    public static XPayPollingResponse createXpayAuthPollingResponse(boolean isOk, XPayPollingResponseError error,
                                                                    boolean isCancelled) {
        XPayPollingResponse response = new XPayPollingResponse();
        if (isOk) {
            response.setHtml("<html><body></body></html>");
            response.setStatus(CREATED.name());
        } else if (Objects.nonNull(error)) {
            response.setStatus(DENIED.name());
            response.setError(error);
        }
        if (isCancelled) {
            response.setStatus(CANCELLED.name());
        }
        return response;
    }

    private static String createMac(BigInteger importo, String timeStamp) throws NoSuchAlgorithmException {
        String macString = String.format("apiKey=%scodiceTransazione=%sdivisa=%simporto=%stimeStamp=%s%s",
                "apiKey", "02", "978", importo, timeStamp, "secretKey");
        return hashMac(macString);
    }

    private static String hashMac(String macString) throws NoSuchAlgorithmException {
        String hash;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] in = digest.digest(macString.getBytes(StandardCharsets.UTF_8));

        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        hash = builder.toString();

        return hash;
    }

    public static PaymentXPayRequest createXPayPaymentRequest(PaymentRequestEntity entity) {
        PaymentXPayRequest xPayRequest = new PaymentXPayRequest();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        xPayRequest.setApiKey("ExampleApiKey");
        xPayRequest.setCodiceTransazione(entity.getIdTransaction());
        xPayRequest.setImporto(BigInteger.valueOf(1256));
        xPayRequest.setDivisa(978L);
        xPayRequest.setTimeStamp(timeStamp);
        xPayRequest.setMac("mac");
        xPayRequest.setXpayNonce("nonce");
        return xPayRequest;
    }

    public static MultiValueMap<String, String> createXPayResumeRequest(boolean isValid) throws NoSuchAlgorithmException {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put(XPAY_KEY_RESUME_TYPE, Collections.singletonList(RESUME_TYPE_XPAY));
        if (isValid) {
            parameters.put(XPAY_OUTCOME, Collections.singletonList("OK"));
            parameters.put(XPAY_OPERATION_ID, Collections.singletonList("123456"));
            parameters.put(XPAY_NONCE, Collections.singletonList("nonce"));
            parameters.put(XPAY_MAC, Collections.singletonList(createMac(BigInteger.valueOf(1234), String.valueOf(12L))));
        } else {
            parameters.put(XPAY_OUTCOME, Collections.singletonList("KO"));
            parameters.put(XPAY_ERROR_CODE, Collections.singletonList("codiceErrore"));
            parameters.put(XPAY_ERROR_MESSAGE, Collections.singletonList("messaggioErrore"));
        }
        parameters.put(XPAY_TIMESTAMP, Collections.singletonList(timeStamp));
        return parameters;
    }

    public static MultiValueMap<String, String> createXPayResumeRequestWithEsitoNull() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put(XPAY_KEY_RESUME_TYPE, Collections.singletonList(RESUME_TYPE_XPAY));
        parameters.put(XPAY_OPERATION_ID, Collections.singletonList("123456"));
        parameters.put(XPAY_NONCE, Collections.singletonList("nonce"));
        parameters.put(XPAY_MAC, Collections.singletonList("mac"));
        return parameters;
    }

    public static PaymentXPayResponse createPaymentXPayResponse(boolean isValid) {
        PaymentXPayResponse response = new PaymentXPayResponse();
        response.setTimeStamp(System.currentTimeMillis());
        if (isValid) {
            response.setEsito(EsitoXpay.OK);
            response.setIdOperazione("idOperazione");
            response.setCodiceAutorizzazione("codiceAutorizzazione");
            response.setCodiceConvenzione("codiceConvenzione");
            response.setData("DataDiOggi");
            response.setNazione("IT");
            response.setRegione("Lombardia");
            response.setBrand("brand");
            response.setMac("mac");
            response.setTipoProdotto("prodotto");
            response.setTipoTransazione("transazione");
        } else {
            response.setEsito(EsitoXpay.KO);
            XpayError error = new XpayError();
            error.setCodice(1L);
            error.setMessaggio("messaggioErrore");
            response.setErrore(error);
        }
        return response;
    }

    public static XPayOrderStatusResponse createXPayOrderStatusResponse(Boolean isValid) {
        XPayOrderStatusResponse response = new XPayOrderStatusResponse();

        response.setTimeStamp(System.currentTimeMillis());
        if (isValid) {
            response.setEsito(EsitoXpay.OK);
        } else {
            response.setEsito(EsitoXpay.KO);
            XpayError error = new XpayError();
            error.setCodice(12L);
            error.setMessaggio("ErrroMessage");
            response.setErrore(error);
        }
        response.setIdOperazione("idOperazione");
        response.setReport(null);
        response.setMac("mac");
        return response;
    }

    public static XPayRevertResponse createXPayRevertResponse(boolean isValid) {
        XPayRevertResponse response = new XPayRevertResponse();

        if (isValid) {
            response.setEsito(EsitoXpay.OK);
        } else {
            response.setEsito(EsitoXpay.KO);
            XpayError error = new XpayError();
            error.setCodice(12L);
            error.setMessaggio("ErrroMessage");
            response.setErrore(error);
        }
        response.setIdOperazione("idOperazione");
        response.setTimeStamp(System.currentTimeMillis());
        response.setMac("mac");
        return response;
    }

    public static Calendar returnCalendar() {
        return Calendar.getInstance();
    }

    public static BPayInfoResponse bpayInfoResponse(boolean isError, String errorString) {
        BPayInfoResponse bPayInfoResponse;
        bPayInfoResponse = isError ? new BPayInfoResponse(null, errorString) :
                new BPayInfoResponse("id", null);
        return bPayInfoResponse;
    }

    public static XPayOrderStatusRequest createXPayOrderStatusRequest() {
        return new XPayOrderStatusRequest(
                "apiKey",
                "codTrans",
                String.valueOf(System.currentTimeMillis()),
                "mac");
    }

    public static XPayRevertRequest createXPayRevertRequest() {
        return new XPayRevertRequest(
                "apiKey",
                "codTrans",
                BigInteger.valueOf(23L),
                978L,
                String.valueOf(System.currentTimeMillis()),
                "mac");
    }

    public static StepZeroRequest createStep0Request(Boolean isFirstPayment) {
        return new StepZeroRequest(
                "123456",
                BigInteger.valueOf(123455),
                "3456567899754",
                "123",
                "12/23",
                "holder",
                MASTERCARD,
                "threeDsData",
                "email@emal.com",
                isFirstPayment,
                "idPsp13");
    }

    public static HttpClientResponse createHttpClientResponseVPos() throws IOException {
        HttpClientResponse httpClientResponse = new HttpClientResponse();
        httpClientResponse.setStatus(200);
        ThreeDS2Response response = createThreeDS2ResponseStep0Authorization();
        String json = OBJECT_MAPPER.writeValueAsString(response);
        byte[] entity = convertToBytes(json);
        httpClientResponse.setEntity(entity);
        return httpClientResponse;
    }

    public static HttpClientResponse createKOHttpClientResponseVPos() throws IOException {
        HttpClientResponse httpClientResponse = new HttpClientResponse();
        httpClientResponse.setStatus(500);
        ThreeDS2Response response = createThreeDS2ResponseStep0Authorization();
        String json = OBJECT_MAPPER.writeValueAsString(response);
        byte[] entity = convertToBytes(json);
        httpClientResponse.setEntity(entity);
        return httpClientResponse;
    }

    private static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    public static ThreeDS2Response createThreeDS2ResponseStep0Authorization() {
        ThreeDS2Response response = new ThreeDS2Response();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        response.setTimestamp(timeStamp);
        response.setResultCode("00");
        response.setResponseType(AUTHORIZATION);
        ThreeDS2ResponseElement threeDS2ResponseElement = createteThreeDs2Authorization(timeStamp);
        response.setThreeDS2ResponseElement(threeDS2ResponseElement);
        return response;
    }

    public static ThreeDS2Response createThreeDS2ResponseStep0Method() {
        ThreeDS2Response response = new ThreeDS2Response();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        response.setTimestamp(timeStamp);
        response.setResultCode("25");
        response.setResponseType(METHOD);
        ThreeDS2ResponseElement threeDS2ResponseElement = createteThreeDs2Method();
        response.setThreeDS2ResponseElement(threeDS2ResponseElement);
        return response;
    }

    public static ThreeDS2Response createThreeDS2ResponseStep0Challenge() {
        ThreeDS2Response response = new ThreeDS2Response();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        response.setTimestamp(timeStamp);
        response.setResultCode("26");
        response.setResponseType(CHALLENGE);
        ThreeDS2ResponseElement threeDS2ResponseElement = createteThreeDs2Challenge();
        response.setThreeDS2ResponseElement(threeDS2ResponseElement);
        return response;
    }

    private static ThreeDS2Authorization createteThreeDs2Authorization(String timeStamp) {
        ThreeDS2Authorization authorization = new ThreeDS2Authorization();
        authorization.setPaymentType("PaymenyType");
        authorization.setAuthorizationType("AuthType");
        authorization.setTransactionId("idTrans");
        authorization.setNetwork(MASTERCARD);
        authorization.setOrderId("orderId");
        authorization.setTransactionAmount(12344L);
        authorization.setAuthorizedAmount(1234L);
        authorization.setCurrency("978");
        authorization.setExponent("exponent");
        authorization.setAccountedAmount(1234L);
        authorization.setRefundedAmount(1234L);
        authorization.setTransactionResult("result");
        authorization.setTimestamp(timeStamp);
        authorization.setAuthorizationNumber("authNumb");
        authorization.setMerchantId("merchantId");
        authorization.setTransactionStatus("status");
        authorization.setResponseCodeIso("00");
        authorization.setRrn("rrn");
        return authorization;
    }


    private static ThreeDS2Method createteThreeDs2Method() {
        ThreeDS2Method method = new ThreeDS2Method();
        method.setThreeDSTransId("123");
        method.setThreeDSMethodData("methodData");
        method.setThreeDSMethodUrl("urlRedirect");
        method.setMac("mac");
        return method;
    }

    private static ThreeDS2Challenge createteThreeDs2Challenge() {
        ThreeDS2Challenge challenge = new ThreeDS2Challenge();
        challenge.setThreeDSTransId("123");
        challenge.setCReq("cReq");
        challenge.setAcsUrl("urlRedirect");
        challenge.setMac("mac");
        return challenge;
    }

    public static AuthResponse createVPosAuthResponse(String resultCode) {
        AuthResponse response = new AuthResponse();
        response.setResultCode(resultCode);
        response.setResultMac("resultMac");
        response.setStatus("status");
        response.setAmount(23L);
        response.setAcquirerBin("acqBin");
        response.setAccountAmount(23L);
        response.setAcquirerTransactionId("transId");
        response.setAuthorizationAmount(23L);
        response.setAuthorizationMac("authMAc");
        response.setAuthorizationNumber("authNumb");
        response.setTimestamp(String.valueOf(System.currentTimeMillis()));
        response.setRrn("rrn");
        response.setRefundAmount(23L);
        response.setPaymentType("PaymentType");
        response.setOrderNumber("ordNumb");
        response.setMerchantCode("MerchCode");
        response.setCurrency("978");
        response.setAuthorizationType("authType");
        return response;
    }

    public static String createConfigMacStep0(ThreeDS2Response threeDS2Response) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addString(threeDS2Response.getTimestamp());
        macBuilder.addString(threeDS2Response.getResultCode());
        macBuilder.addString("null");
        return macBuilder.toSha1Hex(StandardCharsets.ISO_8859_1);
    }

    public static String createConfigMacAuth(AuthResponse authResponse) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addString(authResponse.getTimestamp());
        macBuilder.addString(authResponse.getResultCode());
        macBuilder.addString("null");
        return macBuilder.toSha1Hex(StandardCharsets.ISO_8859_1);
    }

    public static List<String> generateVariable(Boolean isFisrtPayment) {
        List<String> variables;
        if (isFisrtPayment) {
            variables = Arrays.asList("1", "2", "shopId", "terminalId", "mac");
        } else {
            variables = Arrays.asList("1", "2", "3", "4", "5", "shopId", "terminalId", "mac");
        }
        return variables;
    }

    public static Shop generateShop(String idPsp) {
        Shop shop = new Shop();
        shop.setIdPsp(idPsp);
        shop.setAbi("ABI");
        shop.setShopIdFirstPayment("ShopId_F");
        shop.setMacFirstPayment("mac_F");
        shop.setTerminalIdFirstPayment("terminalId_F");
        shop.setShopIdSuccPayment("shopId_S");
        shop.setMacSuccPayment("mac_S");
        shop.setTerminalIdSuccPayment("terminalId_S");
        return shop;
    }

    public static Shop generateKOShop(String idPsp) {
        Shop shop = new Shop();
        shop.setIdPsp(idPsp);
        shop.setShopIdFirstPayment("ShopId_F");
        shop.setTerminalIdFirstPayment("terminalId_F");
        shop.setShopIdSuccPayment("shopId_S");
        shop.setMacSuccPayment("mac_S");
        return shop;
    }

    public static Document createThreeDs2AuthorizationResponseDocument(ThreeDS2Response threeDS2Response) throws IOException, JDOMException {
        ThreeDS2Authorization authorization = (ThreeDS2Authorization) threeDS2Response.getThreeDS2ResponseElement();
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BPWXmlResponse><Timestamp>" + threeDS2Response.getTimestamp() + "</Timestamp><Result>00</Result><MAC>null</MAC><Data><PanAliasData><PanAlias>alias</PanAlias>" +
                "<PanAliasExpDate>exp</PanAliasExpDate><PanAliasTail>tail</PanAliasTail><MAC>9ba711c59658a8abb5a962a0becb515e980cee1e</MAC></PanAliasData>" +
                "<ThreeDSAuthorizationRequest0>" +
                "<Header><ShopID>shopIdS</ShopID><OperatorID>terminalIdS</OperatorID><ReqRefNum>123456</ReqRefNum></Header>" +
                "<OrderID>12345678921</OrderID><Pan>123456789123</Pan><CVV2>123</CVV2><ExpDate>30/12</ExpDate><Amount>12345</Amount><Currency>978</Currency>" +
                "<AccountingMode>D</AccountingMode><Network>02</Network><Userid>prova prova</Userid><OpDescr>Pagamenti PA</OpDescr><ThreeDSData>threeDSData</ThreeDSData>" +
                "<NotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</NotifUrl>" +
                "<EmailCH>prova@mail.com</EmailCH><NameCH>prova prova</NameCH>" +
                "<ThreeDSMtdNotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</ThreeDSMtdNotifUrl>" +
                "</ThreeDSAuthorizationRequest0>" +
                "<Authorization>" +
                "<PaymentType>" + authorization.getPaymentType() + "</PaymentType><AuthorizationType>" + authorization.getAuthorizationType() + "</AuthorizationType>" +
                "<TransactionID>" + authorization.getTransactionId() + "</TransactionID><Network>02</Network>" +
                "<OrderID>" + authorization.getOrderId() + "</OrderID><TransactionAmount>" + authorization.getTransactionAmount() + "</TransactionAmount>" +
                "<AuthorizedAmount>" + authorization.getAuthorizedAmount() + "</AuthorizedAmount><Currency>978</Currency><Exponent>" + authorization.getExponent() + "</Exponent>" +
                "<AccountedAmount>" + authorization.getAccountedAmount() + "</AccountedAmount><RefundedAmount>" + authorization.getRefundedAmount() + "</RefundedAmount>" +
                "<TransactionResult>" + authorization.getTransactionResult() + "</TransactionResult><Timestamp>" + authorization.getTimestamp() + "</Timestamp>" +
                "<AuthorizationNumber>" + authorization.getAuthorizationNumber() + "</AuthorizationNumber><AcquirerBIN>" + authorization.getAcquirerBin() + "</AcquirerBIN>" +
                "<MerchantID>" + authorization.getMerchantId() + "</MerchantID>" + "<TransactionStatus>" + authorization.getTransactionStatus() + "</TransactionStatus>" +
                "<ResponseCodeISO>" + authorization.getResponseCodeIso() + "</ResponseCodeISO><RRN>" + authorization.getRrn() + "</RRN>" +
                "</Authorization>" +
                "</Data></BPWXmlResponse>\n";
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(xmlString));
    }

    public static Document createThreeDs2MethodResponseDocument(ThreeDS2Response threeDS2Response) throws IOException, JDOMException {
        ThreeDS2Method method = (ThreeDS2Method) threeDS2Response.getThreeDS2ResponseElement();
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BPWXmlResponse><Timestamp>" + threeDS2Response.getTimestamp() + "</Timestamp><Result>00</Result><MAC>null</MAC><Data><PanAliasData><PanAlias>alias</PanAlias>" +
                "<PanAliasExpDate>exp</PanAliasExpDate><PanAliasTail>tail</PanAliasTail><MAC>9ba711c59658a8abb5a962a0becb515e980cee1e</MAC></PanAliasData>" +
                "<ThreeDSAuthorizationRequest0>" +
                "<Header><ShopID>shopIdS</ShopID><OperatorID>terminalIdS</OperatorID><ReqRefNum>123456</ReqRefNum></Header>" +
                "<OrderID>12345678921</OrderID><Pan>123456789123</Pan><CVV2>123</CVV2><ExpDate>30/12</ExpDate><Amount>12345</Amount><Currency>978</Currency>" +
                "<AccountingMode>D</AccountingMode><Network>02</Network><Userid>prova prova</Userid><OpDescr>Pagamenti PA</OpDescr><ThreeDSData>threeDSData</ThreeDSData>" +
                "<NotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</NotifUrl>" +
                "<EmailCH>prova@mail.com</EmailCH><NameCH>prova prova</NameCH>" +
                "<ThreeDSMtdNotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</ThreeDSMtdNotifUrl>" +
                "</ThreeDSAuthorizationRequest0>" +
                "<ThreeDSMethod>" +
                "<ThreeDSTransId>" + method.getThreeDSTransId() + "</ThreeDSTransId><ThreeDSMethodData>" + method.getThreeDSMethodData() + "</ThreeDSMethodData>" +
                "<ThreeDSMethodUrl>" + method.getThreeDSMethodUrl() + "</ThreeDSMethodUrl><MAC>" + method.getMac() + "</MAC>" +
                "</ThreeDSMethod>" +
                "</Data></BPWXmlResponse>\n";
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(xmlString));
    }

    public static Document createThreeDs2ChallengeResponseDocument(ThreeDS2Response threeDS2Response) throws IOException, JDOMException {
        ThreeDS2Challenge challenge = (ThreeDS2Challenge) threeDS2Response.getThreeDS2ResponseElement();
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BPWXmlResponse><Timestamp>" + threeDS2Response.getTimestamp() + "</Timestamp><Result>00</Result><MAC>null</MAC><Data><PanAliasData><PanAlias>alias</PanAlias>" +
                "<PanAliasExpDate>exp</PanAliasExpDate><PanAliasTail>tail</PanAliasTail><MAC>9ba711c59658a8abb5a962a0becb515e980cee1e</MAC></PanAliasData>" +
                "<ThreeDSAuthorizationRequest0>" +
                "<Header><ShopID>shopIdS</ShopID><OperatorID>terminalIdS</OperatorID><ReqRefNum>123456</ReqRefNum></Header>" +
                "<OrderID>12345678921</OrderID><Pan>123456789123</Pan><CVV2>123</CVV2><ExpDate>30/12</ExpDate><Amount>12345</Amount><Currency>978</Currency>" +
                "<AccountingMode>D</AccountingMode><Network>02</Network><Userid>prova prova</Userid><OpDescr>Pagamenti PA</OpDescr><ThreeDSData>threeDSData</ThreeDSData>" +
                "<NotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</NotifUrl>" +
                "<EmailCH>prova@mail.com</EmailCH><NameCH>prova prova</NameCH>" +
                "<ThreeDSMtdNotifUrl>http://localhost:8080/payment-gateway/payment-gateway/request-payments/creditCard/07f92d0c-1735-4863-9051-dcdb03e88ead/resume</ThreeDSMtdNotifUrl>" +
                "</ThreeDSAuthorizationRequest0>" +
                "<ThreeDSChallenge>" +
                "<ThreeDSTransId>" + challenge.getThreeDSTransId() + "</ThreeDSTransId><ACSUrl>" + challenge.getAcsUrl() + "</ACSUrl>" +
                "<CReq>" + challenge.getCReq() + "</CReq><MAC>" + challenge.getMac() + "</MAC>" +
                "</ThreeDSChallenge>" +
                "</Data></BPWXmlResponse>\n";
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(xmlString));
    }

    public static Document createAuthResponseDocument(AuthResponse authResponse) throws IOException, JDOMException {
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BPWXmlResponse><Timestamp>" + authResponse.getTimestamp() + "</Timestamp><Result>" + authResponse.getResultCode() + "</Result><MAC>" + authResponse.getResultMac() + "</MAC>" +
                "<Data><Operation><Authorization>" +
                "<PaymentType>" + authResponse.getPaymentType() + "</PaymentType><AuthorizationType>" + authResponse.getAuthorizationType() + "</AuthorizationType>" +
                "<TransactionID>" + authResponse.getAcquirerTransactionId() + "</TransactionID><Network></Network>" +
                "<OrderID>" + authResponse.getOrderNumber() + "</OrderID><TransactionAmount>" + authResponse.getAmount() + "</TransactionAmount>" +
                "<AuthorizedAmount>" + authResponse.getAuthorizationAmount() + "</AuthorizedAmount><RefundedAmount>" + authResponse.getRefundAmount() + "</RefundedAmount>" +
                "<AccountedAmount>" + authResponse.getAccountAmount() + "</AccountedAmount><Currency>" + authResponse.getCurrency() + "</Currency>" +
                "<AuthorizationNumber>" + authResponse.getAuthorizationNumber() + "</AuthorizationNumber><AcquirerBIN>" + authResponse.getAcquirerBin() + "</AcquirerBIN>" +
                "<MerchantID>" + authResponse.getMerchantCode() + "</MerchantID><TransactionStatus>" + authResponse.getStatus() + "</TransactionStatus>" +
                "<RRN>" + authResponse.getRrn() + "</RRN><MAC>" + authResponse.getAuthorizationMac() + "</MAC>" +
                "</Authorization></Operation></Data>" +
                "</BPWXmlResponse>\n";
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(xmlString));
    }

    public static byte[] convertToByte(Document document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Format format = Format.getCompactFormat();
        format.setOmitDeclaration(false);
        format.setEncoding(StandardCharsets.ISO_8859_1.displayName());
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(format);
        xmlOutput.output(document, outputStream);
        return outputStream.toByteArray();
    }

    public static StepZeroResponse createStepzeroResponse(HttpStatus httpStatus, String idTransaction) {
        StepZeroResponse response = new StepZeroResponse();
        if (httpStatus.is2xxSuccessful()) {
            response.setRequestId("requestId");
            response.setUrlRedirect("http://localhost:8080/payment-gateway/\"");
            response.setStatus(CREATED.name());
        } else if (httpStatus.value() == 400) {
            response.setError(BAD_REQUEST_MSG);
        } else if (httpStatus.value() == 401) {
            response.setError(TRANSACTION_ALREADY_PROCESSED_MSG);
        } else {
            response.setError(GENERIC_ERROR_MSG + idTransaction);
        }
        return response;
    }

    public static CreditCardResumeRequest createCreditCardResumeRequest(boolean isValid) {
        CreditCardResumeRequest request = new CreditCardResumeRequest();
        if (isValid) {
            request.setMethodCompleted("Y");
        } else {
            request.setMethodCompleted("N");
        }
        return request;
    }

    public static VposDeleteResponse createVposDeleteResponse(String uuid_sample, String error, boolean isValid) {
        VposDeleteResponse response = new VposDeleteResponse();
        response.setRequestId(uuid_sample);
        if (isValid) {
            response.setRefundOutcome(OK.name());
        } else {
            response.setRefundOutcome(KO.name());
        }
        response.setError(error);
        return response;
    }

    public static VposOrderStatusResponse createVposOrderStatusResponse(String resultCode) {
        VposOrderStatusResponse response = new VposOrderStatusResponse();

        response.setTimestamp(String.valueOf(System.currentTimeMillis()));
        response.setResultCode(resultCode);
        response.setResultMac("MAC");
        response.setProductRef("prodictRef");
        response.setNumberOfItems("numberOfItems");
        response.setAuthorizations(Collections.singletonList(new ThreeDS2Authorization()));
        response.setOrderStatus(createVposOrderStatus());
        return response;
    }

    private static VposOrderStatus createVposOrderStatus() {
        VposOrderStatus orderStatus = new VposOrderStatus();
        orderStatus.setHeader(createHeader());
        orderStatus.setOrderId("orderId");
        return orderStatus;
    }

    private static Header createHeader() {
        Header header = new Header();
        header.setOperatorId("OperatorId");
        header.setReqRefNum("reRefNum");
        header.setShopId("shooId");
        return header;
    }

    public static Document createVposOrderStatusResponseDocument(VposOrderStatusResponse response) throws IOException, JDOMException {
        VposOrderStatus orderStatus = response.getOrderStatus();
        Header header = orderStatus.getHeader();
        List<ThreeDS2Authorization> authorizations = response.getAuthorizations();
        ThreeDS2Authorization authorization = authorizations.get(0);
        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BPWXmlResponse><Timestamp>" + response.getTimestamp() + "</Timestamp><Result>" + response.getResultCode() + "</Result><MAC>" + response.getResultMac() + "</MAC>" +
                "<Header>" +
                "<OperatorId>" + header.getOperatorId() + "</OperatorId><ShopId>" + header.getShopId() + "</ShopId>" + "<ReqRefNum>" + header.getReqRefNum() + "</ReqRefNum>" +
                "</Header>" +
                "<Data>" +
                "<OrderStatus>" +
                "<OrderID>" + orderStatus.getOrderId() + "</OrderID>" +
                "</OrderStatus>" +
                "<ProductRef>" + response.getProductRef() + "</ProductRef>" +
                "<NumberOfItems>" + response.getNumberOfItems() + "</NumberOfItems>" +
                "<NumberOfItems>" + response.getNumberOfItems() + "</NumberOfItems>" +
                "<Authorization>" + authorization + "</Authorization>" +
                "</Data>" +
                "</BPWXmlResponse>\n";
        SAXBuilder saxBuilder = new SAXBuilder();
        return saxBuilder.build(new StringReader(xmlString));
    }

    public static ClientConfig createClientsConfig() {
        String clientReturnUrl = "http://localhost:8080";
        ClientConfig clientConfig = new ClientConfig();

        VposClientConfig vposClientConfig = new VposClientConfig();
        vposClientConfig.setClientReturnUrl(clientReturnUrl);
        clientConfig.setVpos(vposClientConfig);

        XpayClientConfig xpayClientConfig = new XpayClientConfig();
        xpayClientConfig.setClientReturnUrl(clientReturnUrl);
        clientConfig.setXpay(xpayClientConfig);

        return clientConfig;
    }
}



