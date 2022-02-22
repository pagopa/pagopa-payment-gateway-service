//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.21 alle 05:24:24 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per otpContestoVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="otpContestoVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}p2BAbstractPresentationBean"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="deviceIdData" type="{http://p2b.gft.it/srv/pp}deviceIdDataVO" minOccurs="0"/&gt;
 *         &lt;element name="otpData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signatureData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="signatureDataHash" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="transactionId" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otpContestoVO", propOrder = {
    "deviceIdData",
    "otpData",
    "signatureData",
    "signatureDataHash",
    "transactionId"
})
public class OtpContestoVO
    extends P2BAbstractPresentationBean
{

    protected DeviceIdDataVO deviceIdData;
    protected String otpData;
    protected String signatureData;
    protected String signatureDataHash;
    protected long transactionId;

    /**
     * Recupera il valore della proprietà deviceIdData.
     * 
     * @return
     *     possible object is
     *     {@link DeviceIdDataVO }
     *     
     */
    public DeviceIdDataVO getDeviceIdData() {
        return deviceIdData;
    }

    /**
     * Imposta il valore della proprietà deviceIdData.
     * 
     * @param value
     *     allowed object is
     *     {@link DeviceIdDataVO }
     *     
     */
    public void setDeviceIdData(DeviceIdDataVO value) {
        this.deviceIdData = value;
    }

    /**
     * Recupera il valore della proprietà otpData.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtpData() {
        return otpData;
    }

    /**
     * Imposta il valore della proprietà otpData.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtpData(String value) {
        this.otpData = value;
    }

    /**
     * Recupera il valore della proprietà signatureData.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignatureData() {
        return signatureData;
    }

    /**
     * Imposta il valore della proprietà signatureData.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignatureData(String value) {
        this.signatureData = value;
    }

    /**
     * Recupera il valore della proprietà signatureDataHash.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSignatureDataHash() {
        return signatureDataHash;
    }

    /**
     * Imposta il valore della proprietà signatureDataHash.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSignatureDataHash(String value) {
        this.signatureDataHash = value;
    }

    /**
     * Recupera il valore della proprietà transactionId.
     * 
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Imposta il valore della proprietà transactionId.
     * 
     */
    public void setTransactionId(long value) {
        this.transactionId = value;
    }

}
