package it.pagopa.pm.gateway.client.postepay;

import it.pagopa.pm.gateway.dto.PostePayAuthRequest;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginRequest;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.MicrosoftAzureLoginResponse;
import it.pagopa.pm.gateway.dto.microsoft.azure.login.UrlEncoded;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PostePayClient {

    private final String microsoftAzureLoginMode = "x-www-form-urlencoded";


    @Value("${postePay.client.microsoft.azure.login.url:https://login.microsoftonline.com/520b4604-8be7-4169-9320-4e50c72f728d/oauth2/v2.0/token}")
    public String MICROSOFT_AZURE_LOGIN_URL;

    @Value("${postePay.client.microsoft.azure.login.client.id:1d1828e6-cf67-4ced-a256-7f076cf2750c}")
    public String MICROSOFT_AZURE_LOGIN_CLIENT_ID;
    @Value("${postePay.client.microsoft.azure.login.client.secret:9nL8Q~hpIUqPGq.607OUZSoCj8mKc1ILjr~ncaiJ}")
    public String MICROSOFT_AZURE_LOGIN_CLIENT_SECRET;
    @Value("${postePay.client.microsoft.azure.login.grant.type:client_credentials}")
    public String MICROSOFT_AZURE_LOGIN_GRANT_TYPE;
    @Value("${postePay.client.microsoft.azure.login.scope:https://paymentmanagerppsvil.onmicrosoft.com/paymentserver/.default}")
    public String MICROSOFT_AZURE_LOGIN_SCOPE;




    @Value("${postePay.client.server.notification.url}")
    public String SERVER_NOTIFICATION_URL;
    @Value("${postePay.client.shop.id}")
    public String SHOP_ID;
    @Value("${postePay.client.auth.type}")
    public String AUTH_TYPE;
    @Value("${postePay.client.url}")
    public String POSTE_PAY_ROOT_URL;


    @Autowired
    private RestTemplate createPaymentRestTemplate;

    @Autowired
    private RestTemplate microsoftAzureRestTemplate;


    public MicrosoftAzureLoginResponse requestMicrosoftAzureLogin(){
        MicrosoftAzureLoginRequest microsoftAzureLoginRequest = new MicrosoftAzureLoginRequest();
        updateMicrosoftAzureLoginRequest(microsoftAzureLoginRequest);

        MicrosoftAzureLoginResponse microsoftAzureLoginResponse;

        try {
            HttpEntity<MicrosoftAzureLoginRequest> entity = new HttpEntity<>(microsoftAzureLoginRequest, null);
            microsoftAzureLoginResponse = microsoftAzureRestTemplate.postForObject(MICROSOFT_AZURE_LOGIN_URL, entity, MicrosoftAzureLoginResponse.class);
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
