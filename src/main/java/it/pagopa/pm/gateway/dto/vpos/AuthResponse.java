package it.pagopa.pm.gateway.dto.vpos;

import it.pagopa.pm.gateway.dto.enums.CardCircuit;
import lombok.Data;

@Data
public class AuthResponse {

    private String timestamp;
    private String resultCode;
    private String resultMac;
    private String authorizationMac;
    private String paymentType;
    private String authorizationType;
    private String acquirerTransactionId;
    private CardCircuit circuit;
    private String orderNumber;
    private Long amount;
    private Long authorizationAmount;
    private String currency;
    private Long accountAmount;
    private Long refundAmount;
    private String authorizationNumber;
    private String acquirerBin;
    private String merchantCode;
    private String status;
    private String rrn;
    private String urlAcs;

}
