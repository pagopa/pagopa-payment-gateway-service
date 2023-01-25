package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

import java.util.List;

@Data
public class VposOrderStatusResponse {

    private String timestamp;
    private String resultCode;
    private String resultMac;
    private String productRef;
    private String numberOfItems;
    private List<ThreeDS2Authorization> authorizations;
    private VposOrderStatus orderStatus;
    private PanAliasData panAliasData;
}
