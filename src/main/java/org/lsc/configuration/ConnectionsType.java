//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.02.23 à 10:31:02 AM CET 
//


package org.lsc.configuration;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour connectionsType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="connectionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="ldapConnection" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}ldapConnectionType"/>
 *         &lt;element name="databaseConnection" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}databaseConnectionType"/>
 *         &lt;element name="googleAppsConnection" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}googleAppsConnectionType"/>
 *         &lt;element name="pluginConnection" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}pluginConnectionType"/>
 *       &lt;/choice>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "connectionsType", propOrder = {
    "ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection"
})
public class ConnectionsType {

    @XmlElements({
        @XmlElement(name = "ldapConnection", type = LdapConnectionType.class),
        @XmlElement(name = "databaseConnection", type = DatabaseConnectionType.class),
        @XmlElement(name = "googleAppsConnection", type = GoogleAppsConnectionType.class),
        @XmlElement(name = "pluginConnection", type = PluginConnectionType.class)
    })
    protected List<ConnectionType> ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLdapConnectionOrDatabaseConnectionOrGoogleAppsConnection().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LdapConnectionType }
     * {@link DatabaseConnectionType }
     * {@link GoogleAppsConnectionType }
     * {@link PluginConnectionType }
     * 
     * 
     */
    public List<ConnectionType> getLdapConnectionOrDatabaseConnectionOrGoogleAppsConnection() {
        if (ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection == null) {
            ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection = new ArrayList<ConnectionType>();
        }
        return this.ldapConnectionOrDatabaseConnectionOrGoogleAppsConnection;
    }

    /**
     * Obtient la valeur de la propriété id.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Définit la valeur de la propriété id.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
