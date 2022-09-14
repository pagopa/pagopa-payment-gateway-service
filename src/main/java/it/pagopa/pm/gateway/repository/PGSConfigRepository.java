package it.pagopa.pm.gateway.repository;

import it.pagopa.pm.gateway.entity.PGSConfigEntity;
import it.pagopa.pm.gateway.entity.PaymentRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PGSConfigRepository extends JpaRepository<PGSConfigEntity, Long> {

}
