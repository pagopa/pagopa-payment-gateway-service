package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.VposDeleteResponse;
import it.pagopa.pm.gateway.dto.vpos.VposOrderStatusResponse;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import it.pagopa.pm.gateway.utils.VPosResponseUtils;
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

import static it.pagopa.pm.gateway.constant.Messages.*;
import static it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum.CANCELLED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VposDeleteService.class)
public class VposDeleteServiceTest {

    public static final String RESULT_CODE_OK = "00";
    public static final String RESULT_CODE_KO = "02";
    public static final String CREATED = "CREATED";
    public static final String AUTHORIZED = "AUTHORIZED";
    private final String UUID_SAMPLE = "8d8b30e3-de52-4f1c-a71c-9905a8043dac";

    @Mock
    private PaymentRequestRepository paymentRequestRepository;
    @Mock
    private VPosResponseUtils vPosResponseUtils;
    @Mock
    private VPosRequestUtils vPosRequestUtils;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;

    @Spy
    @InjectMocks
    private VposDeleteService service = new VposDeleteService(paymentRequestRepository,
            vPosRequestUtils, vPosResponseUtils,
            httpClient, objectMapper, "http://localhost:8080");

    @Test
    public void startDelete_Test_OK() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setIdTransaction("1235");
        entity.setStatus(CANCELLED.name());

        //This param is not validated, so the test doesn't fail
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode(RESULT_CODE_OK);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setResultCode(RESULT_CODE_OK);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildOrderStatusParams(any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);
        when(vPosRequestUtils.buildRevertRequestParams(any(), any())).thenReturn(params);
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);


        VposDeleteResponse resposeTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, null, true);
        resposeTest.setStatus(CANCELLED.name());
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        assertEquals(resposeTest, responseService);
    }

    @Test
    public void startDelete_Test_KO_OrderStatus() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setIdTransaction("1235");
        entity.setStatus(CREATED);

        //This param is not validated, so the test doesn't fail
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode(RESULT_CODE_KO);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildOrderStatusParams(any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);


        VposDeleteResponse responseTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, "Error during orderStatus", false);
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        assertEquals(responseTest, responseService);
    }

    @Test
    public void startDelete_Test_KO_Revert() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setIdTransaction("1235");
        entity.setStatus(CREATED);

        //This param is not validated, so the test doesn't fail
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        VposOrderStatusResponse vposOrderStatusResponse = new VposOrderStatusResponse();
        vposOrderStatusResponse.setResultCode(RESULT_CODE_OK);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setResultCode(RESULT_CODE_KO);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildOrderStatusParams(any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildOrderStatusResponse(any())).thenReturn(vposOrderStatusResponse);
        when(vPosRequestUtils.buildRevertRequestParams(any(), any())).thenReturn(params);
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);


        VposDeleteResponse responseTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, "Error during Revert", false);
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        assertEquals(responseTest, responseService);
    }

    @Test
    public void startDelete_Test_EntityNull() {
        VposDeleteResponse responseTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, REQUEST_ID_NOT_FOUND_MSG, false);
        when(paymentRequestRepository.findByGuid(any())).thenReturn(null);
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        responseTest.setStatus(null);
        assertEquals(responseTest, responseService);
    }

    @Test
    public void startDelete_Test_EntityAlreadyAuthorized() {
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setIdTransaction("1235");
        entity.setIsRefunded(true);
        entity.setStatus(AUTHORIZED);
        VposDeleteResponse responseTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, null, true);
        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        responseTest.setStatus(AUTHORIZED);
        assertEquals(responseTest, responseService);
    }

    @Test
    public void startDelete_Test_Exception_In_getStepZeroRequest() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setIdTransaction("1235");
        entity.setStatus(CREATED);

        VposDeleteResponse resposeTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, GENERIC_REFUND_ERROR_MSG + UUID_SAMPLE, false);

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenThrow(new RuntimeException());

        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);
        assertEquals(resposeTest, responseService);
    }

    @Test
    public void startDelete_Test_Error_In_CallVpos() throws IOException {
        StepZeroRequest stepZeroRequest = ValidBeans.createStep0Request(false);

        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE.name());
        String requestJson = objectMapper.writeValueAsString(stepZeroRequest);
        entity.setJsonRequest(requestJson);
        entity.setIdTransaction("1235");
        entity.setStatus(CREATED);

        //This param is not validated, so the test doesn't fail
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByGuid(any())).thenReturn(entity);
        when(objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class)).thenReturn(stepZeroRequest);
        when(vPosRequestUtils.buildOrderStatusParams(any())).thenReturn(params);
        when(httpClient.callVPos(any(), any())).thenReturn(ValidBeans.createKOHttpClientResponseVPos());


        VposDeleteResponse resposeTest = ValidBeans.createVposDeleteResponse(UUID_SAMPLE, GENERIC_REFUND_ERROR_MSG + UUID_SAMPLE, false);
        VposDeleteResponse responseService = service.startDelete(UUID_SAMPLE);

        assertEquals(resposeTest, responseService);
    }

}
