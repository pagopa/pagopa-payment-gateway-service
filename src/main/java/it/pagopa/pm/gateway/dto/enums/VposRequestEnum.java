package it.pagopa.pm.gateway.dto.enums;

import lombok.Getter;

import java.math.BigInteger;
import java.util.Date;

@Getter
public enum VposRequestEnum {

    ROOT_RICHIESTA(true, "BPWXmlRichiesta", null, null, 0),
    ROOT_REQUEST(true, "BPWXmlRequest", null, null, 0),
    RELEASE(false, "Release", String.class, null, 2),
    TIMESTAMP(true, "Timestamp", Date.class, "yyyy-MM-dd'T'HH:mm:ss.SSS", 23),
    MAC(true, "MAC", String.class, null, 0),
    REQ_REF_NUM(true, "ReqRefNum", String.class, null, 32),
    PAN(true, "Pan", String.class, null, 19),
    CVV2(true, "CVV2", String.class, null, 4),
    USER_ID(false, "Userid", String.class, null, 30),

    ACCOUNTING(true, "Accounting", null, null, 0),

    AUTH_REQUEST_3DS2_STEP_0(true, "ThreeDSAuthorizationRequest0", null, null, 0),
    AUTH_REQUEST_3DS2_STEP_1(true, "ThreeDSAuthorizationRequest1", null, null, 0),
    AUTH_REQUEST_3DS2_STEP_2(true, "ThreeDSAuthorizationRequest2", null, null, 0),
    THREEDS_DATA(true, "ThreeDSData", String.class, null, 200000),
    NOTIF_URL(true, "NotifUrl", String.class, null, 200000),
    THREEDS_MTD_NOTIF_URL(false, "ThreeDSMtdNotifUrl", String.class, null, 200000),
    EMAIL_CH(false, "EmailCH", String.class, null, 50),
    NAME_CH(false, "NameCH", String.class, null, 45),
    REQUEST(true, "Request", null, null, 0),
    OPERATION(true, "Operation", String.class, null, 0),
    DATA(true, "Data", null, null, 0),
    HEADER(true, "Header", null, null, 0),
    ORDER_ID(true, "OrderID", String.class, null, 50),
    EXP_DATE(true, "ExpDate", String.class, null, 4),
    AMOUNT(true, "Amount",BigInteger .class, null, 8),
    CURRENCY(true, "Currency", String.class, null, 3),
    EXPONENT(false, "Exponent", Integer.class, null, 1),
    ACCOUNTING_MODE(true, "AccountingMode", String.class, null, 1),
    NETWORK(true, "Network", String.class, null, 2),
    OPERATION_DESCRIPTION(false, "OpDescr", String.class, null, 100),
    PRODUCT_REF(false, "ProductRef", String.class, null, 15),
    NAME(false, "Name", String.class, null, 40),
    SURNAME(false, "Surname", String.class, null, 40),
    TAX_ID(false, "TaxID", String.class, null, 16),
    THREEDS_TRANS_ID(true, "ThreeDSTransID", String.class, null, 100),
    SHOP_ID(true, "ShopID", String.class, null, 15),
    OPERATOR_ID(true, "OperatorID", String.class, null, 18),
    THREEDS_METHOD_COMPLETED(true, "ThreeDSMtdComplInd", String.class, null, 2),
    TRANSACTION_ID(true, "TransactionID", String.class, null, 11),

    REQ_STORNO_ONLINE(true, "RicStorno", null, null, 0),
    REFUND(true, "Refund", null, null, 0),

    ORDERSTATUS(true, "OrderStatus", null, null, 0);

    private final boolean mandatory;
    private final String tagName;
    private final Class<?> type;
    private final String format;
    private final int length;

    VposRequestEnum(boolean mandatory, String tagName, Class<?> type, String format, int length) {
        this.mandatory = mandatory;
        this.tagName = tagName;
        this.type = type;
        this.format = format;
        this.length = length;
    }

    public static VposRequestEnum getRootElementIta() {
        return ROOT_RICHIESTA;
    }

    public static VposRequestEnum getRootElementEng() {
        return ROOT_REQUEST;
    }
}
