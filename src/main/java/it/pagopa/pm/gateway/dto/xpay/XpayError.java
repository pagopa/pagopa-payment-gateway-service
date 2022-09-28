package it.pagopa.pm.gateway.dto.xpay;

import lombok.Data;

import java.io.Serializable;

@Data
public class XpayError implements Serializable {

    private Long codice;
    private String messaggio;

}
