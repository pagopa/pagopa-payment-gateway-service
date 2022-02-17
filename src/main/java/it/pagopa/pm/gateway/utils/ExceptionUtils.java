package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.exception.*;
import lombok.extern.log4j.Log4j;

@Log4j
public class ExceptionUtils {

    public static void handleRestException(Exception e) throws RestApiException {
        if (e instanceof BusinessException) {
            log.warn("BusinessException: " + e);
            throw new RestApiUnprocessableEntityException(((BusinessException) e).getException().getRestApiCode(), e.getMessage());
        } else if (e instanceof BancomatPayClientException ){


        }
        else {
            log.warn("Exception: ", e);
            throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
        }
    }

    /*public static void handleNotFoundRestException(Exception e) throws RestApiException {
        if (e instanceof BusinessException) {
            ExceptionsEnum ee = ExceptionsEnum.fromCode(((BusinessException) e).getException().getCode());
            if (StringUtils.equalsAny(ee.getCode(), PAYMENT_NOT_FOUND.getCode(), PSP_NOT_FOUND.getCode())) {
                throw new RestApiNotFoundException(ee.getRestApiCode(), e.getMessage());
            }
        }
        handleRestException(e);
    }

    public static void handleRestExceptionNoLogs(Exception e) throws RestApiNoLogException, RestApiInternalException {
        if (e.getCause() instanceof BusinessRuntimeException) {
            BusinessRuntimeException bre = (BusinessRuntimeException) e.getCause();
            throw new RestApiNoLogException(bre.getException().getRestApiCode(), bre.getMessage());
        } else {
            log.error(e.getMessage(), e);
            throw new RestApiInternalException(ExceptionsEnum.GENERIC_ERROR.getRestApiCode(), ExceptionsEnum.GENERIC_ERROR.getDescription());
        }
    } */

}
