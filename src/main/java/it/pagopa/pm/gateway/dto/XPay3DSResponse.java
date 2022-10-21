package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.xpay.EsitoXpay;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XPay3DSResponse {

    private EsitoXpay outcome;
    private String operationId;
    private String xpayNonce;
    private String timestamp;
    private String mac;
    private String errorCode;
    private String errorMessage;

}
