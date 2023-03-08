package it.pagopa.pm.gateway.dto.enums;

import lombok.Getter;

public enum VPosResponseEnum {
    ACQ_BIN("AcqBIN"),
    ACQUIRER_BIN("AcquirerBIN"),
    RRN("RRN"),
    DATA_RESPONSE("Data"),
    ORDER_ID_RESPONSE("OrderID"),
    CURRENCY_RESPONSE("Currency"),
    EXPONENT("Exponent"),
    NETWORK_RESPONSE("Network"),
    THREEDS_METHOD("ThreeDSMethod"),
    THREEDS_TRANS_ID("ThreeDSTransId"),
    THREEDS_METHOD_DATA("ThreeDSMethodData"),
    THREEDS_METHOD_URL("ThreeDSMethodUrl"),
    RESULT("Result"),
    THREEDS_CHALLENGE("ThreeDSChallenge"),
    C_REQ("CReq"),
    ACS_URL("ACSUrl"),
    AUTHORIZATION("Authorization"),
    PAYMENT_TYPE("PaymentType"),
    AUTHORIZATION_TYPE("AuthorizationType"),
    TRANSACTION_ID("TransactionID"),
    TRANSACTION_AMOUNT("TransactionAmount"),
    AUTHORIZED_AMOUNT("AuthorizedAmount"),
    ACCOUNTED_AMOUNT("AccountedAmount"),
    REFUNDED_AMOUNT("RefundedAmount"),
    TRANSACTION_RESULT("TransactionResult"),
    AUTHORIZATION_NUMBER("AuthorizationNumber"),
    MERCHANT_ID("MerchantID"),
    TRANSACTION_STATUS("TransactionStatus"),
    RESPONSE_CODE_ISO("ResponseCodeISO"),
    TIMESTAMP_RESPONSE("Timestamp"),
    OPERATION("Operation"),
    HEADER("Header"),
    ORDER_ID("OrderID"),
    OPERATOR_ID("OperatorId"),
    REQ_REF_NUM("ReqRefNum"),
    SHOP_ID("ShopId"),
    PRODUCT_REF("ProductRef"),
    NUMBER_OF_ITEMS("NumberOfItems"),
    NETWORK("Network"),
    CURRENCY("Currency"),
    TIMESTAMP("Timestamp"),
    ORDER_STATUS("OrderStatus"),
    MAC_RESPONSE("MAC");

    @Getter
    private final String tagName;

    VPosResponseEnum(String tagName) {
        this.tagName = tagName;
    }
}
