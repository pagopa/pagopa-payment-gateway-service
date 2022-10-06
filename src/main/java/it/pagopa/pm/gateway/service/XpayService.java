package it.pagopa.pm.gateway.service;

import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayResponse;
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

    @Value("${xpay.authenticationUrl}")
    private String XPAY_AUTH_URL;

    @Autowired
    RestTemplate xpayRestTemplate;

    public AuthPaymentXPayResponse callAutenticazione3DS(AuthPaymentXPayRequest xPayRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthPaymentXPayRequest> entity = new HttpEntity<>(xPayRequest, headers);
        log.info("Calling POST - " + XPAY_AUTH_URL);
        return xpayRestTemplate.postForObject(XPAY_AUTH_URL, entity, AuthPaymentXPayResponse.class);
    }
}
