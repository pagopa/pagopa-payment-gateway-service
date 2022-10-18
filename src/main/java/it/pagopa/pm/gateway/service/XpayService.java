package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.xpay.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class XpayService {

    private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
    private static final String XPAY_REQUEST_STRING = "XPay Request: ";
    private static final String CALLING_POST_STRING = "Calling POST - ";
    @Value("${xpay.authenticationUrl}")
    private String xpayAuthUrl;

    @Value("${pgs.xpay.azure.apiKey}")
    private String azureApiKey;

    @Value("${xpay.paymentUrl}")
    private String xpayPaymentUrl;

    @Value("${xpay.orderStatusUrl")
    private String orderStatusUrl;

    @Value("${xpay.revertUrl")
    private String revertUrl;

    @Autowired
    RestTemplate xpayRestTemplate;

    public AuthPaymentXPayResponse callAutenticazione3DS(AuthPaymentXPayRequest xPayRequest) {
        log.debug(XPAY_REQUEST_STRING + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<AuthPaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info(CALLING_POST_STRING + xpayAuthUrl);
        return xpayRestTemplate.postForObject(xpayAuthUrl, entity, AuthPaymentXPayResponse.class);
    }

    public PaymentXPayResponse callPaga3DS(PaymentXPayRequest xPayRequest) {
        log.debug(XPAY_REQUEST_STRING + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<PaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info(CALLING_POST_STRING + xpayPaymentUrl);
        return xpayRestTemplate.postForObject(xpayPaymentUrl, entity, PaymentXPayResponse.class);
    }

    public XPayOrderStatusResponse callSituazioneOrdine(XPayOrderStatusRequest xPayRequest) {
        log.debug(XPAY_REQUEST_STRING + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<XPayOrderStatusRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info(CALLING_POST_STRING + orderStatusUrl);
        return xpayRestTemplate.postForObject(orderStatusUrl, entity, XPayOrderStatusResponse.class);
    }

    public XPayRevertResponse callStorna(XPayRevertRequest xPayRequest) {
        log.debug(XPAY_REQUEST_STRING + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<XPayRevertRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info(CALLING_POST_STRING + orderStatusUrl);
        return xpayRestTemplate.postForObject(xpayPaymentUrl, entity, XPayRevertResponse.class);
    }
}