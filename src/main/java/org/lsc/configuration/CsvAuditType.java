//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.07.16 at 07:22:18 PM CEST 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for csvAuditType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="csvAuditType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}auditType">
 *       &lt;sequence>
 *         &lt;element name="datasets" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="separator" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="outputHeader" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="taskNames" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}valuesType" minOccurs="0"/>
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
     * Gets the value of the datasets property.
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
     * Sets the value of the datasets property.
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
     * Gets the value of the separator property.
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
     * Sets the value of the separator property.
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
     * Gets the value of the outputHeader property.
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
     * Sets the value of the outputHeader property.
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
     * Gets the value of the taskNames property.
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
     * Sets the value of the taskNames property.
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
