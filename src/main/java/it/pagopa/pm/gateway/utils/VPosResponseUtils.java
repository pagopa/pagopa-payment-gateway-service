package it.pagopa.pm.gateway.utils;

import it.pagopa.pm.gateway.dto.creditcard.StepZeroRequest;
import it.pagopa.pm.gateway.dto.enums.CardCircuit;
import it.pagopa.pm.gateway.dto.enums.ThreeDS2ResponseTypeEnum;
import it.pagopa.pm.gateway.dto.enums.VPosResponseEnum;
import it.pagopa.pm.gateway.dto.vpos.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static it.pagopa.pm.gateway.constant.VposConstant.*;
import static it.pagopa.pm.gateway.dto.enums.VPosResponseEnum.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class VPosResponseUtils {

    private static final String NULL_STRING = "null";
    private static final String APACHE_FEATURES_BASE_URL = "http://apache.org/xml/features/";
    private static final String XML_FEATURES_BASE_URL = "http://xml.org/sax/features/";
    private static final String DISALLOW_DOCTYPE_DECL = APACHE_FEATURES_BASE_URL + "disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = XML_FEATURES_BASE_URL + "external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = XML_FEATURES_BASE_URL + "external-parameter-entities";

    @Autowired
    VPosUtils vPosUtils;

    public ThreeDS2Response build3ds2Response(byte[] clientResponse) throws IOException {
        ThreeDS2Response threeDS2Response;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(DISALLOW_DOCTYPE_DECL, true);
            saxBuilder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            saxBuilder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            Document responseDocument = saxBuilder.build(new ByteArrayInputStream(clientResponse));
            Element root = responseDocument.getRootElement();
            threeDS2Response = create3DS2ResponseWithHeaders(root);
            Element data = root.getChild(DATA_RESPONSE.getTagName());
            if (data == null) {
                throw new IOException("Data cannot be null");
            } else if (data.getChild(THREEDS_METHOD.getTagName()) != null) {
                Element method = data.getChild(THREEDS_METHOD.getTagName());
                threeDS2Response = createThreeDS2Method(method, root);
            } else if (data.getChild(THREEDS_CHALLENGE.getTagName()) != null) {
                Element challenge = data.getChild(THREEDS_CHALLENGE.getTagName());
                threeDS2Response = createThreeDS2Challenge(challenge, root);
            } else if (data.getChild(AUTHORIZATION.getTagName()) != null) {
                Element authorization = data.getChild(AUTHORIZATION.getTagName());
                threeDS2Response = createThreeDS2Authorization(authorization, root);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }
        return threeDS2Response;
    }

    private ThreeDS2Response create3DS2ResponseWithHeaders(Element root) {
        ThreeDS2Response threeDS2Response = new ThreeDS2Response();
        threeDS2Response.setTimestamp(getValue(root, TIMESTAMP_RESPONSE));
        threeDS2Response.setResultCode(getValue(root, RESULT));
        threeDS2Response.setResultMac(getValue(root, MAC_RESPONSE));
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Method(Element methodElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.METHOD);
        ThreeDS2Method method = new ThreeDS2Method();
        method.setThreeDSTransId(getValue(methodElem, THREEDS_TRANS_ID));
        method.setThreeDSMethodData(getValue(methodElem, THREEDS_METHOD_DATA));
        method.setThreeDSMethodUrl(getValue(methodElem, THREEDS_METHOD_URL));
        method.setMac(getValue(methodElem, MAC_RESPONSE));
        threeDS2Response.setThreeDS2ResponseElement(method);
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Challenge(Element challengeElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE);
        ThreeDS2Challenge challenge = new ThreeDS2Challenge();
        challenge.setThreeDSTransId(getValue(challengeElem, THREEDS_TRANS_ID));
        challenge.setAcsUrl(getValue(challengeElem, ACS_URL));
        challenge.setCReq(getValue(challengeElem, C_REQ));
        challenge.setMac(getValue(challengeElem, MAC_RESPONSE));
        threeDS2Response.setThreeDS2ResponseElement(challenge);
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Authorization(Element authorizationElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.AUTHORIZATION);
        ThreeDS2Authorization authorization = new ThreeDS2Authorization();
        authorization.setPaymentType(getValue(authorizationElem, PAYMENT_TYPE));
        authorization.setAuthorizationType(getValue(authorizationElem, AUTHORIZATION_TYPE));
        authorization.setTransactionId(getValue(authorizationElem, TRANSACTION_ID));
        CardCircuit network = CardCircuit.fromCode(getValue(authorizationElem, NETWORK_RESPONSE));
        authorization.setNetwork(ObjectUtils.isEmpty(network) ? CardCircuit.UNKNOWN : network);
        authorization.setOrderId(getValue(authorizationElem, ORDER_ID_RESPONSE));
        authorization.setTransactionAmount(Long.parseLong(getValue(authorizationElem, TRANSACTION_AMOUNT)));
        authorization.setCurrency(getValue(authorizationElem, CURRENCY_RESPONSE));
        authorization.setExponent(getValue(authorizationElem, EXPONENT));
        String authorizationAmount = getValue(authorizationElem, AUTHORIZED_AMOUNT);
        if (StringUtils.isNumeric(authorizationAmount)) {
            authorization.setAuthorizedAmount(Long.parseLong(authorizationAmount));
        }
        String refundAmount = getValue(authorizationElem, REFUNDED_AMOUNT);
        if (StringUtils.isNumeric(refundAmount)) {
            authorization.setRefundedAmount(Long.parseLong(refundAmount));
        }
        String accountAmount = getValue(authorizationElem, ACCOUNTED_AMOUNT);
        if (StringUtils.isNumeric(accountAmount)) {
            authorization.setAccountedAmount(Long.parseLong(accountAmount));
        }
        authorization.setTransactionResult(getValue(authorizationElem, TRANSACTION_RESULT));
        authorization.setTimestamp(getValue(authorizationElem, TIMESTAMP_RESPONSE));
        authorization.setAuthorizationNumber(getValue(authorizationElem, AUTHORIZATION_NUMBER));
        authorization.setAcquirerBin(getValue(authorizationElem, ACQ_BIN));
        authorization.setMerchantId(getValue(authorizationElem, MERCHANT_ID));
        authorization.setTransactionStatus(getValue(authorizationElem, TRANSACTION_STATUS));
        authorization.setResponseCodeIso(getValue(authorizationElem, RESPONSE_CODE_ISO));
        authorization.setRrn(getValue(authorizationElem, RRN));
        threeDS2Response.setThreeDS2ResponseElement(authorization);
        return threeDS2Response;
    }

    private String getValue(Element element, VPosResponseEnum authResponseEnum) {
        return element.getChildText(authResponseEnum.getTagName());
    }

    public void validateResponseMac(String timestamp, String resultCode, String resultMac, StepZeroRequest pgsRequest) {
        List<String> vposShopData = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
        String configMac = BooleanUtils.isTrue(pgsRequest.getIsFirstPayment()) ?
                vposShopData.get(MAC_FIRST_PAY_POSITION) :
                vposShopData.get(MAC_NEXT_PAY_POSITION);
        String mac = calculateMac(timestamp, resultCode, configMac);
        if (isMacDifferent(mac, resultMac)) {
            String macWithoutShopKey = calculateMac(timestamp, resultCode, NULL_STRING);
            if (isMacDifferent(macWithoutShopKey, resultMac)) {
                log.warn(String.format(WRONG_MAC_MSG, macWithoutShopKey, resultMac));
            }
        }
    }

    private String calculateMac(String timestamp, String resultCode, String mac) {
        VPosMacBuilder vposMacBuilder = new VPosMacBuilder();
        vposMacBuilder.addString(timestamp);
        vposMacBuilder.addString(resultCode);
        vposMacBuilder.addString(mac);
        return vposMacBuilder.toSha1Hex(DEFAULT_CHARSET);
    }

    private boolean isMacDifferent(String mac, String responseMac) {
        return StringUtils.isNotBlank(responseMac) || !StringUtils.equalsIgnoreCase(responseMac, mac);
    }

    public AuthResponse buildAuthResponse(byte[] clientResponse) throws IOException {
        AuthResponse authResponse;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(DISALLOW_DOCTYPE_DECL, true);
            saxBuilder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            saxBuilder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            Document responseDocument = saxBuilder.build(new ByteArrayInputStream(clientResponse));
            Element root = responseDocument.getRootElement();
            authResponse = new AuthResponse();
            authResponse.setTimestamp(getValue(root, TIMESTAMP_RESPONSE));
            authResponse.setResultCode(getValue(root, RESULT));
            authResponse.setResultMac(getValue(root, MAC_RESPONSE));
            Element data = root.getChild(DATA_RESPONSE.getTagName());
            if (data != null) {
                Element operation = data.getChild(OPERATION.getTagName());
                if (operation != null) {
                    Element authorization = operation.getChild(AUTHORIZATION.getTagName());
                    checkAuthorization(authResponse, authorization);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }
        return authResponse;
    }

    private void checkAuthorization(AuthResponse authResponse, Element authorization) {
        if (authorization != null) {
            authResponse.setPaymentType(getValue(authorization, PAYMENT_TYPE));
            authResponse.setAuthorizationType(getValue(authorization, AUTHORIZATION_TYPE));
            authResponse.setAcquirerTransactionId(getValue(authorization, TRANSACTION_ID));
            CardCircuit network = CardCircuit.fromCode(getValue(authorization, NETWORK_RESPONSE));
            authResponse.setCircuit(ObjectUtils.isEmpty(network) ? CardCircuit.UNKNOWN : network);
            authResponse.setOrderNumber(getValue(authorization, ORDER_ID_RESPONSE));
            authResponse.setAmount(Long.parseLong(getValue(authorization, TRANSACTION_AMOUNT)));
            String authorizationAmount = getValue(authorization, AUTHORIZED_AMOUNT);
            if (StringUtils.isNumeric(authorizationAmount)) {
                authResponse.setAuthorizationAmount(Long.parseLong(authorizationAmount));
            }
            String refundAmount = getValue(authorization, REFUNDED_AMOUNT);
            if (StringUtils.isNumeric(refundAmount)) {
                authResponse.setRefundAmount(Long.parseLong(refundAmount));
            }
            String accountAmount = getValue(authorization, ACCOUNTED_AMOUNT);
            if (StringUtils.isNumeric(accountAmount)) {
                authResponse.setAccountAmount(Long.parseLong(accountAmount));
            }
            authResponse.setCurrency(getValue(authorization, CURRENCY_RESPONSE));
            authResponse.setAuthorizationNumber(getValue(authorization, AUTHORIZATION_NUMBER));
            authResponse.setAcquirerBin(getValue(authorization, ACQUIRER_BIN));
            authResponse.setMerchantCode(getValue(authorization, MERCHANT_ID));
            authResponse.setStatus(getValue(authorization, TRANSACTION_STATUS));
            authResponse.setRrn(getValue(authorization, RRN));
            authResponse.setAuthorizationMac(getValue(authorization, MAC_RESPONSE));
        }
    }
}
