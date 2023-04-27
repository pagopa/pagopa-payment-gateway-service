package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.CcResumeStep2AsyncService;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@NoArgsConstructor
public class CcResumeStep2Service {

    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private ObjectMapper objectMapper;
    private CcResumeStep2AsyncService ccResumeStep2AsyncService;

    @Autowired
    public CcResumeStep2Service(PaymentRequestRepository paymentRequestRepository,
                                VPosRequestUtils vPosRequestUtils, ObjectMapper objectMapper,
                                CcResumeStep2AsyncService ccResumeStep2AsyncService) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.objectMapper = objectMapper;
        this.ccResumeStep2AsyncService = ccResumeStep2AsyncService;
    }

    public void startResumeStep2(String requestId) {
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        if (Objects.isNull(entity)) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            return;
        }

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn(String.format("requestId %s already processed", requestId));
            entity.setErrorMessage("requestId already processed");
            return;
        }

        processResume(entity, requestId);
    }

    private void processResume(PaymentRequestEntity entity, String requestId) {
        String responseType = entity.getResponseType();
        String correlationId = entity.getCorrelationId();
        try {
            StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
            stepZeroRequest.setIsFirstPayment(false);
            if (Objects.nonNull(responseType) && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.CHALLENGE.name())) {
                Map<String, String> params = vPosRequestUtils.buildStepTwoRequestParams(stepZeroRequest, correlationId);
                ccResumeStep2AsyncService.executeStep2(params, entity, stepZeroRequest);
            }
        } catch (Exception e) {
            log.error("error during execution of resume for requestId {}", requestId, e);
        }
    }
}
