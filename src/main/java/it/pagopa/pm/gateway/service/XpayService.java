package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
import it.pagopa.pm.gateway.dto.xpay.PaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.PaymentXPayResponse;
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
    @Value("${xpay.authenticationUrl}")
    private String xpayAuthUrl;

    @Value("${pgs.xpay.azure.apiKey}")
    private String azureApiKey;

    @Value("${xpay.paymentUrl}")
    private String xpayPaymentUrl;

    @Autowired
    RestTemplate xpayRestTemplate;

    public AuthPaymentXPayResponse callAutenticazione3DS(AuthPaymentXPayRequest xPayRequest) {
        log.debug("XPay Request: " + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<AuthPaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info("Calling POST - " + xpayAuthUrl);
        return xpayRestTemplate.postForObject(xpayAuthUrl, entity, AuthPaymentXPayResponse.class);
    }

    public PaymentXPayResponse callPaga3DS(PaymentXPayRequest xPayRequest) {
        log.debug("XPay Request: " + xPayRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(OCP_APIM_SUBSCRIPTION_KEY, azureApiKey);
        HttpEntity<PaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info("Calling POST - " + xpayPaymentUrl);
        return xpayRestTemplate.postForObject(xpayPaymentUrl, entity, PaymentXPayResponse.class);
    }
}