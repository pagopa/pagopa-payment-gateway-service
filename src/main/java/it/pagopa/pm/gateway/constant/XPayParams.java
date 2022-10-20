package it.pagopa.pm.gateway.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XPayParams {

    public static final String XPAY_OUTCOME = "esito";
    public static final String XPAY_OPERATION_ID = "idOperazione";
    public static final String XPAY_TIMESTAMP = "timeStamp";
    public static final String XPAY_MAC = "mac";
    public static final String XPAY_NONCE = "xpayNonce";
    public static final String XPAY_ERROR_CODE = "codice";
    public static final String XPAY_ERROR_MESSAGE = "messaggio";
    public static final String XPAY_KEY_RESUME_TYPE = "resumeType";
    public static final String RESUME_TYPE_XPAY = "xpay";

}
