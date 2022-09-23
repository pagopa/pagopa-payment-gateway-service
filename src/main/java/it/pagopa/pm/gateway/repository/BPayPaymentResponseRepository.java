package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;

@Repository
public interface BPayPaymentResponseRepository extends JpaRepository<BPayPaymentResponseEntity, Long> {

    BPayPaymentResponseEntity findByIdPagoPa(Long idPagoPa);

    BPayPaymentResponseEntity findByCorrelationId(String correlationId);

}
