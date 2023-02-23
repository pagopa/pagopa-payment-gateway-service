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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final String CHALLENGE = "/challenge";
    private String vposResumeUrl;
    private String methodNotifyUrl;
    private VPosUtils vPosUtils;
    private String shopId;
    private String terminalId;
    private String mac;

    @Autowired
    public VPosRequestUtils(@Value("${vpos.resume.url}") String vposResumeUrl,
                            @Value("${vpos.method.notifyUrl}") String methodNotifyUrl,
                            VPosUtils vPosUtils) {
        this.vposResumeUrl = vposResumeUrl;
        this.methodNotifyUrl = methodNotifyUrl;
        this.vPosUtils = vPosUtils;
    }

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

    public Map<String, String> buildStepZeroRequestParams(StepZeroRequest pgsRequest, String requestId) throws Exception {
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

    public Map<String, String> buildOrderStatusParams(StepZeroRequest pgsRequest) throws IOException {
        retrieveShopInformation(pgsRequest);
        Document revertRequest = buildOrderStatusRequest(shopId, terminalId, mac, pgsRequest);
        return getParams(revertRequest);
    }

    private Document buildStepZeroRequest(StepZeroRequest pgsRequest, String shopId, String terminalId, String mac, String requestId) throws Exception {
        String notifyUrl = String.format(vposResumeUrl, requestId);
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
        String expDate = pgsRequest.getExpireDate().substring(2);
        pgsRequest.setExpireDate(expDate);
        String threeDsData = encode3DSdata(mac, pgsRequest.getThreeDsData());
        pgsRequest.setThreeDsData(threeDsData);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, ORDER_ID, pgsRequest.getIdTransaction());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, PAN, pgsRequest.getPan());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, CVV2, pgsRequest.getSecurityCode());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, EXP_DATE, pgsRequest.getExpireDate());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, AMOUNT, pgsRequest.getAmount());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, CURRENCY, CURRENCY_VALUE);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, ACCOUNTING_MODE, ACCOUNT_DEFERRED);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NETWORK, pgsRequest.getCircuit().getCode());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, EMAIL_CH, pgsRequest.getEmailCH());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NAME_CH, pgsRequest.getHolder());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, USER_ID, pgsRequest.getHolder());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, THREEDS_DATA, pgsRequest.getThreeDsData());
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, NOTIF_URL, notifyUrl + CHALLENGE);
        documentBuilder.addElement(AUTH_REQUEST_3DS2_STEP_0, THREEDS_MTD_NOTIF_URL, String.format(methodNotifyUrl, requestId));
        //MAC
        VPosMacBuilder macBuilder = calculateMacStep0(date, shopId, pgsRequest, terminalId, mac, notifyUrl, reqRefNum, requestId);
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
        VPosMacBuilder macBuilder = calculateMacAccounting(date, shopId, pgsRequest, terminalId, mac, reqRefNum, correlationId);
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
        //REFUND
        documentBuilder.addElement(REFUND, TRANSACTION_ID, correlationId);
        documentBuilder.addElement(REFUND, ORDER_ID, pgsRequest.getIdTransaction());
        documentBuilder.addElement(REFUND, AMOUNT, pgsRequest.getAmount());
        documentBuilder.addElement(REFUND, CURRENCY, CURRENCY_VALUE);
        documentBuilder.addElement(REFUND, OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        //MAC
        VPosMacBuilder macBuilder = calculateMacRevert(date, shopId, pgsRequest, terminalId, mac, reqRefNum, correlationId);
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

    private Document buildOrderStatusRequest(String shopId, String terminalId, String mac, StepZeroRequest pgsRequest) {
        String reqRefNum = vPosUtils.getReqRefNum();
        VPosDocumentBuilder documentBuilder = new VPosDocumentBuilder(Locale.ENGLISH);
        Date date = new Date();
        Element macElement = new Element(MAC.getTagName());
        documentBuilder.addElement(VposRequestEnum.RELEASE, RELEASE_VALUE);
        //REQUEST
        documentBuilder.addBodyElement(REQUEST);
        documentBuilder.addElement(REQUEST, OPERATION, OPERATION_ORDERSTATUS);
        documentBuilder.addElement(REQUEST, TIMESTAMP, date);
        documentBuilder.addElement(REQUEST, macElement);
        //DATA
        documentBuilder.addBodyElement(DATA);
        documentBuilder.addBodyElement(DATA, ORDERSTATUS);
        documentBuilder.addBodyElement(ORDERSTATUS, HEADER);
        //HEADER
        documentBuilder.addElement(HEADER, SHOP_ID, shopId);
        documentBuilder.addElement(HEADER, OPERATOR_ID, terminalId);
        documentBuilder.addElement(HEADER, REQ_REF_NUM, reqRefNum);
        //STEP2
        documentBuilder.addElement(ORDERSTATUS, ORDER_ID, pgsRequest.getIdTransaction());
        //MAC
        VPosMacBuilder macBuilder = calculateMacOrderStatus(date, shopId, terminalId, mac, reqRefNum, pgsRequest);
        macElement.setText(macBuilder.toSha1Hex(DEFAULT_CHARSET));
        return documentBuilder.build();
    }

    private VPosMacBuilder calculateMacStep0(Date date, String shopId, StepZeroRequest pgsRequest, String terminalId, String mac, String notifyUrl, String reqRefNum, String requestId) {
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
        macBuilder.addElement(CURRENCY, CURRENCY_VALUE);
        macBuilder.addElement(ACCOUNTING_MODE, ACCOUNT_DEFERRED);
        macBuilder.addElement(NETWORK, pgsRequest.getCircuit().getCode());
        macBuilder.addElement(EMAIL_CH, pgsRequest.getEmailCH());
        macBuilder.addElement(USER_ID, pgsRequest.getHolder());
        macBuilder.addElement(OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        macBuilder.addElement(THREEDS_DATA, pgsRequest.getThreeDsData());
        macBuilder.addElement(NAME_CH, pgsRequest.getHolder());
        macBuilder.addElement(NOTIF_URL, notifyUrl + CHALLENGE);
        macBuilder.addElement(THREEDS_MTD_NOTIF_URL, String.format(methodNotifyUrl, requestId));
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMacRevert(Date date, String shopId, StepZeroRequest pgsRequest, String terminalId, String mac, String reqRefNum, String correlationId) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, REFUND);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(TRANSACTION_ID, correlationId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(AMOUNT, pgsRequest.getAmount());
        macBuilder.addElement(CURRENCY, CURRENCY_VALUE);
        macBuilder.addElement(OPERATION_DESCRIPTION, FAKE_DESCRIPTION);
        macBuilder.addString(mac);
        return macBuilder;
    }

    private VPosMacBuilder calculateMacAccounting(Date date, String shopId, StepZeroRequest pgsRequest, String terminalId, String mac, String reqRefNum, String correlationId) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, ACCOUNTING);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(TRANSACTION_ID, correlationId);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
        macBuilder.addElement(AMOUNT, pgsRequest.getAmount());
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

    private VPosMacBuilder calculateMacOrderStatus(Date date, String shopId, String terminalId, String mac, String reqRefNum, StepZeroRequest pgsRequest) {
        VPosMacBuilder macBuilder = new VPosMacBuilder();
        macBuilder.addElement(OPERATION, OPERATION_ORDERSTATUS);
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP.getFormat());
        macBuilder.addElement(TIMESTAMP, dateFormat.format(date));
        macBuilder.addElement(SHOP_ID, shopId);
        macBuilder.addElement(OPERATOR_ID, terminalId);
        macBuilder.addElement(REQ_REF_NUM, reqRefNum);
        macBuilder.addElement(ORDER_ID, pgsRequest.getIdTransaction());
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

    @SuppressWarnings({"squid:S112", "squid:S3329"})
    public static String encode3DSdata(String apiSecretMerchant, String jsonObject) throws Exception {
        byte[] initVector = new byte[16];
        byte[] key = apiSecretMerchant.substring(0, 16).getBytes();
        IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        byte[] toEncrypt = jsonObject.getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encrypted = cipher.doFinal(toEncrypt);
        return DatatypeConverter.printBase64Binary(encrypted);
    }

}
