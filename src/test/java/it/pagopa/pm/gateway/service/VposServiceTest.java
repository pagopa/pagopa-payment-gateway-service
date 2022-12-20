package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.beans.ValidBeans;
import it.pagopa.pm.gateway.client.restapicd.RestapiCdClientImpl;
import it.pagopa.pm.gateway.client.vpos.HttpClient;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroResponse;
import it.pagopa.pm.gateway.dto.vpos.AuthResponse;
import it.pagopa.pm.gateway.dto.vpos.ThreeDS2Response;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.Messages.BAD_REQUEST_MSG_CLIENT_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = VposService.class)
public class VposServiceTest {

    @Spy
    @InjectMocks
    private VposService service = new VposService();

    @Before
    public void setUpProperties() {
        ReflectionTestUtils.setField(service, "responseUrlRedirect", "http://localhost:8080/payment-gateway/");
        ReflectionTestUtils.setField(service, "vposUrl", "http://localhost:8080");
    }

    @Mock
    private PaymentRequestRepository paymentRequestRepository;
    @Mock
    private RestapiCdClientImpl restapiCdClient;
    @Mock
    private VPosRequestUtils vPosRequestUtils;
    @Mock
    private VPosResponseUtils vPosResponseUtils;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void getRequestPayment_Invalid_ClientId_Test_400() {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.BAD_REQUEST, null);
        mockResponse.setError(BAD_REQUEST_MSG_CLIENT_ID);

        StepZeroResponse realResponse = service.startCreditCardPayment("invalido", null, requestOK);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_Invalid_Request_Test_400() {
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        request.setAmount(BigInteger.ZERO);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.BAD_REQUEST, null);

        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, request);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_Invalid_IdTransaction_Test_401() {
        StepZeroRequest request = ValidBeans.createStep0Request(false);
        PaymentRequestEntity entity = new PaymentRequestEntity();
        entity.setGuid("guidProvaRandoma");
        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(entity);

        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.UNAUTHORIZED, null);

        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, request);
        assertEquals(mockResponse, realResponse);
    }

    @Test
    public void getRequestPayment_No_FirstPayment_Test_OK() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(restapiCdClient.callPatchTransactionV2(any(), any())).thenReturn("OK");
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }


    @Test
    public void getRequestPayment_FirstPayment_Test_200() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when(vPosRequestUtils.buildRevertRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Test_500() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenThrow(RuntimeException.class);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.INTERNAL_SERVER_ERROR, requestOK.getIdTransaction());
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Error_During_Payment_Test_200() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createKOHttpClientResponseVPos());
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Error_During_Account_Test_200() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos()).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Error_During_Revert_Test_200() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(true);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos()).thenThrow(RuntimeException.class);
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildRevertRequestParams(any(), any()))).thenReturn(params);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Error_During_Patch_Test_200() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("00");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(restapiCdClient.callPatchTransactionV2(any(), any())).thenThrow(RuntimeException.class);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_No_To_Account_Test_OK() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        AuthResponse authResponse = ValidBeans.createVPosAuthResponse("12");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        when((vPosRequestUtils.buildAccountingRequestParams(any(), any()))).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.buildAuthResponse(any())).thenReturn(authResponse);
        when(restapiCdClient.callPatchTransactionV2(any(), any())).thenReturn("OK");
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Not_Authorized_METHOD_Test_OK() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Method();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Not_Authorized_CHALLENGE_Test_OK() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Challenge();
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }

    @Test
    public void getRequestPayment_Status_Denied_Test_OK() throws IOException {
        StepZeroRequest requestOK = ValidBeans.createStep0Request(false);
        ThreeDS2Response response = ValidBeans.createThreeDS2ResponseStep0Authorization();
        response.setResultCode("32");
        Map<String, String> params = new HashMap<>();
        params.put("1", "prova");

        when(paymentRequestRepository.findByIdTransaction(any())).thenReturn(null);
        when(vPosRequestUtils.buildStepZeroRequestParams(any(), any())).thenReturn(params);
        when(httpClient.post(any(), any(), any())).thenReturn(ValidBeans.createHttpClientResponseVPos());
        when(vPosResponseUtils.build3ds2Response(any())).thenReturn(response);
        StepZeroResponse mockResponse = ValidBeans.createStepzeroResponse(HttpStatus.OK, null);
        StepZeroResponse realResponse = service.startCreditCardPayment("APP", null, requestOK);
        assertEquals(mockResponse.getStatus(), realResponse.getStatus());
    }
}