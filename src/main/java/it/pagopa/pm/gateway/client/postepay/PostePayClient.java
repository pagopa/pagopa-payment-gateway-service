package it.pagopa.pm.gateway.client.postepay;

import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class PostePayClient {

    @Value("${azureAuth.client.postepay.url}")
    private String MICROSOFT_AZURE_LOGIN_URL;

    @Value("${azureAuth.client.postepay.client_id}")
    private String MICROSOFT_AZURE_LOGIN_CLIENT_ID;
    @Value("${azureAuth.client.postepay.client_secret}")
    private String MICROSOFT_AZURE_LOGIN_CLIENT_SECRET;

    private final static String MICROSOFT_AZURE_LOGIN_GRANT_TYPE = "client_credentials";

    @Value("${azureAuth.client.postepay.scope}")
    private String MICROSOFT_AZURE_LOGIN_SCOPE;

    @Autowired
    private RestTemplate microsoftAzureRestTemplatePostePay;


    public MicrosoftAzureLoginResponse requestMicrosoftAzureLogin() {
        MicrosoftAzureLoginResponse microsoftAzureLoginResponse;
        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(createMicrosoftAzureLoginRequest(), createHttpHeadersAzureLoginRequest());
            microsoftAzureLoginResponse = microsoftAzureRestTemplatePostePay.postForObject(MICROSOFT_AZURE_LOGIN_URL, entity, MicrosoftAzureLoginResponse.class);
        } catch (Exception e) {
            log.error("Exception calling POSTEPAY Microsoft Azure login service", e);
            throw e;
        }

        return microsoftAzureLoginResponse;

    }

    private HttpHeaders createHttpHeadersAzureLoginRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> createMicrosoftAzureLoginRequest() {

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", MICROSOFT_AZURE_LOGIN_CLIENT_ID);
        requestBody.add("client_secret", MICROSOFT_AZURE_LOGIN_CLIENT_SECRET);
        requestBody.add("grant_type", MICROSOFT_AZURE_LOGIN_GRANT_TYPE);
        requestBody.add("scope", MICROSOFT_AZURE_LOGIN_SCOPE);

        return requestBody;
    }

}
