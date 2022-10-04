package it.pagopa.pm.gateway.dto;

import it.pagopa.pm.gateway.dto.enums.XPayOutcomeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XPayAuthPollingResponse {

    private XPayOutcomeEnum authOutcome;
    private XPayPollingResponseError error;
    private String html;
}
