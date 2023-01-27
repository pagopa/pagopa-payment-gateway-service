package it.pagopa.pm.gateway.dto.config;

import lombok.Data;

@Data
public class ClientConfig {
    private String closePaymentUrl;
    private String closePaymentApiKey;
    private String closePaymentApiKeyHeader;
    private BpayClientConfig bpay;
    private PostepayClientConfig postepay;
    private XpayClientConfig xpay;
    private VposClientConfig vpos;
}
