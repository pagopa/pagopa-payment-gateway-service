//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.22 alle 02:24:45 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per utenteVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="utenteVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}p2BAbstractPresentationBean"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="codGruppo" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="codIstituto" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="codUtente" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="nome" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="cognome" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="codiceFiscale" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "utenteVO", propOrder = {
    "codGruppo",
    "codIstituto",
    "codUtente",
    "nome",
    "cognome",
    "codiceFiscale"
})
@XmlSeeAlso({
    UtenteAttivoVO.class
})
public class UtenteVO
    extends P2BAbstractPresentationBean
{

    @XmlElement(required = true)
    protected String codGruppo;
    @XmlElement(required = true)
    protected String codIstituto;
    @XmlElement(required = true)
    protected String codUtente;
    protected String nome;
    protected String cognome;
    protected String codiceFiscale;

    /**
     * Recupera il valore della proprietà codGruppo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodGruppo() {
        return codGruppo;
    }

    /**
     * Imposta il valore della proprietà codGruppo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodGruppo(String value) {
        this.codGruppo = value;
    }

    /**
     * Recupera il valore della proprietà codIstituto.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodIstituto() {
        return codIstituto;
    }

    /**
     * Imposta il valore della proprietà codIstituto.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodIstituto(String value) {
        this.codIstituto = value;
    }

    /**
     * Recupera il valore della proprietà codUtente.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodUtente() {
        return codUtente;
    }

    /**
     * Imposta il valore della proprietà codUtente.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodUtente(String value) {
        this.codUtente = value;
    }

    /**
     * Recupera il valore della proprietà nome.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNome() {
        return nome;
    }

    /**
     * Imposta il valore della proprietà nome.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNome(String value) {
        this.nome = value;
    }

    /**
     * Recupera il valore della proprietà cognome.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCognome() {
        return cognome;
    }

    /**
     * Imposta il valore della proprietà cognome.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCognome(String value) {
        this.cognome = value;
    }

    /**
     * Recupera il valore della proprietà codiceFiscale.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    /**
     * Imposta il valore della proprietà codiceFiscale.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodiceFiscale(String value) {
        this.codiceFiscale = value;
    }

}
