//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.03.06 à 10:51:01 AM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour asyncLdapSourceServiceType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="asyncLdapSourceServiceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}ldapSourceServiceType">
 *       &lt;sequence>
 *         &lt;element name="synchronizingAllWhenStarting" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="serverType" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}ldapServerType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "asyncLdapSourceServiceType", propOrder = {
    "synchronizingAllWhenStarting",
    "serverType"
})
public class AsyncLdapSourceServiceType
    extends LdapSourceServiceType
{

    @XmlElement(defaultValue = "true")
    protected Boolean synchronizingAllWhenStarting = true;
    @XmlElement(required = true)
    protected LdapServerType serverType;

    /**
     * Obtient la valeur de la propriété synchronizingAllWhenStarting.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSynchronizingAllWhenStarting() {
        return synchronizingAllWhenStarting;
    }

    /**
     * Définit la valeur de la propriété synchronizingAllWhenStarting.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSynchronizingAllWhenStarting(Boolean value) {
        this.synchronizingAllWhenStarting = value;
    }

    /**
     * Obtient la valeur de la propriété serverType.
     * 
     * @return
     *     possible object is
     *     {@link LdapServerType }
     *     
     */
    public LdapServerType getServerType() {
        return serverType;
    }

    /**
     * Définit la valeur de la propriété serverType.
     * 
     * @param value
     *     allowed object is
     *     {@link LdapServerType }
     *     
     */
    public void setServerType(LdapServerType value) {
        this.serverType = value;
    }

}
