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
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pm.gateway.constant.VposConstant.DEFAULT_CHARSET;
import static it.pagopa.pm.gateway.constant.VposConstant.WRONG_MAC_MSG;
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
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(DISALLOW_DOCTYPE_DECL, true);
            saxBuilder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            saxBuilder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            Document responseDocument = saxBuilder.build(new ByteArrayInputStream(clientResponse));
            Element root = responseDocument.getRootElement();
            Element data = root.getChild(DATA_RESPONSE.getTagName());
            if (data == null) {
                throw new IOException("Data cannot be null");
            } else if (data.getChild(THREEDS_METHOD.getTagName()) != null) {
                Element method = data.getChild(THREEDS_METHOD.getTagName());
                return createThreeDS2Method(method, root);
            } else if (data.getChild(THREEDS_CHALLENGE.getTagName()) != null) {
                Element challenge = data.getChild(THREEDS_CHALLENGE.getTagName());
                return createThreeDS2Challenge(challenge, root);
            } else if (data.getChild(AUTHORIZATION.getTagName()) != null) {
                Element authorization = data.getChild(AUTHORIZATION.getTagName());
                return createThreeDS2Authorization(authorization, root);
            } else {
                return create3DS2ResponseWithHeaders(root);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

    private ThreeDS2Response create3DS2ResponseWithHeaders(Element root) {
        ThreeDS2Response threeDS2Response = new ThreeDS2Response();
        threeDS2Response.setTimestamp(getVposResponseField(root, TIMESTAMP_RESPONSE));
        threeDS2Response.setResultCode(getVposResponseField(root, RESULT));
        threeDS2Response.setResultMac(getVposResponseField(root, MAC_RESPONSE));
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Method(Element methodElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.METHOD);
        ThreeDS2Method method = new ThreeDS2Method();
        method.setThreeDSTransId(getVposResponseField(methodElem, THREEDS_TRANS_ID));
        method.setThreeDSMethodData(getVposResponseField(methodElem, THREEDS_METHOD_DATA));
        method.setThreeDSMethodUrl(getVposResponseField(methodElem, THREEDS_METHOD_URL));
        method.setMac(getVposResponseField(methodElem, MAC_RESPONSE));
        threeDS2Response.setThreeDS2ResponseElement(method);
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Challenge(Element challengeElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.CHALLENGE);
        ThreeDS2Challenge challenge = new ThreeDS2Challenge();
        challenge.setThreeDSTransId(getVposResponseField(challengeElem, THREEDS_TRANS_ID));
        challenge.setAcsUrl(getVposResponseField(challengeElem, ACS_URL));
        challenge.setCReq(getVposResponseField(challengeElem, C_REQ));
        challenge.setMac(getVposResponseField(challengeElem, MAC_RESPONSE));
        threeDS2Response.setThreeDS2ResponseElement(challenge);
        return threeDS2Response;
    }

    private ThreeDS2Response createThreeDS2Authorization(Element authorizationElem, Element root) {
        ThreeDS2Response threeDS2Response = create3DS2ResponseWithHeaders(root);
        threeDS2Response.setResponseType(ThreeDS2ResponseTypeEnum.AUTHORIZATION);

        ThreeDS2Authorization authorization = new ThreeDS2Authorization();
        authorization.setPaymentType(getVposResponseField(authorizationElem, PAYMENT_TYPE));
        authorization.setAuthorizationType(getVposResponseField(authorizationElem, AUTHORIZATION_TYPE));
        authorization.setTransactionId(getVposResponseField(authorizationElem, TRANSACTION_ID));

        CardCircuit network = CardCircuit.fromCode(getVposResponseField(authorizationElem, NETWORK_RESPONSE));
        authorization.setNetwork(ObjectUtils.isEmpty(network) ? CardCircuit.UNKNOWN : network);
        authorization.setOrderId(getVposResponseField(authorizationElem, ORDER_ID_RESPONSE));
        authorization.setTransactionAmount(Long.parseLong(getVposResponseField(authorizationElem, TRANSACTION_AMOUNT)));
        authorization.setCurrency(getVposResponseField(authorizationElem, CURRENCY_RESPONSE));
        authorization.setExponent(getVposResponseField(authorizationElem, EXPONENT));

        String authorizationAmount = getVposResponseField(authorizationElem, AUTHORIZED_AMOUNT);
        if (StringUtils.isNumeric(authorizationAmount)) {
            authorization.setAuthorizedAmount(Long.parseLong(authorizationAmount));
        }

        String refundAmount = getVposResponseField(authorizationElem, REFUNDED_AMOUNT);
        if (StringUtils.isNumeric(refundAmount)) {
            authorization.setRefundedAmount(Long.parseLong(refundAmount));
        }

        String accountAmount = getVposResponseField(authorizationElem, ACCOUNTED_AMOUNT);
        if (StringUtils.isNumeric(accountAmount)) {
            authorization.setAccountedAmount(Long.parseLong(accountAmount));
        }

        authorization.setTransactionResult(getVposResponseField(authorizationElem, TRANSACTION_RESULT));
        authorization.setTimestamp(getVposResponseField(authorizationElem, TIMESTAMP_RESPONSE));
        authorization.setAuthorizationNumber(getVposResponseField(authorizationElem, AUTHORIZATION_NUMBER));
        authorization.setAcquirerBin(getVposResponseField(authorizationElem, ACQ_BIN));
        authorization.setMerchantId(getVposResponseField(authorizationElem, MERCHANT_ID));
        authorization.setTransactionStatus(getVposResponseField(authorizationElem, TRANSACTION_STATUS));
        authorization.setResponseCodeIso(getVposResponseField(authorizationElem, RESPONSE_CODE_ISO));
        authorization.setRrn(getVposResponseField(authorizationElem, RRN));

        threeDS2Response.setThreeDS2ResponseElement(authorization);
        return threeDS2Response;
    }

    private String getVposResponseField(Element element, VPosResponseEnum authResponseEnum) {
        return element.getChildText(authResponseEnum.getTagName());
    }

    public void validateResponseMac(String timestamp, String resultCode, String resultMac, StepZeroRequest pgsRequest) {
        Shop vposShopData = vPosUtils.getVposShopByIdPsp(pgsRequest.getIdPsp());
        String configMac = BooleanUtils.isTrue(pgsRequest.getIsFirstPayment()) ?
                vposShopData.getMacFirstPayment() :
                vposShopData.getMacSuccPayment();
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
        return !StringUtils.equalsIgnoreCase(responseMac, mac);
    }

    public AuthResponse buildAuthResponse(byte[] clientResponse) throws IOException {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(DISALLOW_DOCTYPE_DECL, true);
            saxBuilder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            saxBuilder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            Document responseDocument = saxBuilder.build(new ByteArrayInputStream(clientResponse));
            Element root = responseDocument.getRootElement();
            AuthResponse authResponse = new AuthResponse();
            authResponse.setTimestamp(getVposResponseField(root, TIMESTAMP_RESPONSE));
            authResponse.setResultCode(getVposResponseField(root, RESULT));
            authResponse.setResultMac(getVposResponseField(root, MAC_RESPONSE));
            Element data = root.getChild(DATA_RESPONSE.getTagName());
            if (data != null) {
                Element operation = data.getChild(OPERATION.getTagName());
                if (operation != null) {
                    Element authorization = operation.getChild(AUTHORIZATION.getTagName());
                    checkAuthorization(authResponse, authorization);
                }
            }
            return authResponse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

    private void checkAuthorization(AuthResponse authResponse, Element authorization) {
        if (authorization != null) {
            authResponse.setPaymentType(getVposResponseField(authorization, PAYMENT_TYPE));
            authResponse.setAuthorizationType(getVposResponseField(authorization, AUTHORIZATION_TYPE));
            authResponse.setAcquirerTransactionId(getVposResponseField(authorization, TRANSACTION_ID));
            CardCircuit network = CardCircuit.fromCode(getVposResponseField(authorization, NETWORK_RESPONSE));
            authResponse.setCircuit(ObjectUtils.isEmpty(network) ? CardCircuit.UNKNOWN : network);
            authResponse.setOrderNumber(getVposResponseField(authorization, ORDER_ID_RESPONSE));
            authResponse.setAmount(Long.parseLong(getVposResponseField(authorization, TRANSACTION_AMOUNT)));
            String authorizationAmount = getVposResponseField(authorization, AUTHORIZED_AMOUNT);
            if (StringUtils.isNumeric(authorizationAmount)) {
                authResponse.setAuthorizationAmount(Long.parseLong(authorizationAmount));
            }
            String refundAmount = getVposResponseField(authorization, REFUNDED_AMOUNT);
            if (StringUtils.isNumeric(refundAmount)) {
                authResponse.setRefundAmount(Long.parseLong(refundAmount));
            }
            String accountAmount = getVposResponseField(authorization, ACCOUNTED_AMOUNT);
            if (StringUtils.isNumeric(accountAmount)) {
                authResponse.setAccountAmount(Long.parseLong(accountAmount));
            }
            authResponse.setCurrency(getVposResponseField(authorization, CURRENCY_RESPONSE));
            authResponse.setAuthorizationNumber(getVposResponseField(authorization, AUTHORIZATION_NUMBER));
            authResponse.setAcquirerBin(getVposResponseField(authorization, ACQUIRER_BIN));
            authResponse.setMerchantCode(getVposResponseField(authorization, MERCHANT_ID));
            authResponse.setStatus(getVposResponseField(authorization, TRANSACTION_STATUS));
            authResponse.setRrn(getVposResponseField(authorization, RRN));
            authResponse.setAuthorizationMac(getVposResponseField(authorization, MAC_RESPONSE));
        }
    }

    public VposOrderStatusResponse buildOrderStatusResponse(byte[] clientResponse) throws IOException {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            saxBuilder.setFeature(DISALLOW_DOCTYPE_DECL, true);
            saxBuilder.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            saxBuilder.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            Document responseDocument = saxBuilder.build(new ByteArrayInputStream(clientResponse));
            Element root = responseDocument.getRootElement();
            VposOrderStatusResponse response = new VposOrderStatusResponse();
            response.setTimestamp(getVposResponseField(root, TIMESTAMP_RESPONSE));
            response.setResultCode(getVposResponseField(root, RESULT));
            response.setResultMac(getVposResponseField(root, MAC_RESPONSE));
            Element data = root.getChild(DATA_RESPONSE.getTagName());
            if (data != null) {
                Element orderStatus = data.getChild(ORDER_STATUS.getTagName());
                if (orderStatus != null) {
                    VposOrderStatus vposOrderStatus = new VposOrderStatus();
                    Element header = root.getChild(HEADER.getTagName());
                    if (header != null) {
                        Header headerDto = new Header();
                        headerDto.setOperatorId(getVposResponseField(header, OPERATOR_ID));
                        headerDto.setShopId(getVposResponseField(header, SHOP_ID));
                        headerDto.setReqRefNum(getVposResponseField(header, REQ_REF_NUM));
                        vposOrderStatus.setHeader(headerDto);
                        response.setOrderStatus(vposOrderStatus);
                    }
                    vposOrderStatus.setOrderId(getVposResponseField(orderStatus, ORDER_ID));
                    response.setOrderStatus(vposOrderStatus);
                }
                response.setProductRef(getVposResponseField(data, PRODUCT_REF ));
                response.setNumberOfItems(getVposResponseField(data, NUMBER_OF_ITEMS));
                List<Element> authorizations = data.getChildren(AUTHORIZATION.getTagName());
                List<ThreeDS2Authorization> authorizationsDto = new ArrayList<>();
                for (Element authorization : authorizations){
                    checkOrderStatusAuthorization(authorizationsDto, authorization);
                }
                response.setAuthorizations(authorizationsDto);
            }
            return response;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

    private void checkOrderStatusAuthorization(List<ThreeDS2Authorization> authorizationsDto, Element authorization) {
        if (authorization != null) {
            ThreeDS2Authorization authorizationDto = new ThreeDS2Authorization();

            authorizationDto.setPaymentType(getVposResponseField(authorization, PAYMENT_TYPE));
            authorizationDto.setAuthorizationType(getVposResponseField(authorization, AUTHORIZATION_TYPE));
            authorizationDto.setAuthorizationNumber(getVposResponseField(authorization, AUTHORIZATION_NUMBER));
            String authorizationAmount = getVposResponseField(authorization, AUTHORIZED_AMOUNT);
            if (StringUtils.isNumeric(authorizationAmount)) {
                authorizationDto.setAuthorizedAmount(Long.parseLong(authorizationAmount));
            }
            authorizationDto.setAcquirerBin(getVposResponseField(authorization, ACQUIRER_BIN));
            authorizationDto.setExponent(getVposResponseField(authorization, EXPONENT));
            authorizationDto.setCurrency(getVposResponseField(authorization, CURRENCY));
            authorizationDto.setMerchantId(getVposResponseField(authorization, MERCHANT_ID));
            String accountedAmount = getVposResponseField(authorization, ACCOUNTED_AMOUNT);
            if (StringUtils.isNumeric(accountedAmount)) {
                authorizationDto.setAccountedAmount(Long.parseLong(accountedAmount));
            }
            authorizationDto.setNetwork(CardCircuit.fromCode(getVposResponseField(authorization, NETWORK)));

            authorizationDto.setOrderId(getVposResponseField(authorization, ORDER_ID));
            authorizationDto.setResponseCodeIso(getVposResponseField(authorization, RESPONSE_CODE_ISO));
            String refundedAmount = getVposResponseField(authorization, REFUNDED_AMOUNT);
            if (StringUtils.isNumeric(refundedAmount)) {
                authorizationDto.setRefundedAmount(Long.parseLong(refundedAmount));
            }

            authorizationDto.setRrn(getVposResponseField(authorization, RRN));
            authorizationDto.setTimestamp(getVposResponseField(authorization, TIMESTAMP));
            String transactionAmount = getVposResponseField(authorization, TRANSACTION_AMOUNT);
            if (StringUtils.isNumeric(transactionAmount)) {
                authorizationDto.setTransactionAmount(Long.parseLong(transactionAmount));
            }
            authorizationDto.setTransactionId(getVposResponseField(authorization, TRANSACTION_ID));
            authorizationDto.setTransactionResult(getVposResponseField(authorization, TRANSACTION_RESULT));
            authorizationDto.setTransactionStatus(getVposResponseField(authorization, TRANSACTION_STATUS));
            authorizationsDto.add(authorizationDto);
        }
    }
}
