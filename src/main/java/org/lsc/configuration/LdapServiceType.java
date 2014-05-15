//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.05.15 à 05:01:12 PM CEST 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour ldapServiceType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="ldapServiceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}serviceType">
 *       &lt;sequence>
 *         &lt;element name="baseDn" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pivotAttributes" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="fetchedAttributes" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="getAllFilter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="allFilter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;element name="getOneFilter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="oneFilter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ldapServiceType", propOrder = {
    "baseDn",
    "pivotAttributes",
    "fetchedAttributes",
    "getAllFilter",
    "allFilter",
    "getOneFilter",
    "oneFilter"
})
@XmlSeeAlso({
    LdapDestinationServiceType.class,
    LdapSourceServiceType.class
})
public abstract class LdapServiceType
    extends ServiceType
{

    @XmlElement(required = true)
    protected String baseDn;
    protected ValuesType pivotAttributes;
    protected ValuesType fetchedAttributes;
    protected String getAllFilter;
    protected String allFilter;
    protected String getOneFilter;
    protected String oneFilter;

    /**
     * Obtient la valeur de la propriété baseDn.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBaseDn() {
        return baseDn;
    }

    /**
     * Définit la valeur de la propriété baseDn.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBaseDn(String value) {
        this.baseDn = value;
    }

    /**
     * Obtient la valeur de la propriété pivotAttributes.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getPivotAttributes() {
        return pivotAttributes;
    }

    /**
     * Définit la valeur de la propriété pivotAttributes.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setPivotAttributes(ValuesType value) {
        this.pivotAttributes = value;
    }

    /**
     * Obtient la valeur de la propriété fetchedAttributes.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getFetchedAttributes() {
        return fetchedAttributes;
    }

    /**
     * Définit la valeur de la propriété fetchedAttributes.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setFetchedAttributes(ValuesType value) {
        this.fetchedAttributes = value;
    }

    /**
     * Obtient la valeur de la propriété getAllFilter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetAllFilter() {
        return getAllFilter;
    }

    /**
     * Définit la valeur de la propriété getAllFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetAllFilter(String value) {
        this.getAllFilter = value;
    }

    /**
     * Obtient la valeur de la propriété allFilter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllFilter() {
        return allFilter;
    }

    /**
     * Définit la valeur de la propriété allFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllFilter(String value) {
        this.allFilter = value;
    }

    /**
     * Obtient la valeur de la propriété getOneFilter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGetOneFilter() {
        return getOneFilter;
    }

    /**
     * Définit la valeur de la propriété getOneFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGetOneFilter(String value) {
        this.getOneFilter = value;
    }

    /**
     * Obtient la valeur de la propriété oneFilter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOneFilter() {
        return oneFilter;
    }

    /**
     * Définit la valeur de la propriété oneFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOneFilter(String value) {
        this.oneFilter = value;
    }

}
