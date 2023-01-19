package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

@Data
public class PanAliasData {

    private String panAlias;
    private String panAliasRev;
    private String panAliasExpDate;
    private String panAliasTail;
    private String mac;
}
