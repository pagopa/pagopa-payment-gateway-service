package it.pagopa.pm.gateway.dto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@AllArgsConstructor
public enum VposErrorCodeEnum {
    SUCCESS("00", "Success"),
    ORDER_OR_REQREFNUM_NOT_FOUND("01", "Order or ReqRefNum not found"),
    REQREFNUM_INVALID("02", "ReqRefNum duplicated or not valid"),
    INCORRECT_FORMAT("03", "Incorrect message format, missing or incorrect field"),
    INCORRECT_MAC_OR_TIMESTAMP("04", "Incorrect API authentication, incorrect MAC or timestamp exceeding the limit range"),
    INCORRECT_DATE("05", "Incorrect date, or period indicated is empty"),
    UNKNOWN_ERROR("06", "Unforeseen error in the circuit during processing of request"),
    TRANSACTION_ID_NOT_FOUND("07", "TransactionID not found"),
    OPERATOR_NOT_FOUND("08", "Operator indicated not found"),
    TRANSACTION_ID_NOT_CONSISTENT("09", "TRANSACTIONID indicated does not make reference to the entered ORDERID"),
    EXCEEDING_AMOUNT("10", "Amount indicated exceeds maximum amount permitted"),
    INCORRECT_STATUS("11", "Incorrect status. Transaction not possible in the current status"),
    CIRCUIT_DISABLED("12", "Circuit disabled"),
    DUPLICATED_ORDER("13", "Duplicated order"),
    UNSUPPORTED_CURRENCY("16", "Currency not supported or not available for the merchant"),
    UNSUPPORTED_EXPONENT("17", "Exponent not supported for the chosen currency"),
    REDIRECTION_3DS1("20", "The card is VBV/SecureCode/SafeKey-enabled; the reply contains the data for redirection to ACS website"),
    TIMEOUT("21", "Maximum time-limit for forwarding VBV request step 2 expired"),
    METHOD_REQUESTED("25", "A call to 3DS method must be performed by the Requestor"),
    CHALLENGE_REQUESTED("26", "A challenge flow must be initiated by the Requestor"),
    PAYMENT_INSTRUMENT_NOT_ACCEPTED("35", "No payment instrument is acceptable"),
    MISSING_CVV2("37", "Missing CVV2: this is compulsory for the circuit selected"),
    INVALID_PAN("38", "Pan alias not found or revoked"),
    XML_EMPTY("40", "Empty Xml or missing ‘data’ parameter"),
    XML_NOT_PARSABLE("41", "Xml not parsable"),
    INSTALLMENTS_NOT_AVAILABLE("50", "Installments not available"),
    INSTALLMENT_NUMBER_OUT_OF_BOUNDS("51", "Installment number out of bounds (client side)"),
    APPLICATION_ERROR("98", "Application error"),
    TRANSACTION_FAILED("99", "Transaction failed, see specific outcome attached to the element <Data> of the reply");

    @Getter
    private final String code;
    @Getter
    private final String description;

    public static VposErrorCodeEnum getEnumFromCode(String code) {
        return Arrays.stream(VposErrorCodeEnum.values())
                .filter(enumValue -> StringUtils.equals(enumValue.code, code))
                .findFirst().orElse(null);
    }
}