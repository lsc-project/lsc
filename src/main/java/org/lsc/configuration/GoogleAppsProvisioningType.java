//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2017.02.03 à 01:41:23 PM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour googleAppsProvisioningType.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="googleAppsProvisioningType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="UserAccounts"/>
 *     &lt;enumeration value="Groups"/>
 *     &lt;enumeration value="OrganizationUnits"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "googleAppsProvisioningType")
@XmlEnum
public enum GoogleAppsProvisioningType {

    @XmlEnumValue("UserAccounts")
    USER_ACCOUNTS("UserAccounts"),
    @XmlEnumValue("Groups")
    GROUPS("Groups"),
    @XmlEnumValue("OrganizationUnits")
    ORGANIZATION_UNITS("OrganizationUnits");
    private final String value;

    GoogleAppsProvisioningType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static GoogleAppsProvisioningType fromValue(String v) {
        for (GoogleAppsProvisioningType c: GoogleAppsProvisioningType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
