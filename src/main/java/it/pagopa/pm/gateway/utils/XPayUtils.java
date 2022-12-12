package it.pagopa.pm.gateway.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pm.gateway.dto.xpay.AuthPaymentXPayRequest;
import it.pagopa.pm.gateway.dto.xpay.EsitoXpay;
import it.pagopa.pm.gateway.dto.xpay.XPay3DSResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static it.pagopa.pm.gateway.controller.XPayPaymentController.EUR_CURRENCY;
import static it.pagopa.pm.gateway.controller.XPayPaymentController.OBJECT_MAPPER;

@Service
@Slf4j
public class XPayUtils {

    @Value("${xpay.apiKey}")
    private String apiKey;

    @Value("${xpay.secretKey}")
    private String secretKey;

    public Boolean checkMac(String xPayMac, XPay3DSResponse xPay3DSResponse) {
        String operationId = xPay3DSResponse.getOperationId();
        String timeStamp = xPay3DSResponse.getTimestamp();
        EsitoXpay outcome = xPay3DSResponse.getOutcome();
        String xpayNonce = xPay3DSResponse.getXpayNonce();
        String pgsMac = hashMac(String.format("esito=%sidOperazione=%sxpayNonce=%stimeStamp=%s%s",
                outcome.name(), operationId, xpayNonce, timeStamp, secretKey));
        return xPayMac.equals(pgsMac);
    }

    public String createMacForOrderStatus(String codTrans, String timeStamp) {
        String macString = String.format("apiKey=%scodiceTransazione=%stimeStamp=%s%s",
                apiKey, codTrans, timeStamp, secretKey);
        return hashMac(macString);
    }

    public String createMac(String codTrans, BigInteger importo, String timeStamp) {
        String macString = String.format("apiKey=%scodiceTransazione=%sdivisa=%simporto=%stimeStamp=%s%s",
                apiKey, codTrans, EUR_CURRENCY, importo, timeStamp, secretKey);
        return hashMac(macString);
    }

    public String createPaymentMac(String codTrans, BigInteger importo, String timeStamp, String xpayNonce) {
        String macString = String.format("apiKey=%scodiceTransazione=%simporto=%sdivisa=%sxpayNonce=%stimeStamp=%s%s",
                apiKey, codTrans, importo, EUR_CURRENCY, xpayNonce, timeStamp, secretKey);
        return hashMac(macString);
    }

    public BigInteger getGrandTotalForMac(PaymentRequestEntity entity) throws JsonProcessingException {
        String json = entity.getJsonRequest();
        AuthPaymentXPayRequest authRequest = OBJECT_MAPPER.readValue(json, AuthPaymentXPayRequest.class);
        return authRequest.getImporto();
    }

    public String hashMac(String macString) {
        String hash = StringUtils.EMPTY;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] in = digest.digest(macString.getBytes(StandardCharsets.UTF_8));

            final StringBuilder builder = new StringBuilder();
            for (byte b : in) {
                builder.append(String.format("%02x", b));
            }
            hash = builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("hashMac", e);
        }
        return hash;
    }
}