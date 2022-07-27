package it.pagopa.pm.gateway.client.azure;

import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static it.pagopa.pm.gateway.constant.ClientConfigs.*;

@Slf4j
public class AzureLoginClient {

    @Value("${azureAuth.client.postepay.config}")
    private String AZUREAUTH_CLIENT_SPOSTEPAY_CONFIG;

    private final Map<String, String> configValues;

    {
        configValues =  getConfigValues();
    }

    private Map<String, String> getConfigValues() {
        List<String> listConfig = Arrays.asList(AZUREAUTH_CLIENT_SPOSTEPAY_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(ENABLED, Objects.nonNull(listConfig.get(0))?listConfig.get(0):"true");
        configsMap.put(URL, listConfig.get(1));
        configsMap.put(SCOPE_PARAMETER, listConfig.get(2));
        configsMap.put(CLIENT_ID_PARAMETER, listConfig.get(3));
        configsMap.put(CLIENT_SECRET_PARAMETER, listConfig.get(4));

        return configsMap;
    }


    @Autowired
    private RestTemplate microsoftAzureRestTemplate;

    public MicrosoftAzureLoginResponse requestMicrosoftAzureLoginPostepay() {

        if (BooleanUtils.isTrue(Boolean.valueOf(configValues.get(ENABLED)))) {
            MultiValueMap<String, String> loginRequest = createMicrosoftAzureLoginRequest(configValues.get(CLIENT_ID_PARAMETER),
                    configValues.get(CLIENT_SECRET_PARAMETER), configValues.get(SCOPE_PARAMETER));
            return requestMicrosoftAzureLogin(loginRequest, configValues.get(URL));
        } else {
            // this is to avoid call to AZURE login if not needed, for local environment
            log.warn("Azure authentication phase bypassed");
            return new MicrosoftAzureLoginResponse();
        }
    }

    private MicrosoftAzureLoginResponse requestMicrosoftAzureLogin(MultiValueMap<String, String> body, String url) {
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse;
        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, createHttpHeadersAzureLoginRequest());
            microsoftAzureLoginResponse = microsoftAzureRestTemplate.postForObject(url, entity, MicrosoftAzureLoginResponse.class);
        } catch (Exception e) {
            log.error("Exception calling Microsoft Azure login service", e);
            throw e;
        }
        log.info("Azure authentication token acquired");
        return microsoftAzureLoginResponse;
    }

    private HttpHeaders createHttpHeadersAzureLoginRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> createMicrosoftAzureLoginRequest(String clientId, String clientSecret, String scope) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add(CLIENT_ID_PARAMETER, clientId);
        requestBody.add(CLIENT_SECRET_PARAMETER, clientSecret);
        requestBody.add(GRANT_TYPE_PARAMETER, MICROSOFT_AZURE_LOGIN_GRANT_TYPE);
        requestBody.add(SCOPE_PARAMETER, scope);
        return requestBody;
    }
}
