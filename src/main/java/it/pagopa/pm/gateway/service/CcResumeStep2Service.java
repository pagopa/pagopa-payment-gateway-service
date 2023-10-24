package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestLockRepository;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.CcResumeStep2AsyncService;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@Transactional
@NoArgsConstructor
public class CcResumeStep2Service {

    private PaymentRequestLockRepository paymentRequestLockRepository;
    private PaymentRequestRepository paymentRequestRepository;
    private VPosRequestUtils vPosRequestUtils;
    private ObjectMapper objectMapper;
    private CcResumeStep2AsyncService ccResumeStep2AsyncService;

    @Autowired
    public CcResumeStep2Service(PaymentRequestLockRepository paymentRequestLockRepository,
                                PaymentRequestRepository paymentRequestRepository,
                                VPosRequestUtils vPosRequestUtils, ObjectMapper objectMapper,
                                CcResumeStep2AsyncService ccResumeStep2AsyncService) {
        this.paymentRequestLockRepository = paymentRequestLockRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.vPosRequestUtils = vPosRequestUtils;
        this.objectMapper = objectMapper;
        this.ccResumeStep2AsyncService = ccResumeStep2AsyncService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareResumeStep2(String requestId) {
        PaymentRequestEntity entity = paymentRequestLockRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error("No CreditCard request entity has been found for requestId: {}", requestId);
            return false;
        }

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn("requestId {} already processed", requestId);
            entity.setErrorMessage("requestId already processed");
            return false;
        }

        String responseType = entity.getResponseType();
        if (PaymentRequestStatusEnum.CREATED.name().equals(entity.getStatus())
                && responseType != null
                && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.CHALLENGE.name())) {
            log.info("prepareResumeStep2 request in state CREATED CHALLENGE - proceed for requestId: {}", requestId);
            entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
            paymentRequestLockRepository.save(entity);
            return true;
        } else {
            log.info("prepareResumeStep2 request in state {} {} - not proceed for requestId: {}", entity.getStatus(), responseType, requestId);
        }

        return false;
    }

    public void startResumeStep2(String requestId) {
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        processResume(entity);
    }

    private void processResume(PaymentRequestEntity entity) {
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
            log.error("error during execution of resume for requestId {}", entity.getGuid(), e);
            if (PaymentRequestStatusEnum.PROCESSING.name().equals(entity.getStatus())) {
                entity.setStatus(PaymentRequestStatusEnum.CREATED.name());
                paymentRequestRepository.save(entity);
            }
        }
    }
}
