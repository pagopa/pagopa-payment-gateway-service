package it.pagopa.pm.gateway.constant;

public class ApiPaths {

    private ApiPaths() {}

    private static final String REQUEST_PAYMENTS = "/request-payments";
    private static final String REQUEST_REFUNDS = "/request-refunds";
    private static final String BPAY = "/bancomatpay";
    public static final String REQUEST_PAYMENTS_BPAY = REQUEST_PAYMENTS + BPAY;
    public static final String REQUEST_REFUNDS_BPAY = REQUEST_REFUNDS + BPAY;

}
