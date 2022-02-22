//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB) Reference Implementation, v2.3.0 
// Vedere <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello schema di origine. 
// Generato il: 2022.02.22 alle 02:24:45 PM CET 
//


package it.pagopa.pm.gateway.client;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per linguaEnum.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * <p>
 * <pre>
 * &lt;simpleType name="linguaEnum"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="IT"/&gt;
 *     &lt;enumeration value="EN"/&gt;
 *     &lt;enumeration value="DE"/&gt;
 *     &lt;enumeration value="AT"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "linguaEnum")
@XmlEnum
public enum LinguaEnum {

    IT,
    EN,
    DE,
    AT;

    public String value() {
        return name();
    }

    public static LinguaEnum fromValue(String v) {
        return valueOf(v);
    }

}
