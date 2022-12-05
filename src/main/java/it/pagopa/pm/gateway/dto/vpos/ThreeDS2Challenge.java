package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class ThreeDS2Challenge implements ThreeDS2ResponseElement {

    private String threeDSTransId;
    private String cReq;
    private String acsUrl;
    private String mac;

}
