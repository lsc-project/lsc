//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.05.15 à 05:01:12 PM CEST 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour conditionsType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="conditionsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="create" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="update" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="delete" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="changeId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionsType", propOrder = {
    "create",
    "update",
    "delete",
    "changeId"
})
public class ConditionsType {

    protected String create;
    protected String update;
    protected String delete;
    protected String changeId;

    /**
     * Obtient la valeur de la propriété create.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCreate() {
        return create;
    }

    /**
     * Définit la valeur de la propriété create.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCreate(String value) {
        this.create = value;
    }

    /**
     * Obtient la valeur de la propriété update.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdate() {
        return update;
    }

    /**
     * Définit la valeur de la propriété update.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdate(String value) {
        this.update = value;
    }

    /**
     * Obtient la valeur de la propriété delete.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDelete() {
        return delete;
    }

    /**
     * Définit la valeur de la propriété delete.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDelete(String value) {
        this.delete = value;
    }

    /**
     * Obtient la valeur de la propriété changeId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChangeId() {
        return changeId;
    }

    /**
     * Définit la valeur de la propriété changeId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChangeId(String value) {
        this.changeId = value;
    }

}
