//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.06.16 à 09:37:26 AM CEST 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour csvAuditType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="csvAuditType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}auditType">
 *       &lt;sequence>
 *         &lt;element name="datasets" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="separator" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="outputHeader" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="taskNames" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}valuesType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "csvAuditType", propOrder = {
    "datasets",
    "separator",
    "outputHeader",
    "taskNames"
})
public class CsvAuditType
    extends AuditType
{

    protected String datasets;
    @XmlElement(defaultValue = ";")
    protected String separator = ";";
    @XmlElement(defaultValue = "true")
    protected Boolean outputHeader = true;
    protected ValuesType taskNames;

    /**
     * Obtient la valeur de la propriété datasets.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatasets() {
        return datasets;
    }

    /**
     * Définit la valeur de la propriété datasets.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatasets(String value) {
        this.datasets = value;
    }

    /**
     * Obtient la valeur de la propriété separator.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Définit la valeur de la propriété separator.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSeparator(String value) {
        this.separator = value;
    }

    /**
     * Obtient la valeur de la propriété outputHeader.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isOutputHeader() {
        return outputHeader;
    }

    /**
     * Définit la valeur de la propriété outputHeader.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOutputHeader(Boolean value) {
        this.outputHeader = value;
    }

    /**
     * Obtient la valeur de la propriété taskNames.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getTaskNames() {
        return taskNames;
    }

    /**
     * Définit la valeur de la propriété taskNames.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setTaskNames(ValuesType value) {
        this.taskNames = value;
    }

}
