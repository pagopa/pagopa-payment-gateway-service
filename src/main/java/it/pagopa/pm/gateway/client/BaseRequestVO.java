//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.09 alle 04:22:41 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per baseRequestVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="baseRequestVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}baseRequestLightVO"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="otpContesto" type="{http://p2b.gft.it/srv/pp}otpContestoVO" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "baseRequestVO", propOrder = {
    "otpContesto"
})
@XmlSeeAlso({
    RequestStornoPagamentoVO.class,
    RequestInserimentoRichiestaPagamentoPagoPaVO.class,
    RequestInquiryTransactionStatusVO.class
})
public class BaseRequestVO
    extends BaseRequestLightVO
{

    protected OtpContestoVO otpContesto;

    /**
     * Recupera il valore della proprietà otpContesto.
     * 
     * @return
     *     possible object is
     *     {@link OtpContestoVO }
     *     
     */
    public OtpContestoVO getOtpContesto() {
        return otpContesto;
    }

    /**
     * Imposta il valore della proprietà otpContesto.
     * 
     * @param value
     *     allowed object is
     *     {@link OtpContestoVO }
     *     
     */
    public void setOtpContesto(OtpContestoVO value) {
        this.otpContesto = value;
    }

}
