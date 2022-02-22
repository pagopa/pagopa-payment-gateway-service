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
 * <p>Classe Java per richiestaPagamentoPagoPaVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="richiestaPagamentoPagoPaVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}richiestaPagamentoOnlineVO"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="numeroTelefonicoCriptato" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="idPSP" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="idPagoPa" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "richiestaPagamentoPagoPaVO", propOrder = {
    "numeroTelefonicoCriptato",
    "idPSP",
    "idPagoPa"
})
public class RichiestaPagamentoPagoPaVO
    extends RichiestaPagamentoOnlineVO
{

    protected String numeroTelefonicoCriptato;
    protected String idPSP;
    protected String idPagoPa;

    /**
     * Recupera il valore della proprietà numeroTelefonicoCriptato.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumeroTelefonicoCriptato() {
        return numeroTelefonicoCriptato;
    }

    /**
     * Imposta il valore della proprietà numeroTelefonicoCriptato.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumeroTelefonicoCriptato(String value) {
        this.numeroTelefonicoCriptato = value;
    }

    /**
     * Recupera il valore della proprietà idPSP.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdPSP() {
        return idPSP;
    }

    /**
     * Imposta il valore della proprietà idPSP.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdPSP(String value) {
        this.idPSP = value;
    }

    /**
     * Recupera il valore della proprietà idPagoPa.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdPagoPa() {
        return idPagoPa;
    }

    /**
     * Imposta il valore della proprietà idPagoPa.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdPagoPa(String value) {
        this.idPagoPa = value;
    }

}
