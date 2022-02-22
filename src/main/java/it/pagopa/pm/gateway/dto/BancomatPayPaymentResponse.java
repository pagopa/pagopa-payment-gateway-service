package it.pagopa.pm.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "PP_BANCOMAT_PAY_PAYMENT_RESPONSE")
public class BancomatPayPaymentResponse {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = "SEQ_BPAY_PAYMENT_RESPONSE", sequenceName = "SEQ_BPAY_PAYMENT_RESPONSE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BPAY_PAYMENT_RESPONSE")
    private Long id;

    @Column(name = "ID_PAGOPA", nullable = false)
    private Long idPagoPa;

    @Column(name = "OUTCOME", nullable = false)
    private Boolean outcome;

    @Column(name = "ERROR_CODE", nullable = false)
    private String errorCode;

    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "CORRELATION_ID", nullable = false)
    private String correlationId;

}
