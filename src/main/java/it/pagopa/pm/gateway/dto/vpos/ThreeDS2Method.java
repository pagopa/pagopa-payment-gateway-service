package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class ThreeDS2Method implements ThreeDS2ResponseElement {

    private String threeDSTransId;
    private String threeDSMethodData;
    private String threeDSMethodUrl;
    private String mac;

}
