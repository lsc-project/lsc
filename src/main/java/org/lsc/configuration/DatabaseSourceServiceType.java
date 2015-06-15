//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.02.27 à 04:49:36 PM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour databaseSourceServiceType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="databaseSourceServiceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}serviceType">
 *       &lt;sequence>
 *         &lt;element name="interval" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="requestNameForList" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="requestNameForObject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="requestNameForNextId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="requestNameForClean" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "databaseSourceServiceType", propOrder = {
    "interval",
    "requestNameForList",
    "requestNameForObject",
    "requestNameForNextId",
    "requestNameForClean"
})
public class DatabaseSourceServiceType
    extends ServiceType
{

    @XmlElement(defaultValue = "5")
    protected Integer interval = 5;
    protected String requestNameForList;
    protected String requestNameForObject;
    protected String requestNameForNextId;
    protected String requestNameForClean;

    /**
     * Obtient la valeur de la propriété interval.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getInterval() {
        return interval;
    }

    /**
     * Définit la valeur de la propriété interval.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setInterval(Integer value) {
        this.interval = value;
    }

    /**
     * Obtient la valeur de la propriété requestNameForList.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestNameForList() {
        return requestNameForList;
    }

    /**
     * Définit la valeur de la propriété requestNameForList.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestNameForList(String value) {
        this.requestNameForList = value;
    }

    /**
     * Obtient la valeur de la propriété requestNameForObject.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestNameForObject() {
        return requestNameForObject;
    }

    /**
     * Définit la valeur de la propriété requestNameForObject.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestNameForObject(String value) {
        this.requestNameForObject = value;
    }

    /**
     * Obtient la valeur de la propriété requestNameForNextId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestNameForNextId() {
        return requestNameForNextId;
    }

    /**
     * Définit la valeur de la propriété requestNameForNextId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestNameForNextId(String value) {
        this.requestNameForNextId = value;
    }

    /**
     * Obtient la valeur de la propriété requestNameForClean.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestNameForClean() {
        return requestNameForClean;
    }

    /**
     * Définit la valeur de la propriété requestNameForClean.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestNameForClean(String value) {
        this.requestNameForClean = value;
    }

}
