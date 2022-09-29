package it.pagopa.pm.gateway.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "PP_PGS_RESPONSE_INFO")
public class PaymentResponseEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = "SEQ_PGS_RESPONSE_INFO", sequenceName = "SEQ_PGS_RESPONSE_INFO", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PGS_RESPONSE_INFO")
    private Long id;

    @Column(name = "REQUEST_ID")
    private String requestId;

    @Column(name = "OPERATION_ID")
    private String operationId;

    @Column(name = "RESPONSE_TIMESTAMP")
    private String timeStamp;

    @Column(name = "HTML")
    private String html;

    @Column(name = "ERROR_CODE")
    private String errorCode;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "MAC")
    private String mac;

    @Column(name = "NONCE")
    private String nonce;
}
