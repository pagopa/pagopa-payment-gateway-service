package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequestEntity, Long> {

    PaymentRequestEntity findByIdTransaction(Long idTransaction);

    PaymentRequestEntity findByGuid(String guid);

}
