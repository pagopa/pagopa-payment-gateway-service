package it.pagopa.pm.gateway.dto.microsoft.azure.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftAzureLoginRequest {

        String mode;
        List<UrlEncoded> urlencoded;

}
