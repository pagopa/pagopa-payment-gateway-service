package it.pagopa.pm.gateway.dto.xpay;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.pm.gateway.dto.transaction.AuthResultEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutcomeXpayGatewayRequest {
    @NotNull
    private String paymentGatewayType;

    @NotNull
    private AuthResultEnum outcome;

    private String authorizationCode;

    private String errorCode;

    public OutcomeXpayGatewayRequest(String paymentGatewayType, AuthResultEnum outcome, String authorizationCode, String errorCode) {
        this.paymentGatewayType = paymentGatewayType;
        this.outcome = outcome;
        this.authorizationCode = StringUtils.isEmpty(authorizationCode) ? null : authorizationCode;
        this.errorCode = StringUtils.isEmpty(errorCode) ? null : errorCode;
    }
}
