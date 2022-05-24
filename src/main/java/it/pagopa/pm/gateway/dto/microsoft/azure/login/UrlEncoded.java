package it.pagopa.pm.gateway.dto.microsoft.azure.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlEncoded {

    String key;
    String value;
    String type;

}
