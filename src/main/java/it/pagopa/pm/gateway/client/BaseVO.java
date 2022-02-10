//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.09 alle 04:22:41 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per baseVO complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="baseVO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://p2b.gft.it/srv/pp}p2BAbstractPresentationBean"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="contesto" type="{http://p2b.gft.it/srv/pp}contestoVO"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "baseVO", propOrder = {
    "contesto"
})
@XmlSeeAlso({
    BaseRequestLightVO.class,
    BaseResponseVO.class
})
public class BaseVO
    extends P2BAbstractPresentationBean
{

    @XmlElement(required = true)
    protected ContestoVO contesto;

    /**
     * Recupera il valore della proprietà contesto.
     * 
     * @return
     *     possible object is
     *     {@link ContestoVO }
     *     
     */
    public ContestoVO getContesto() {
        return contesto;
    }

    /**
     * Imposta il valore della proprietà contesto.
     * 
     * @param value
     *     allowed object is
     *     {@link ContestoVO }
     *     
     */
    public void setContesto(ContestoVO value) {
        this.contesto = value;
    }

}
