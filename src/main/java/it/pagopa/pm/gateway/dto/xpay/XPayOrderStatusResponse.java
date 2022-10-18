package it.pagopa.pm.gateway.dto.xpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XPayOrderStatusResponse {

    private Long timeStamp;
    private EsitoXpay esito;
    private String idOperazione;
    private List<XPayReport> report;
    private String mac;
    private XpayError errore;

}
