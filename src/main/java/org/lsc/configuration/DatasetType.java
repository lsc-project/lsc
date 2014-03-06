//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.03.06 à 05:49:07 PM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour datasetType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="datasetType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="policy" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}policyType" minOccurs="0"/>
 *         &lt;element name="defaultValues" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="forceValues" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="createValues" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="delimiter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "datasetType", propOrder = {
    "name",
    "policy",
    "defaultValues",
    "forceValues",
    "createValues",
    "delimiter"
})
public class DatasetType {

    @XmlElement(required = true)
    protected String name;
    protected PolicyType policy;
    protected ValuesType defaultValues;
    protected ValuesType forceValues;
    protected ValuesType createValues;
    protected String delimiter;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Obtient la valeur de la propriété name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Définit la valeur de la propriété name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Obtient la valeur de la propriété policy.
     * 
     * @return
     *     possible object is
     *     {@link PolicyType }
     *     
     */
    public PolicyType getPolicy() {
        return policy;
    }

    /**
     * Définit la valeur de la propriété policy.
     * 
     * @param value
     *     allowed object is
     *     {@link PolicyType }
     *     
     */
    public void setPolicy(PolicyType value) {
        this.policy = value;
    }

    /**
     * Obtient la valeur de la propriété defaultValues.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getDefaultValues() {
        return defaultValues;
    }

    /**
     * Définit la valeur de la propriété defaultValues.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setDefaultValues(ValuesType value) {
        this.defaultValues = value;
    }

    /**
     * Obtient la valeur de la propriété forceValues.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getForceValues() {
        return forceValues;
    }

    /**
     * Définit la valeur de la propriété forceValues.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setForceValues(ValuesType value) {
        this.forceValues = value;
    }

    /**
     * Obtient la valeur de la propriété createValues.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getCreateValues() {
        return createValues;
    }

    /**
     * Définit la valeur de la propriété createValues.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setCreateValues(ValuesType value) {
        this.createValues = value;
    }

    /**
     * Obtient la valeur de la propriété delimiter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Définit la valeur de la propriété delimiter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDelimiter(String value) {
        this.delimiter = value;
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
