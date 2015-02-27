//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.02.27 à 04:49:36 PM CET 
//


package org.lsc.configuration;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour propertiesBasedSyncOptionsType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="propertiesBasedSyncOptionsType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}syncOptionsType">
 *       &lt;sequence>
 *         &lt;element name="pivotTransformation" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}pivotTransformationType" minOccurs="0"/>
 *         &lt;element name="defaultDelimiter" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="defaultPolicy" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}policyType"/>
 *         &lt;element name="conditions" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}conditionsType" minOccurs="0"/>
 *         &lt;element name="dataset" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}datasetType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "propertiesBasedSyncOptionsType", propOrder = {
    "pivotTransformation",
    "defaultDelimiter",
    "defaultPolicy",
    "conditions",
    "dataset"
})
public class PropertiesBasedSyncOptionsType
    extends SyncOptionsType
{

    protected PivotTransformationType pivotTransformation;
    @XmlElement(required = true, defaultValue = ";")
    protected String defaultDelimiter = ";";
    @XmlElement(required = true, defaultValue = "FORCE")
    protected PolicyType defaultPolicy = PolicyType.FORCE;
    protected ConditionsType conditions;
    protected List<DatasetType> dataset;

    /**
     * Obtient la valeur de la propriété pivotTransformation.
     * 
     * @return
     *     possible object is
     *     {@link PivotTransformationType }
     *     
     */
    public PivotTransformationType getPivotTransformation() {
        return pivotTransformation;
    }

    /**
     * Définit la valeur de la propriété pivotTransformation.
     * 
     * @param value
     *     allowed object is
     *     {@link PivotTransformationType }
     *     
     */
    public void setPivotTransformation(PivotTransformationType value) {
        this.pivotTransformation = value;
    }

    /**
     * Obtient la valeur de la propriété defaultDelimiter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultDelimiter() {
        return defaultDelimiter;
    }

    /**
     * Définit la valeur de la propriété defaultDelimiter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultDelimiter(String value) {
        this.defaultDelimiter = value;
    }

    /**
     * Obtient la valeur de la propriété defaultPolicy.
     * 
     * @return
     *     possible object is
     *     {@link PolicyType }
     *     
     */
    public PolicyType getDefaultPolicy() {
        return defaultPolicy;
    }

    /**
     * Définit la valeur de la propriété defaultPolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link PolicyType }
     *     
     */
    public void setDefaultPolicy(PolicyType value) {
        this.defaultPolicy = value;
    }

    /**
     * Obtient la valeur de la propriété conditions.
     * 
     * @return
     *     possible object is
     *     {@link ConditionsType }
     *     
     */
    public ConditionsType getConditions() {
        return conditions;
    }

    /**
     * Définit la valeur de la propriété conditions.
     * 
     * @param value
     *     allowed object is
     *     {@link ConditionsType }
     *     
     */
    public void setConditions(ConditionsType value) {
        this.conditions = value;
    }

    /**
     * Gets the value of the dataset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DatasetType }
     * 
     * 
     */
    public List<DatasetType> getDataset() {
        if (dataset == null) {
            dataset = new ArrayList<DatasetType>();
        }
        return this.dataset;
    }

}
