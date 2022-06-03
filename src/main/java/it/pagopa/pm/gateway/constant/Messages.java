package it.pagopa.pm.gateway.constant;

import lombok.Data;

@Data
public class Messages {
    public static final String BAD_REQUEST_MSG = "Bad Request - mandatory parameters missing";
    public static final String BAD_REQUEST_MSG_CLIENT_ID = "Bad Request - client id is not valid";
    public static final String TRANSACTION_ALREADY_PROCESSED_MSG = "Transaction already processed";
    public static final String SERIALIZATION_ERROR_MSG = "Error while creating json from PostePayAuthRequest object";
    public static final String GENERIC_ERROR_MSG = "Error while requesting authorization for idTransaction: ";
}
