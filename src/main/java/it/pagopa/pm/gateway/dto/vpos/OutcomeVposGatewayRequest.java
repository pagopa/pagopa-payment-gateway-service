package it.pagopa.pm.gateway.dto.vpos;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import it.pagopa.pm.gateway.dto.xpay.OutcomeXpayGatewayRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutcomeVposGatewayRequest extends OutcomeXpayGatewayRequest {
    private String rrn;

    public OutcomeVposGatewayRequest(String paymentGatewayType, AuthResultEnum outcome, String rrn, String authorizationCode, String errorCode) {
        super(paymentGatewayType, outcome, authorizationCode, errorCode);
        this.rrn = StringUtils.isEmpty(rrn) ? null : rrn;
    }
}
