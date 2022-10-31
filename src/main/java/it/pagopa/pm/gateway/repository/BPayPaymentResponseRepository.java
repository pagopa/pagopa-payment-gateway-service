package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.BPayPaymentResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BPayPaymentResponseRepository extends JpaRepository<BPayPaymentResponseEntity, Long> {

    BPayPaymentResponseEntity findByIdPagoPa(Long idPagoPa);

    BPayPaymentResponseEntity findByCorrelationId(String correlationId);

    BPayPaymentResponseEntity findByClientGuid(String requestId);

}
