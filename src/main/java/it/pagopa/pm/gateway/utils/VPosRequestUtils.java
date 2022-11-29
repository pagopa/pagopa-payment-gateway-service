package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.creditcard.Step0CreditCardRequest;
import it.pagopa.pm.gateway.dto.enums.VposRequestEnum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static it.pagopa.pm.gateway.dto.enums.VposRequestEnum.*;
import static it.pagopa.pm.gateway.utils.VPosUtils.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosRequestUtils {

    private static final String RELEASE_VALUE = "02";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
    private static final String PARAM_DATA = "data";
    private static final String OPERATION_AUTH_REQUEST_3DS2_STEP_0 = "THREEDSAUTHORIZATION0";
    private static final String CURRENCY_VALUE = "978";
    private static final String ACCOUNT_DEFERRED = "D";
    private static final String FAKE_DESCRIPTION = "Pagamenti PA";
    private static final String OPERATION_ACCOUNTING = "ACCOUNTING";
    public static final String OPERATION_REFUND = "REFUND";
    public static final String ERROR_MSG = "Error";
    public static final String ERROR_CONSTRUCTING_REQUEST_BODY = "Error constructing requestBody";

    @Value("${vpos.request.responseUrl}")
    private String vposResponseUrl;

    @Autowired
    VPosUtils vPosUtils;

    public Map<String, String> createStepZeroRequest(Step0CreditCardRequest pgsRequest, String requestId) throws IOException {
        Map<String, String> params;
        try {
            String shopId;
            String terminalId;
            String mac;
            List<String> shopParameters = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                shopId = shopParameters.get(SHOP_ID_FIRST_PAY_POSITION);
                terminalId = shopParameters.get(TERMINAL_ID_FIRST_PAY_POSITION);
                mac = shopParameters.get(MAC_FIRST_PAY_POSITION);
            } else {
                shopId = shopParameters.get(SHOP_ID_NEXT_PAY_POSITION);
                terminalId = shopParameters.get(TERMINAL_ID_NEXT_PAY_POSITION);
                mac = shopParameters.get(MAC_NEXT_PAY_POSITION);
            }
            params = getParams(buildRequestStep0(pgsRequest, shopId, terminalId, mac, requestId));
        } catch (Exception e) {
            log.error(ERROR_CONSTRUCTING_REQUEST_BODY);
            throw e;
        }
        return params;
    }

    public Map<String, String> generateRequestForAccount(Step0CreditCardRequest pgsRequest) throws IOException {
        Map<String, String> params;
        try {
            List<String> variables = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
            String shopId;
            String terminalId;
            String mac;
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                shopId = variables.get(2);
                terminalId = variables.get(3);
                mac = variables.get(4);
            } else {
                shopId = variables.get(5);
                terminalId = variables.get(6);
                mac = variables.get(7);
            }
            params = getParams(buildRequestForAccount(pgsRequest, shopId, terminalId, mac));
        } catch (Exception e) {
            log.error(ERROR_CONSTRUCTING_REQUEST_BODY);
            throw e;
        }
        return params;
    }

    public Map<String, String> generateRequestForRevert(Step0CreditCardRequest pgsRequest) throws IOException {
        Map<String, String> params;
        try {
            List<String> variables = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
            String shopId;
            String terminalId;
            String mac;
            if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
                shopId = variables.get(2);
                terminalId = variables.get(3);
                mac = variables.get(4);
            } else {
                shopId = variables.get(5);
                terminalId = variables.get(6);
                mac = variables.get(7);
            }
            params = getParams(buildRequestForRevert(pgsRequest, shopId, terminalId, mac));
        } catch (Exception e) {
            log.error(ERROR_CONSTRUCTING_REQUEST_BODY);
            throw e;
        }
        return params;
    }

    private Document buildRequestStep0(Step0CreditCardRequest pgsRequest, String shopId, String terminalId, String mac, String requestId) {
        String notifyUrl = String.format(vposResponseUrl, requestId);
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        try {
            Date date = new Date();
            Element macElement = new Element(MAC.getTagName());
            documentBuilder.addElement(RELEASE, RELEASE_VALUE);
            //REQUEST
            documentBuilder.addBodyElement(REQUEST);
            documentBuilder.addElement(REQUEST, OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_0);
            documentBuilder.addElement(REQUEST, TIMESTAMP, date);
            documentBuilder.addElement(REQUEST, macElement);
            //DATA
            documentBuilder.addBodyElement(DATA);
            documentBuilder.addBodyElement(DATA, AUTH_REQUEST_3DS2_STEP_0);
            //HEADER
            documentBuilder.addBodyElement(AUTH_REQUEST_3DS2_STEP_0, HEADER);
            documentBuilder.addElement(HEADER, SHOP_ID, shopId);
            documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
            documentBuilder.addElement(HEADER, REQ_REF_NUM, pgsRequest.getReqRefNumber());
            //AUTH_REQUEST_3DS2_STEP_0
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, ORDER_ID, pgsRequest.getIdTransaction());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, PAN, pgsRequest.getPan());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, CVV2, pgsRequest.getSecurityCode());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, EXP_DATE, pgsRequest.getExpireDate());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, AMOUNT, pgsRequest.getAmount());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, CURRENCY, CURRENCY_VALUE);
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, ACCOUNTING_MODE, ACCOUNT_DEFERRED);
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NETWORK, pgsRequest.getCircuit().getCode());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, THREEDS_DATA, pgsRequest.getThreeDsData());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NOTIF_URL, notifyUrl);
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, THREEDS_MTD_NOTIF_URL, notifyUrl);
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, USER_ID, pgsRequest.getHolder());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NAME_CH, pgsRequest.getHolder());
            documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, EMAIL_CH, pgsRequest.getEmailCH());
            //MAC
            VPosMacBuilder macBuilder = calculateMacStep0(date, shopId, pgsRequest, terminalId, mac, notifyUrl);
            macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        } catch (Exception e) {
            log.error(ERROR_MSG);
            throw e;
        }
        return documentBuilder.build();
    }

    private Document buildRequestForAccount(Step0CreditCardRequest pgsRequest, String shopId, String terminalId, String mac) {
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        try {
            Date date = new Date();
            Element macElement = new Element(MAC.getTagName());
            documentBuilder.addElement(VposRequestEnum.RELEASE, RELEASE_VALUE);
            //REQUEST
            documentBuilder.addBodyElement(REQUEST);
            documentBuilder.addElement(REQUEST, OPERATION, OPERATION_ACCOUNTING);
            documentBuilder.addElement(REQUEST, TIMESTAMP, date);
            documentBuilder.addElement(REQUEST, macElement);
            //DATA
            documentBuilder.addBodyElement(DATA);
            documentBuilder.addBodyElement(DATA, ACCOUNTING);
            documentBuilder.addBodyElement(ACCOUNTING, HEADER);
            //HEADER
            documentBuilder.addElement(HEADER, SHOP_ID, shopId);
            documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
            documentBuilder.addElement(HEADER, REQ_REF_NUM, pgsRequest.getReqRefNumber());
            //ACCOUNTING
            documentBuilder.addElement(ACCOUNTING, TRANSACTION_ID, pgsRequest.getIdTransaction());
            documentBuilder.addElement(ACCOUNTING, ORDER_ID, pgsRequest.getIdTransaction());
            documentBuilder.addElement(ACCOUNTING, AMOUNT, pgsRequest.getAmount());
            documentBuilder.addElement(ACCOUNTING, CURRENCY, CURRENCY_VALUE);
            documentBuilder.addElement(ACCOUNTING, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
            //MAC
            VPosMacBuilder macBuilder = calculateMac(date, shopId, pgsRequest, terminalId, mac, ACCOUNTING);
            macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        } catch (Exception e) {
            log.error(ERROR_MSG);
            throw e;
        }
        return documentBuilder.build();
    }

    private Document buildRequestForRevert(Step0CreditCardRequest pgsRequest, String shopId, String terminalId, String mac) {
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        try {
            Date date = new Date();
            Element macElement = new Element(MAC.getTagName());
            documentBuilder.addElement(VposRequestEnum.RELEASE, RELEASE_VALUE);
            //REQUEST
            documentBuilder.addBodyElement(REQUEST);
            documentBuilder.addElement(REQUEST, OPERATION, OPERATION_REFUND);
            documentBuilder.addElement(REQUEST, TIMESTAMP, date);
            documentBuilder.addElement(REQUEST, macElement);
            //DATA
            documentBuilder.addBodyElement(DATA);
            documentBuilder.addBodyElement(DATA, REFUND);
            documentBuilder.addBodyElement(REFUND, HEADER);
            //HEADER
            documentBuilder.addElement(HEADER, SHOP_ID, shopId);
            documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
            documentBuilder.addElement(HEADER, REQ_REF_NUM, pgsRequest.getReqRefNumber());
            //ACCOUNTING
            documentBuilder.addElement(REFUND, TRANSACTION_ID, pgsRequest.getIdTransaction());
            documentBuilder.addElement(REFUND, ORDER_ID, pgsRequest.getIdTransaction());
            documentBuilder.addElement(REFUND, AMOUNT, pgsRequest.getAmount());
            documentBuilder.addElement(REFUND, CURRENCY, CURRENCY_VALUE);
            documentBuilder.addElement(REFUND, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
            //MAC
            VPosMacBuilder macBuilder = calculateMac(date, shopId, pgsRequest, terminalId, mac, REFUND);
            macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        } catch (Exception e) {
            log.error(ERROR_MSG);
            throw e;
        }
        return documentBuilder.build();
    }

    private VPosMacBuilder calculateMacStep0(Date date, String shopId, Step0CreditCardRequest pgsRequest, String terminalId, String mac, String notifyUrl) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_0);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, pgsRequest.getReqRefNumber());
        macBuilder.addElement(PAN, pgsRequest.getPan());
        macBuilder.addElement(CVV2, pgsRequest.getSecurityCode());
        macBuilder.addElement(EXP_DATE, pgsRequest.getExpireDate());
        macBuilder.addElement(AMOUNT, pgsRequest.getAmount());
        macBuilder.addElement(ACCOUNTING_MODE, ACCOUNT_DEFERRED);
        macBuilder.addElement(NETWORK, pgsRequest.getCircuit());
        macBuilder.addElement(EMAIL_CH, pgsRequest.getEmailCH());
        macBuilder.addElement(USER_ID, pgsRequest.getHolder());
        macBuilder.addElement(OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        macBuilder.addElement(THREEDS_DATA, pgsRequest.getThreeDsData());
        macBuilder.addElement(NOTIF_URL, notifyUrl);
        macBuilder.addElement(THREEDS_MTD_NOTIF_URL, notifyUrl);
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMac(Date date, String shopId, Step0CreditCardRequest pgsRequest, String terminalId, String mac, VposRequestEnum operation) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, operation);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, pgsRequest.getReqRefNumber());
        macBuilder.addElement(AMOUNT, pgsRequest.getAmount());
        macBuilder.addElement(EMAIL_CH, pgsRequest.getEmailCH());
        macBuilder.addElement(CURRENCY, CURRENCY_VALUE);
        macBuilder.addElement(OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        macBuilder.addString(mac);
        return macBuilder;
    }

    private Map<String, String> getParams(Document document) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Format format = Format.getCompactFormat();
            format.setOmitDeclaration(false);
            format.setEncoding(DEFAULT_CHARSET.displayName());
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(format);
            xmlOutput.output(document, outputStream);
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_DATA, new String(outputStream.toByteArray(), DEFAULT_CHARSET));
            return params;
        }
    }

}
