package it.pagopa.pm.gateway.client.azure;

import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.exception.ExceptionsEnum;
import it.pagopa.pm.gateway.exception.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

    @Value("${azureAuth.client.config}")
    private String AZURE_AUTH_CLIENT_CONFIG;

    @Value("${azureAuth.client.postepay.config}")
    private String AZURE_AUTH_CLIENT_POSTEPAY_CONFIG;

    @Autowired
    private Environment environment;

    private final Map<String, String> configValues;
    private final Map<String, String> postepayConfigValues;

    public AzureLoginClient() throws RestApiException {
        postepayConfigValues =  getPostepayConfigValues();
        configValues = getConfigValues();
    }

    private Map<String, String> getConfigValues() throws RestApiException {
        if (StringUtils.isEmpty(AZURE_AUTH_CLIENT_CONFIG)) {
            log.error("Error while retrieving 'azureAuth.client.config' environment variable. Value is blank");
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        List<String> listConfig = Arrays.asList(AZURE_AUTH_CLIENT_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(IS_AZURE_AUTH_ENABLED, Objects.nonNull(listConfig.get(0))?listConfig.get(0):"true");

        return configsMap;
    }


    private Map<String, String> getPostepayConfigValues() throws RestApiException {
        if (StringUtils.isEmpty(AZURE_AUTH_CLIENT_POSTEPAY_CONFIG)) {
            log.error("Error while retrieving 'azureAuth.client.postepay.config' environment variable. Value is blank");
            throw new RestApiException(ExceptionsEnum.GENERIC_ERROR);
        }

        List<String> listConfig = Arrays.asList(AZURE_AUTH_CLIENT_POSTEPAY_CONFIG.split(PIPE_SPLIT_CHAR));
        Map<String, String> configsMap = new HashMap<>();
        configsMap.put(AZURE_AUTH_URL, listConfig.get(0));
        configsMap.put(SCOPE_PARAMETER, listConfig.get(1));
        configsMap.put(CLIENT_ID_PARAMETER, listConfig.get(2));
        configsMap.put(CLIENT_SECRET_PARAMETER, listConfig.get(3));

        return configsMap;
    }


    @Autowired
    private RestTemplate microsoftAzureRestTemplate;

    public MicrosoftAzureLoginResponse requestMicrosoftAzureLoginPostepay() {

        if (BooleanUtils.isTrue(Boolean.valueOf(configValues.get(IS_AZURE_AUTH_ENABLED)))) {
            MultiValueMap<String, String> loginRequest = createMicrosoftAzureLoginRequest(postepayConfigValues.get(CLIENT_ID_PARAMETER),
                    postepayConfigValues.get(CLIENT_SECRET_PARAMETER), postepayConfigValues.get(SCOPE_PARAMETER));
            return requestMicrosoftAzureLogin(loginRequest, postepayConfigValues.get(AZURE_AUTH_URL));
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


