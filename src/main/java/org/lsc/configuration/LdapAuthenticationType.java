//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.02.23 à 10:31:02 AM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour ldapAuthenticationType.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="ldapAuthenticationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="NONE"/>
 *     &lt;enumeration value="SIMPLE"/>
 *     &lt;enumeration value="SASL"/>
 *     &lt;enumeration value="DIGEST-MD5"/>
 *     &lt;enumeration value="GSSAPI"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ldapAuthenticationType")
@XmlEnum
public enum LdapAuthenticationType {

    NONE("NONE"),
    SIMPLE("SIMPLE"),
    SASL("SASL"),
    @XmlEnumValue("DIGEST-MD5")
    DIGEST_MD_5("DIGEST-MD5"),
    GSSAPI("GSSAPI");
    private final String value;

    LdapAuthenticationType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LdapAuthenticationType fromValue(String v) {
        for (LdapAuthenticationType c: LdapAuthenticationType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
