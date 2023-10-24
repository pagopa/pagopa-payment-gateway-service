package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface PaymentRequestLockRepository extends JpaRepository<PaymentRequestEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    PaymentRequestEntity findByGuid(String guid);

}
