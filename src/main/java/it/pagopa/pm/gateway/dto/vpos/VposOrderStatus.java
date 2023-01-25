package it.pagopa.pm.gateway.dto.vpos;

import lombok.Data;

@Data
public class VposOrderStatus {

    private Header header;
    private String orderId;
}
