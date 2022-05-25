package it.pagopa.pm.gateway.dto.microsoft.azure.login;


import lombok.Data;

@Data
public class MicrosoftAzureLoginResponse {

     String token_type;
     int expires_in;
     int ext_expires_in;
     String access_token;
}
