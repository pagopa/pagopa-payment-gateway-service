package it.pagopa.pm.gateway.entity;


import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "PP_PAYMENT_REQUEST")
public class PaymentRequestEntity {

    @Id
    @Column(name = "ID", nullable = false)
    @SequenceGenerator(name = "SEQ_PAYMENT_REQUEST", sequenceName = "SEQ_PAYMENT_REQUEST", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PAYMENT_REQUEST")
    private Long id;

    @Column(name="ID_TRANSACTION", nullable = false)
    private Long idTransaction;

    @Column(name="TYPE", nullable = false)
    private String type;

    @Column(name="OUTCOME")
    private String outcome;

    @Lob
    @Column(name="REQUEST_JSON")
    private String requestJson;

    @Lob
    @Column(name="RESPONSE_JSON")
    private String responseJson;

}
