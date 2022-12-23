package it.pagopa.pm.gateway.constant;

public class ApiPaths {

    private ApiPaths() {
    }

    // features
    public static final String HEALTHCHECK = "/healthcheck";
    private static final String REQUEST_PAYMENTS = "/request-payments";
    private static final String REQUEST_REFUNDS = "/request-refunds";

    // scopes
    private static final String BPAY = "/bancomatpay";
    private static final String POSTEPAY = "/postepay";
    private static final String XPAY = "/xpay";
    private static final String CREDIT_CARD = "/creditCard";

    // path parameters
    public static final String REQUEST_ID = "/{requestId}";

    // BancomatPay
    public static final String REQUEST_PAYMENTS_BPAY = REQUEST_PAYMENTS + BPAY;
    public static final String REQUEST_REFUNDS_BPAY = REQUEST_REFUNDS + BPAY;
    public static final String RETRIEVE_BPAY_INFO = REQUEST_PAYMENTS_BPAY + REQUEST_ID;

    // PostePay
    public static final String REQUEST_PAYMENTS_POSTEPAY = REQUEST_PAYMENTS + POSTEPAY;
    public static final String POSTEPAY_REQUEST_PAYMENTS_PATH = REQUEST_PAYMENTS + POSTEPAY + REQUEST_ID;

    // XPay
    public static final String REQUEST_PAYMENTS_XPAY = REQUEST_PAYMENTS + XPAY;
    public static final String REQUEST_PAYMENTS_RESUME = REQUEST_ID + "/resume";
    public static final String REQUEST_PAYMENTS_RESUME_METHOD = REQUEST_PAYMENTS_RESUME + "/method";
    public static final String REQUEST_PAYMENTS_RESUME_CHALLENGE = REQUEST_PAYMENTS_RESUME + "/challenge";

    //CreditCard
    public static final String REQUEST_PAYMENTS_CREDIT_CARD = REQUEST_PAYMENTS + CREDIT_CARD;

}
