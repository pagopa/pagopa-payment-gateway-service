package it.pagopa.pm.gateway.client.azure;

import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.constant.Configurations;
import it.pagopa.pm.gateway.utils.Config;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class AzureLoginClient {

    private final static String MICROSOFT_AZURE_LOGIN_GRANT_TYPE = "client_credentials";
    private static final String CLIENT_ID_PARAMETER = "client_id";
    private static final String CLIENT_SECRET_PARAMETER = "client_secret";
    private static final String GRANT_TYPE_PARAMETER = "grant_type";
    private static final String SCOPE_PARAMETER = "scope";

    @Autowired
    private Config config;

    @Autowired
    private RestTemplate microsoftAzureRestTemplate;

    public MicrosoftAzureLoginResponse requestMicrosoftAzureLoginPostepay() {
        if (BooleanUtils.isTrue(BooleanUtils.toBoolean(config.getConfig(Configurations.AZUREAUTH_CLIENT_POSTEPAY_ENABLED)))) {
            MultiValueMap<String, String> loginRequest = createMicrosoftAzureLoginRequest(config.getConfig(Configurations.AZUREAUTH_CLIENT_POSTEPAY_CLIENT_ID), config.getConfig(Configurations.AZUREAUTH_CLIENT_POSTEPAY_CLIENT_SECRET), config.getConfig(Configurations.AZUREAUTH_CLIENT_POSTEPAY_SCOPE));
            return requestMicrosoftAzureLogin(loginRequest, config.getConfig(Configurations.AZUREAUTH_CLIENT_POSTEPAY_URL));
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
