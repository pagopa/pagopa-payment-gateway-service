package it.pagopa.pm.gateway.dto.config;

import lombok.Data;

@Data
public class ClientConfig {
    private String authorizationUpdateUrl;
    private String authorizationUpdateApiKey;
    private BpayClientConfig bpay;
    private PostepayClientConfig postepay;
    private XpayClientConfig xpay;
    private VposClientConfig vpos;
}
