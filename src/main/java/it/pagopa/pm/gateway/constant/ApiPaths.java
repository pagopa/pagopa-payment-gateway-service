package it.pagopa.pm.gateway.constant;

public class ApiPaths {

    private ApiPaths() {}

    public static final String REQUEST_PAYMENTS = "/request-payments";
    public static final String BPAY = "/bancomatpay";
    public static final String REQUEST_PAYMENTS_BPAY = REQUEST_PAYMENTS + BPAY;

}
