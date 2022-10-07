package it.pagopa.pm.gateway.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Messages {
    public static final String BAD_REQUEST_MSG = "Bad Request - mandatory parameters missing";
    public static final String BAD_REQUEST_MSG_CLIENT_ID = "Bad Request - client id is not valid";
    public static final String TRANSACTION_ALREADY_PROCESSED_MSG = "Transaction already processed";
    public static final String SERIALIZATION_ERROR_MSG = "Error while creating json from PostePayAuthRequest object";
    public static final String GENERIC_ERROR_MSG = "Error while requesting authorization for idTransaction: ";
    public static final String REQUEST_ID_NOT_FOUND_MSG = "RequestId not Found";


}
