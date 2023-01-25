package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

@Data
public class Header {

    private String shopId;
    private String operatorId;
    private String reqRefNum;
}
