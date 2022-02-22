//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.21 alle 05:24:24 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per inserimentoRichiestaPagamentoPagoPaResponse complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="inserimentoRichiestaPagamentoPagoPaResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="return" type="{http://p2b.gft.it/srv/pp}responseInserimentoRichiestaPagamentoPagoPaVO" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "inserimentoRichiestaPagamentoPagoPaResponse", propOrder = {
    "_return"
})
public class InserimentoRichiestaPagamentoPagoPaResponse {

    @XmlElement(name = "return")
    protected ResponseInserimentoRichiestaPagamentoPagoPaVO _return;

    /**
     * Recupera il valore della proprietà return.
     * 
     * @return
     *     possible object is
     *     {@link ResponseInserimentoRichiestaPagamentoPagoPaVO }
     *     
     */
    public ResponseInserimentoRichiestaPagamentoPagoPaVO getReturn() {
        return _return;
    }

    /**
     * Imposta il valore della proprietà return.
     * 
     * @param value
     *     allowed object is
     *     {@link ResponseInserimentoRichiestaPagamentoPagoPaVO }
     *     
     */
    public void setReturn(ResponseInserimentoRichiestaPagamentoPagoPaVO value) {
        this._return = value;
    }

}
