package it.pagopa.pm.gateway.client.restapicd;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import it.pagopa.pm.gateway.dto.PostePayPatchRequest;
import it.pagopa.pm.gateway.dto.PostePayPatchRequestData;
import it.pagopa.pm.gateway.dto.TransactionUpdateRequest;
import it.pagopa.pm.gateway.dto.TransactionUpdateRequestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

import static it.pagopa.pm.gateway.utils.MdcUtils.buildMdcHeader;

@Slf4j
@Component
public class RestapiCdClientImpl {

    private static final String OCP_APIM_SUBSCRIPTION_KEY_NAME = "Ocp-Apim-Subscription-Key";
    @Value("${HOSTNAME_PM}")
    public String hostnamePm;

    @Value("${APIM_PGS_UPDATES_KEY}")
    public String apimUpdateSubscriptionKey;

    @PostConstruct
    public void init() {
        restapiCdClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(RestapiCdClient.class, hostnamePm);
    }

    private RestapiCdClient restapiCdClient;

    public void callTransactionUpdate(Long id, TransactionUpdateRequest request) {
        log.info("Calling PATCH to update transaction " + id);
        Map<String, Object> headerMap = buildMdcHeader();
        restapiCdClient.updateTransaction(id, headerMap, new TransactionUpdateRequestData(request));
    }

    public String callUpdatePostePayTransaction(Long id, PostePayPatchRequest postePayPatchRequest) {
        log.info("Calling Payment Manager's updatePostePayTransaction for transaction " + id);
        Map<String, Object> headerMap = buildMdcHeader();
        headerMap.put(OCP_APIM_SUBSCRIPTION_KEY_NAME, apimUpdateSubscriptionKey);
        return restapiCdClient.updatePostePayTransaction(id, headerMap, new PostePayPatchRequestData(postePayPatchRequest));
    }

}
