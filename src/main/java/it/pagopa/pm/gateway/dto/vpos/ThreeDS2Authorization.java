package it.pagopa.pm.gateway.dto.vpos;

import it.pagopa.pm.gateway.dto.enums.CardCircuit;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class ThreeDS2Authorization implements ThreeDS2ResponseElement {

    private String paymentType;
    private String authorizationType;
    private String transactionId;
    private CardCircuit network;
    private String orderId;
    private Long transactionAmount;
    private Long authorizedAmount;
    private String currency;
    private String exponent;
    private Long accountedAmount;
    private Long refundedAmount;
    private String transactionResult;
    private String timestamp;
    private String authorizationNumber;
    private String acquirerBin;
    private String merchantId;
    private String transactionStatus;
    private String responseCodeIso;
    private String rrn;

}
