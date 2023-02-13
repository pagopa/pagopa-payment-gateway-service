package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.ecommerce.EcommerceClient;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.config.VposClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.transaction.TransactionInfo;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.ClientsConfig;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = CcResumeStep2Service.class)
public class CcResumeStep2ServiceTest {
    private final ClientConfig clientConfig = new ClientConfig();

    @Mock
    private ClientsConfig clientsConfig;

    @Spy
    @InjectMocks
    private CcResumeStep2Service service = new CcResumeStep2Service(clientsConfig);

    @Before
    public void init() {
        ReflectionTestUtils.setField(service, "vposUrl", "http://localhost:8080");

        VposClientConfig vposClientConfig= new VposClientConfig();
        vposClientConfig.setClientReturnUrl("url");
        clientConfig.setVpos(vposClientConfig);
    }

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
    private ObjectMapper objectMapper;

    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    @Test
    public void startResume_Test_EntityNull() {
        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Test_EntityAlreadyAuthorized() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setAuthorizationOutcome(true);
        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Test_OK() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.getByKey(any())).thenReturn(clientConfig);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Execpetion_in_processResume() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenThrow(RuntimeException.class);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Execpetion_in_executeStep2() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenThrow(RuntimeException.class);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Test_OK_Error_Response() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        response.setResultCode("99");

        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Exception_in_executeAccount() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.getByKey(any())).thenReturn(clientConfig);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenThrow(RuntimeException.class);
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Accounting_KO() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("99");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.getByKey(any())).thenReturn(clientConfig);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenReturn(new TransactionInfo());

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_callVpos_KO() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createKOHttpClientResponseVPos());

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Exception_in_executePatchTransaction() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(clientsConfig.getByKey(any())).thenReturn(clientConfig);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(ecommerceClient.callPatchTransaction(any(), any(), any())).thenThrow(RuntimeException.class);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }
}
