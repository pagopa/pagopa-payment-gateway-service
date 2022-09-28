package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.PaymentResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentResponseRepository extends JpaRepository<PaymentResponseEntity, Long> {
}
