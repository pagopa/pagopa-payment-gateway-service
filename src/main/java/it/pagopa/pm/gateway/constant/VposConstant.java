package it.pagopa.pm.gateway.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VposConstant {

    public static final String RESULT_CODE_AUTHORIZED = "00";
    public static final String RESULT_CODE_METHOD = "25";
    public static final String RESULT_CODE_CHALLENGE = "26";

    public static final String WRONG_MAC_MSG = "WARNING! EXPECTED RESPONSE MAC: %s, BUT WAS: %s";

    public static final String RELEASE_VALUE = "02";
    public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
    public static final String PARAM_DATA = "data";
    public static final String OPERATION_AUTH_REQUEST_3DS2_STEP_0 = "THREEDSAUTHORIZATION0";
    public static final String CURRENCY_VALUE = "978";
    public static final String ACCOUNT_DEFERRED = "D";
    public static final String FAKE_DESCRIPTION = "Pagamenti PA";
    public static final String OPERATION_ACCOUNTING = "ACCOUNTING";
    public static final String OPERATION_REFUND = "REFUND";

}
