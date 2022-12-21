package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.VposRequestEnum;
import it.pagopa.pm.gateway.dto.vpos.MethodCompletedEnum;
import it.pagopa.pm.gateway.dto.vpos.Shop;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static it.pagopa.pm.gateway.constant.VposConstant.*;
import static it.pagopa.pm.gateway.dto.enums.VposRequestEnum.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosRequestUtils {

    private static final String STEP_2 = "/step2";
    private static final String STEP_1 = "/step1";
    @Value("${vpos.request.responseUrl}")
    private String vposResponseUrl;

    @Autowired
    VPosUtils vPosUtils;

    private String shopId;
    private String terminalId;
    private String mac;

    private void retrieveShopInformation(StepZeroRequest pgsRequest) {
        Shop shopParameters = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
        if (BooleanUtils.isTrue(pgsRequest.getIsFirstPayment())) {
            shopId = shopParameters.getShopIdFirstPayment();
            terminalId = shopParameters.getTerminalIdFirstPayment();
            mac = shopParameters.getMacFirstPayment();
        } else {
            shopId = shopParameters.getShopIdSuccPayment();
            terminalId = shopParameters.getTerminalIdSuccPayment();
            mac = shopParameters.getMacSuccPayment();
        }
    }

    public Map<String, String> buildStepZeroRequestParams(StepZeroRequest pgsRequest, String requestId) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document stepZeroRequest = buildStepZeroRequest(pgsRequest, shopId, terminalId, mac, requestId);
        return getParams(stepZeroRequest);
    }

    public Map<String, String> buildAccountingRequestParams(StepZeroRequest pgsRequest, String correlationId) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document accountingRequest = buildAccountingRequest(pgsRequest, shopId, terminalId, mac, correlationId);
        return getParams(accountingRequest);
    }

    public Map<String, String> buildRevertRequestParams(StepZeroRequest pgsRequest, String correlationId) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document revertRequest = buildRevertRequest(pgsRequest, shopId, terminalId, mac, correlationId);
        return getParams(revertRequest);
    }

    public Map<String, String> buildStepOneRequestParams(MethodCompletedEnum methodCompletedEnum, StepZeroRequest pgsRequest, String correlationId) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document stepOneRequest = buildStepOneRequest(methodCompletedEnum, correlationId);
        return getParams(stepOneRequest);
    }

    public Map<String, String> buildStepTwoRequestParams(StepZeroRequest pgsRequest, String correlationId) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document stepTwoRequest = buildStepTwoRequest(correlationId);
        return getParams(stepTwoRequest);
    }

    private Document buildStepZeroRequest(StepZeroRequest pgsRequest, String shopId, String terminalId, String mac, String requestId) {
        String notifyUrl = String.format(vposResponseUrl, requestId);
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
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
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
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
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NOTIF_URL, notifyUrl + STEP_2);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, THREEDS_MTD_NOTIF_URL, notifyUrl + STEP_1);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, USER_ID, pgsRequest.getHolder());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NAME_CH, pgsRequest.getHolder());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, EMAIL_CH, pgsRequest.getEmailCH());
        //MAC
        VPosMacBuilder macBuilder = calculateMacStep0(date, shopId, pgsRequest, terminalId, mac, notifyUrl, reqRefNum);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private Document buildAccountingRequest(StepZeroRequest pgsRequest, String shopId, String terminalId, String mac, String correlationId) {
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
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
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
        //ACCOUNTING
        documentBuilder.addElement(ACCOUNTING, TRANSACTION_ID, correlationId);
        documentBuilder.addElement(ACCOUNTING, ORDER_ID, pgsRequest.getIdTransaction());
        documentBuilder.addElement(ACCOUNTING, AMOUNT, pgsRequest.getAmount());
        documentBuilder.addElement(ACCOUNTING, CURRENCY, CURRENCY_VALUE);
        documentBuilder.addElement(ACCOUNTING, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        //MAC
        VPosMacBuilder macBuilder = calculateMac(date, shopId, pgsRequest, terminalId, mac, ACCOUNTING, reqRefNum);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private Document buildRevertRequest(StepZeroRequest pgsRequest, String shopId, String terminalId, String mac, String correlationId) {
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
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
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
        //ACCOUNTING
        documentBuilder.addElement(REFUND, TRANSACTION_ID, pgsRequest.getIdTransaction());
        //REFUND
        documentBuilder.addElement(REFUND, TRANSACTION_ID, correlationId);
        documentBuilder.addElement(REFUND, ORDER_ID, pgsRequest.getIdTransaction());
        documentBuilder.addElement(REFUND, AMOUNT, pgsRequest.getAmount());
        documentBuilder.addElement(REFUND, CURRENCY, CURRENCY_VALUE);
        documentBuilder.addElement(REFUND, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        //MAC
        VPosMacBuilder macBuilder = calculateMac(date, shopId, pgsRequest, terminalId, mac, REFUND, reqRefNum);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private Document buildStepOneRequest(MethodCompletedEnum methodCompletedEnum, String correlationId) {
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        Date date = new Date();
        Element macElement = new Element(MAC.getTagName());
        documentBuilder.addElement(VposRequestEnum.RELEASE, RELEASE_VALUE);
        //REQUEST
        documentBuilder.addBodyElement(REQUEST);
        documentBuilder.addElement(REQUEST, OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_1);
        documentBuilder.addElement(REQUEST, TIMESTAMP, date);
        documentBuilder.addElement(REQUEST, macElement);
        //DATA
        documentBuilder.addBodyElement(DATA);
        documentBuilder.addBodyElement(DATA, AUTH_REQUEST_3DS2_STEP_1);
        documentBuilder.addBodyElement(AUTH_REQUEST_3DS2_STEP_1, HEADER);
        //HEADER
        documentBuilder.addElement(HEADER, SHOP_ID, shopId);
        documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
        //STEP1
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_1, THREEDS_TRANS_ID, correlationId);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_1, THREEDS_METHOD_COMPLETED, methodCompletedEnum.name());
        //MAC
        VPosMacBuilder macBuilder = calculateMacStep1(date, shopId, terminalId, mac, correlationId, methodCompletedEnum, reqRefNum);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private Document buildStepTwoRequest(String correlationId) {
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        Date date = new Date();
        Element macElement = new Element(MAC.getTagName());
        documentBuilder.addElement(VposRequestEnum.RELEASE, RELEASE_VALUE);
        //REQUEST
        documentBuilder.addBodyElement(REQUEST);
        documentBuilder.addElement(REQUEST, OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_2);
        documentBuilder.addElement(REQUEST, TIMESTAMP, date);
        documentBuilder.addElement(REQUEST, macElement);
        //DATA
        documentBuilder.addBodyElement(DATA);
        documentBuilder.addBodyElement(DATA, AUTH_REQUEST_3DS2_STEP_2);
        documentBuilder.addBodyElement(AUTH_REQUEST_3DS2_STEP_2, HEADER);
        //HEADER
        documentBuilder.addElement(HEADER, SHOP_ID, shopId);
        documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
        //STEP2
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_2, THREEDS_TRANS_ID, correlationId);
        //MAC
        VPosMacBuilder macBuilder = calculateMacStep2(date, shopId, terminalId, mac, correlationId, reqRefNum);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private VPosMacBuilder calculateMacStep0(Date date, String shopId, StepZeroRequest pgsRequest, String terminalId, String mac, String notifyUrl, String reqRefNum) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_0);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
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
        macBuilder.addElement(NOTIF_URL, notifyUrl + STEP_2);
        macBuilder.addElement(THREEDS_MTD_NOTIF_URL, notifyUrl + STEP_1);
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMac(Date date, String shopId, StepZeroRequest pgsRequest, String terminalId, String mac, VposRequestEnum operation, String reqRefNum) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, operation);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(AMOUNT, pgsRequest.getAmount());
        macBuilder.addElement(EMAIL_CH, pgsRequest.getEmailCH());
        macBuilder.addElement(CURRENCY, CURRENCY_VALUE);
        macBuilder.addElement(OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMacStep1(Date date, String shopId, String terminalId, String mac, String correlationId, MethodCompletedEnum methodCompletedEnum, String reqRefNum) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_1);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(THREEDS_TRANS_ID, correlationId);
        macBuilder.addElement(THREEDS_METHOD_COMPLETED, methodCompletedEnum.name());
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMacStep2(Date date, String shopId, String terminalId, String mac, String correlationId, String reqRefNum) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, OPERATION_AUTH_REQUEST_3DS2_STEP_2);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(THREEDS_TRANS_ID, correlationId);
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
