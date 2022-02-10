//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.09 alle 04:22:41 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per utenteAttivoVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="utenteAttivoVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}utenteVO"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="deviceUniqueID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="tokenNotifiche" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "utenteAttivoVO", propOrder = {
    "deviceUniqueID",
    "tokenNotifiche"
})
public class UtenteAttivoVO
    extends UtenteVO
{

    protected String deviceUniqueID;
    protected String tokenNotifiche;

    /**
     * Recupera il valore della proprietà deviceUniqueID.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeviceUniqueID() {
        return deviceUniqueID;
    }

    /**
     * Imposta il valore della proprietà deviceUniqueID.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeviceUniqueID(String value) {
        this.deviceUniqueID = value;
    }

    /**
     * Recupera il valore della proprietà tokenNotifiche.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTokenNotifiche() {
        return tokenNotifiche;
    }

    /**
     * Imposta il valore della proprietà tokenNotifiche.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTokenNotifiche(String value) {
        this.tokenNotifiche = value;
    }

}
