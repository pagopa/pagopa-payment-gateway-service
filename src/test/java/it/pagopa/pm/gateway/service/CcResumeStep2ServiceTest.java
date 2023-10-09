package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.config.ClientConfig;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestLockRepository;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.CcResumeStep2AsyncService;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
import org.junit.Assert;
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
    private PaymentRequestLockRepository paymentRequestLockRepository;
    @Mock
    private PaymentRequestRepository paymentRequestRepository;
    @Mock
    private VPosRequestUtils vPosRequestUtils;
    @Mock
    private VPosResponseUtils vPosResponseUtils;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;
    //    @Mock
//    private EcommercePatchUtils ecommercePatchUtils;
    @Spy
    @InjectMocks
    private CcResumeStep2Service service;
    @Spy
    @InjectMocks
    private CcResumeStep2AsyncService asyncService;

    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(asyncService, "vposUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "ccResumeStep2AsyncService", asyncService);
    }

    @Test
    public void prepareResume_Test_EntityNull() {
        when(paymentRequestLockRepository.findByGuid(any())).thenReturn(null);
        boolean check = service.prepareResumeStep2(UUID_SAMPLE);
        verify(service).prepareResumeStep2(UUID_SAMPLE);
        Assert.assertFalse(check);
    }

    @Test
    public void prepareResume_Test_EntityAlreadyAuthorized() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setAuthorizationOutcome(true);
        when(paymentRequestLockRepository.findByGuid(any())).thenReturn(entity);
        boolean check = service.prepareResumeStep2(UUID_SAMPLE);
        verify(service).prepareResumeStep2(UUID_SAMPLE);
        Assert.assertFalse(check);
    }

    @Test
    public void prepareResume_Test_EntityAlreadyInProcessing() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        when(paymentRequestLockRepository.findByGuid(any())).thenReturn(entity);
        boolean check = service.prepareResumeStep2(UUID_SAMPLE);
        verify(service).prepareResumeStep2(UUID_SAMPLE);
        Assert.assertFalse(check);
    }

    @Test
    public void prepareResume_Test_OK() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.CREATED.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        when(paymentRequestLockRepository.findByGuid(any())).thenReturn(entity);
        when(paymentRequestLockRepository.save(any())).thenReturn(entity);
        boolean check = service.prepareResumeStep2(UUID_SAMPLE);
        verify(service).prepareResumeStep2(UUID_SAMPLE);
        Assert.assertTrue(check);
    }

    @Test
    public void startResume_STEP_2_Test_OK() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
//        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
//        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
//        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Execpetion_in_processResume() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenThrow(RuntimeException.class);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Execpetion_in_executeStep2() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenThrow(RuntimeException.class);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_STEP_2_Test_OK_Error_Response() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        response.setResultCode("99");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Exception_in_executeAccount() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
//        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
//        when(vPosResponseUtils.buildAuthResponse(any())).thenThrow(RuntimeException.class);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_Accounting_KO() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
//        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("99");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
//        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
//        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }

    @Test
    public void startResume_callVpos_KO() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setCorrelationId("CorrelationId");
        entity.setIdTransaction("1234566");

        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildStepTwoRequestParams(any(), any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createKOHttpClientResponseVPos());

        service.startResumeStep2(UUID_SAMPLE);
        verify(service).startResumeStep2(UUID_SAMPLE);
    }
}
