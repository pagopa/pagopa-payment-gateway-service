package it.pagopa.pm.gateway.dto.vpos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThreeDsMethodData {
    private String threeDSMethodNotificationURL;
    private String threeDSServerTransID;
}
