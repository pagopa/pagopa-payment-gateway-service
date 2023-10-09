package it.pagopa.pm.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pm.gateway.dto.creditcard.CreditCardResumeRequest;
import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.PaymentRequestStatusEnum;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.vpos.MethodCompletedEnum;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import it.pagopa.pm.gateway.repository.PaymentRequestLockRepository;
import it.pagopa.pm.gateway.repository.PaymentRequestRepository;
import it.pagopa.pm.gateway.service.async.CcResumeStep1AsyncService;
import it.pagopa.pm.gateway.utils.VPosRequestUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@NoArgsConstructor
public class CcResumeStep1Service {
    public static final String RESULT_CODE_METHOD = "26";

    @Value("${vpos.requestUrl}")
    private String vposUrl;

    @Autowired
    private PaymentRequestLockRepository paymentRequestLockRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private VPosRequestUtils vPosRequestUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CcResumeStep1AsyncService ccResumeStep1AsyncService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareResumeStep1(String requestId) {
        PaymentRequestEntity entity = paymentRequestLockRepository.findByGuid(requestId);

        if (Objects.isNull(entity)) {
            log.error("No CreditCard request entity has been found for requestId: " + requestId);
            return false;
        }

        if (Objects.nonNull(entity.getAuthorizationOutcome())) {
            log.warn(String.format("requestId %s already processed", requestId));
            entity.setErrorMessage("requestId already processed");
            return false;
        }

        String responseType = entity.getResponseType();
        if (PaymentRequestStatusEnum.CREATED.name().equals(entity.getStatus())
                && responseType != null
                && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.CHALLENGE.name())) {
            entity.setStatus(PaymentRequestStatusEnum.PROCESSING.name());
            paymentRequestLockRepository.save(entity);
            return true;
        }

        return false;
    }

    public void startResumeStep1(CreditCardResumeRequest request, String requestId) {
        PaymentRequestEntity entity = paymentRequestRepository.findByGuid(requestId);
        processResume(request, entity);
    }

    private void processResume(CreditCardResumeRequest request, PaymentRequestEntity entity) {
        String methodCompleted = request.getMethodCompleted();
        String responseType = entity.getResponseType();
        String correlationId = entity.getCorrelationId();
        try {
            StepZeroRequest stepZeroRequest = objectMapper.readValue(entity.getJsonRequest(), StepZeroRequest.class);
            stepZeroRequest.setIsFirstPayment(false);
            MethodCompletedEnum methodCompletedEnum = MethodCompletedEnum.valueOf(methodCompleted);
            if (Objects.nonNull(responseType) && responseType.equalsIgnoreCase(ThreeDS2ResponseTypeEnum.METHOD.name())) {
                Map<String, String> params = vPosRequestUtils.buildStepOneRequestParams(methodCompletedEnum, stepZeroRequest, correlationId);
                ccResumeStep1AsyncService.executeStep1(params, entity, stepZeroRequest);
            }
        } catch (Exception e) {
            log.error("error during execution of resume for requestId {}", entity.getGuid(), e);
        }
    }
}
