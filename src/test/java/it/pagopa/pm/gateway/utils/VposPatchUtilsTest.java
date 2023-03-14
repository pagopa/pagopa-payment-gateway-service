package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.VposOrderStatusResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.AUTHORIZED;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.DENIED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VposPatchUtils.class)
public class VposPatchUtilsTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;
    @Mock
    private EcommerceClient ecommerceClient;
    @Mock
    private VPosRequestUtils vPosRequestUtils;
    @Mock
    private VPosResponseUtils vPosResponseUtils;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ClientsConfig clientsConfig;

    @Spy
    @InjectMocks
    private VposPatchUtils vposPatchUtils = new VposPatchUtils("http://localhost:8080/", paymentRequestRepository,
            ecommerceClient, vPosRequestUtils, vPosResponseUtils, clientsConfig, httpClient);

    @Test
    public void executePatch_AUTHORIZED_Test() throws IOException {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(AUTHORIZED.name());
        entity.setClientId("clientId");
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);
        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());
        vposPatchUtils.executePatchTransaction(entity, stepZeroRequest);
        verify(vposPatchUtils).executePatchTransaction(entity, stepZeroRequest);
    }

    @Test
    public void executePatch_DENIED_Test() throws IOException {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(DENIED.name());
        entity.setClientId("clientId");
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());

        vposPatchUtils.executePatchTransaction(entity, stepZeroRequest);
        verify(vposPatchUtils).executePatchTransaction(entity, stepZeroRequest);
    }

    @Test
    public void executePatch_OrderStauts_Revert_Test_OK() throws IOException {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(DENIED.name());
        entity.setClientId("clientId");
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");
        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode("00");

        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);
        when(vPosRequestUtils.buildRevertRequestParams(any(), any())).thenReturn(params);
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());

        vposPatchUtils.executePatchTransaction(entity, stepZeroRequest);
        verify(vposPatchUtils).executePatchTransaction(entity, stepZeroRequest);
    }

    @Test
    public void executePatch_OrderStauts_Test_KO() throws IOException {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(DENIED.name());
        entity.setClientId("clientId");
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);
        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode("02");

        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());

        vposPatchUtils.executePatchTransaction(entity, stepZeroRequest);
        verify(vposPatchUtils).executePatchTransaction(entity, stepZeroRequest);
    }

    @Test
    public void executePatch_Revert_Test_KO() throws IOException {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guid");
        entity.setStatus(DENIED.name());
        entity.setClientId("clientId");
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("02");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");
        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode("00");

        when(clientsConfig.getByKey(any())).thenReturn(new ClientConfig());
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);
        when(vPosRequestUtils.buildRevertRequestParams(any(), any())).thenReturn(params);
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());

        vposPatchUtils.executePatchTransaction(entity, stepZeroRequest);
        verify(vposPatchUtils).executePatchTransaction(entity, stepZeroRequest);
    }
}
