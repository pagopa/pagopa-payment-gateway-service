package it.pagopa.pm.gateway.constant;

public class ApiPaths {

    private ApiPaths() {}

    private static final String REQUEST_PAYMENTS = "/request-payments";
    private static final String REQUEST_REFUNDS = "/request-refunds";
    private static final String BPAY = "/bancomatpay";
    private static final String POSTEPAY = "/postepay";
    public static final String REQUEST_PAYMENTS_BPAY = REQUEST_PAYMENTS + BPAY;
    public static final String REQUEST_REFUNDS_BPAY = REQUEST_REFUNDS + BPAY;
    public static final String HEALTHCHECK = "/healthcheck";
    public static final String REQUEST_PAYMENT_POSTEPAY = REQUEST_PAYMENTS + POSTEPAY;
    public static final String REQUEST_ID = "/{requestId}";
    public static final String REQUEST_PAYMENT_POSTEPAY_REQUEST_ID = REQUEST_PAYMENTS + POSTEPAY + REQUEST_ID;


}
