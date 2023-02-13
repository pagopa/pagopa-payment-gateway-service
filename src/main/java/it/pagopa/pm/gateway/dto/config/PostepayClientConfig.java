package it.pagopa.pm.gateway.dto.config;

import lombok.Data;

@Data
public class PostepayClientConfig {
    private String clientReturnUrl;
    private String paymentResponseUrl;
    private String merchantId;
    private String shopId;
    private String authType;
    private String notificationUrl;
}
