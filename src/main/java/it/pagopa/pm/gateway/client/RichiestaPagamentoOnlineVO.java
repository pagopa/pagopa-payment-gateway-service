//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.22 alle 02:24:45 PM CET 
//


package it.pagopa.pm.gateway.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per richiestaPagamentoOnlineVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="richiestaPagamentoOnlineVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}p2BAbstractPresentationBean"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
 *         &lt;element name="idTransazione" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="tag" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="idNegozio" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/&gt;
 *         &lt;element name="importo" type="{http://www.w3.org/2001/XMLSchema}decimal"/&gt;
 *         &lt;element name="causale" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="numeroTelefonico" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="merchantName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="utenza" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="idOrdine" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="categoriaPagamento" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="tipoRichiestaPagamento" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "richiestaPagamentoOnlineVO", propOrder = {
    "id",
    "idTransazione",
    "tag",
    "idNegozio",
    "importo",
    "causale",
    "numeroTelefonico",
    "merchantName",
    "utenza",
    "idOrdine",
    "categoriaPagamento",
    "tipoRichiestaPagamento"
})
@XmlSeeAlso({
    RichiestaPagamentoPagoPaVO.class
})
public class RichiestaPagamentoOnlineVO
    extends P2BAbstractPresentationBean
{

    protected BigInteger id;
    @XmlElement(required = true)
    protected String idTransazione;
    @XmlElement(required = true)
    protected String tag;
    protected BigInteger idNegozio;
    @XmlElement(required = true)
    protected BigDecimal importo;
    protected String causale;
    @XmlElement(required = true)
    protected String numeroTelefonico;
    protected String merchantName;
    protected String utenza;
    protected String idOrdine;
    protected String categoriaPagamento;
    protected String tipoRichiestaPagamento;

    /**
     * Recupera il valore della proprietà id.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Imposta il valore della proprietà id.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setId(BigInteger value) {
        this.id = value;
    }

    /**
     * Recupera il valore della proprietà idTransazione.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdTransazione() {
        return idTransazione;
    }

    /**
     * Imposta il valore della proprietà idTransazione.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdTransazione(String value) {
        this.idTransazione = value;
    }

    /**
     * Recupera il valore della proprietà tag.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTag() {
        return tag;
    }

    /**
     * Imposta il valore della proprietà tag.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTag(String value) {
        this.tag = value;
    }

    /**
     * Recupera il valore della proprietà idNegozio.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIdNegozio() {
        return idNegozio;
    }

    /**
     * Imposta il valore della proprietà idNegozio.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setIdNegozio(BigInteger value) {
        this.idNegozio = value;
    }

    /**
     * Recupera il valore della proprietà importo.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getImporto() {
        return importo;
    }

    /**
     * Imposta il valore della proprietà importo.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setImporto(BigDecimal value) {
        this.importo = value;
    }

    /**
     * Recupera il valore della proprietà causale.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCausale() {
        return causale;
    }

    /**
     * Imposta il valore della proprietà causale.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCausale(String value) {
        this.causale = value;
    }

    /**
     * Recupera il valore della proprietà numeroTelefonico.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumeroTelefonico() {
        return numeroTelefonico;
    }

    /**
     * Imposta il valore della proprietà numeroTelefonico.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumeroTelefonico(String value) {
        this.numeroTelefonico = value;
    }

    /**
     * Recupera il valore della proprietà merchantName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * Imposta il valore della proprietà merchantName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMerchantName(String value) {
        this.merchantName = value;
    }

    /**
     * Recupera il valore della proprietà utenza.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUtenza() {
        return utenza;
    }

    /**
     * Imposta il valore della proprietà utenza.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUtenza(String value) {
        this.utenza = value;
    }

    /**
     * Recupera il valore della proprietà idOrdine.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdOrdine() {
        return idOrdine;
    }

    /**
     * Imposta il valore della proprietà idOrdine.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdOrdine(String value) {
        this.idOrdine = value;
    }

    /**
     * Recupera il valore della proprietà categoriaPagamento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCategoriaPagamento() {
        return categoriaPagamento;
    }

    /**
     * Imposta il valore della proprietà categoriaPagamento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCategoriaPagamento(String value) {
        this.categoriaPagamento = value;
    }

    /**
     * Recupera il valore della proprietà tipoRichiestaPagamento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTipoRichiestaPagamento() {
        return tipoRichiestaPagamento;
    }

    /**
     * Imposta il valore della proprietà tipoRichiestaPagamento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTipoRichiestaPagamento(String value) {
        this.tipoRichiestaPagamento = value;
    }

}
