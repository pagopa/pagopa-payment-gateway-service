//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.22 alle 02:24:45 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per inserimentoRichiestaPagamentoPagoPa complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="inserimentoRichiestaPagamentoPagoPa"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="arg0" type="{http://p2b.gft.it/srv/pp}requestInserimentoRichiestaPagamentoPagoPaVO" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "inserimentoRichiestaPagamentoPagoPa", propOrder = {
    "arg0"
})
public class InserimentoRichiestaPagamentoPagoPa {

    protected RequestInserimentoRichiestaPagamentoPagoPaVO arg0;

    /**
     * Recupera il valore della proprietà arg0.
     * 
     * @return
     *     possible object is
     *     {@link RequestInserimentoRichiestaPagamentoPagoPaVO }
     *     
     */
    public RequestInserimentoRichiestaPagamentoPagoPaVO getArg0() {
        return arg0;
    }

    /**
     * Imposta il valore della proprietà arg0.
     * 
     * @param value
     *     allowed object is
     *     {@link RequestInserimentoRichiestaPagamentoPagoPaVO }
     *     
     */
    public void setArg0(RequestInserimentoRichiestaPagamentoPagoPaVO value) {
        this.arg0 = value;
    }

}
