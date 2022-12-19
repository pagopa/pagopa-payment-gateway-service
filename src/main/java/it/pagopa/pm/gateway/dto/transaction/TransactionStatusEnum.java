package it.pagopa.pm.gateway.dto.transaction;

public enum TransactionStatusEnum {
    ACTIVATION_REQUESTED,
    ACTIVATED,
    AUTHORIZATION_REQUESTED,
    AUTHORIZED,
    AUTHORIZATION_FAILED,
    CLOSED,
    CLOSURE_FAILED,
    NOTIFIED,
    NOTIFIED_FAILED,
    EXPIRED,
    REFUNDED
}
