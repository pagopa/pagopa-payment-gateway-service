package it.pagopa.pm.gateway.client.postepay;

import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginRequest;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.UrlEncoded;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.openapitools.client.model.ResponseURLs;
import org.openapitools.client.model.PaymentChannel;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PostePayClient {

    private final String microsoftAzureLoginMode = "x-www-form-urlencoded";


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


    public MicrosoftAzureLoginResponse requestMicrosoftAzureLogin(){
        MicrosoftAzureLoginRequest microsoftAzureLoginRequest = new MicrosoftAzureLoginRequest();
        updateMicrosoftAzureLoginRequest(microsoftAzureLoginRequest);

        MicrosoftAzureLoginResponse microsoftAzureLoginResponse;

        try {
            HttpEntity<MicrosoftAzureLoginRequest> entity = new HttpEntity<>(microsoftAzureLoginRequest, null);
            microsoftAzureLoginResponse = microsoftAzureRestTemplatePostePay.postForObject(MICROSOFT_AZURE_LOGIN_URL, entity, MicrosoftAzureLoginResponse.class);
        } catch (Exception e) {
            log.error("Exception calling POSTEPAY Microsoft Azure login service", e);
            throw e;
        }

        return microsoftAzureLoginResponse;

    }

    private void setResponseUrl(ResponseURLs responseURLs, String clientId){
        PaymentChannel paymentChannel = PaymentChannel.fromValue(clientId);

        switch (paymentChannel){
            case APP: responseURLs.setResponseUrlOk("");
                responseURLs.setResponseUrlKo("");
                responseURLs.setServerNotificationUrl("url della put");
                break;
            case WEB: responseURLs.setResponseUrlOk("channel_responseURL");
                responseURLs.setResponseUrlKo("channel_responseURL");
                responseURLs.setServerNotificationUrl("url della put");
                break;
            default: break;
        }

    }


    private void updateMicrosoftAzureLoginRequest(MicrosoftAzureLoginRequest microsoftAzureLoginRequest){
        microsoftAzureLoginRequest.setMode(microsoftAzureLoginMode);

        List<UrlEncoded> urlEncodedList = new ArrayList<>();
        UrlEncoded clientIdUrlEncoded = new UrlEncoded("client_id", MICROSOFT_AZURE_LOGIN_CLIENT_ID, "text");
        urlEncodedList.add(clientIdUrlEncoded);

        UrlEncoded clientSecretdUrlEncoded = new UrlEncoded("client_secret", MICROSOFT_AZURE_LOGIN_CLIENT_SECRET, "text");
        urlEncodedList.add(clientSecretdUrlEncoded);

        UrlEncoded grantTypeUrlEncoded = new UrlEncoded("grant_type", MICROSOFT_AZURE_LOGIN_GRANT_TYPE, "text");
        urlEncodedList.add(grantTypeUrlEncoded);

        UrlEncoded scopeUrlEncoded = new UrlEncoded("scope", MICROSOFT_AZURE_LOGIN_SCOPE, "text");
        urlEncodedList.add(scopeUrlEncoded);

        microsoftAzureLoginRequest.setUrlencoded(urlEncodedList);


    }



}
