package it.pagopa.pm.gateway.constant;

public class ApiPaths {

    private ApiPaths() {}

    private static final String REQUEST_PAYMENTS = "/request-payments";
    private static final String REQUEST_REFUNDS = "/request-refunds";
    private static final String BPAY = "/bancomatpay";
    private static final String POSTEPAY = "/postepay";
    private static final String XPAY = "/xpay";
    public static final String REQUEST_PAYMENTS_BPAY = REQUEST_PAYMENTS + BPAY;
    public static final String REQUEST_REFUNDS_BPAY = REQUEST_REFUNDS + BPAY;
    public static final String HEALTHCHECK = "/healthcheck";
    public static final String REQUEST_PAYMENTS_POSTEPAY = REQUEST_PAYMENTS + POSTEPAY;
    public static final String REQUEST_ID = "/{requestId}";
    public static final String POSTEPAY_REQUEST_PAYMENTS_PATH = REQUEST_PAYMENTS + POSTEPAY + REQUEST_ID;
    public static final String REQUEST_PAYMENTS_XPAY = REQUEST_PAYMENTS + XPAY;

}
