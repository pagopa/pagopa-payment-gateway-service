package it.pagopa.pm.gateway.constant;

import org.springframework.stereotype.Component;

@Component
public class ClientConfigs {

    public static final String PIPE_SPLIT_CHAR = "\\|";
    public static final String MERCHANT_ID_CONFIG = "merchantId";
    public static final String SHOP_ID_CONFIG = "shopId";
    public static final String PAYMENT_CHANNEL_CONFIG = "paymentChannel";
    public static final String AUTH_TYPE_CONFIG = "authType";
    public static final String NOTIFICATION_URL_CONFIG = "notificationUrl";
    public static final String GROUP_CODE = "groupCode";
    public static final String INSTITUTE_CODE = "instituteCode";
    public static final String TAG = "tag";
    public static final String TOKEN = "token";
    public static final String BPAY_URL = "url";
    public static final String TIMEOUT_MS = "timeoutMs";
    public final static String MICROSOFT_AZURE_LOGIN_GRANT_TYPE = "client_credentials";
    public static final String CLIENT_ID_PARAMETER = "client_id";
    public static final String CLIENT_SECRET_PARAMETER = "client_secret";
    public static final String GRANT_TYPE_PARAMETER = "grant_type";
    public static final String SCOPE_PARAMETER = "scope";
    public static final String IS_AZURE_AUTH_ENABLED = "azure_auth_enabled";
    public static final String MAX_TOTAL = "max_total";
    public static final String MAX_PER_ROUTE = "max_per_route";
    public static final String AZURE_AUTH_URL = "azure_auth_url";



}
