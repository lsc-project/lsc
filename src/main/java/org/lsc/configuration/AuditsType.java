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
 * <p>Classe Java pour auditsType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="csvAudit" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}csvAuditType"/>
 *         &lt;element name="ldifAudit" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}ldifAuditType"/>
 *         &lt;element name="pluginAudit" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}pluginAuditType"/>
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
@XmlType(name = "auditsType", propOrder = {
    "csvAuditOrLdifAuditOrPluginAudit"
})
public class AuditsType {

    @XmlElements({
        @XmlElement(name = "csvAudit", type = CsvAuditType.class),
        @XmlElement(name = "ldifAudit", type = LdifAuditType.class),
        @XmlElement(name = "pluginAudit", type = PluginAuditType.class)
    })
    protected List<AuditType> csvAuditOrLdifAuditOrPluginAudit;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the csvAuditOrLdifAuditOrPluginAudit property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the csvAuditOrLdifAuditOrPluginAudit property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCsvAuditOrLdifAuditOrPluginAudit().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CsvAuditType }
     * {@link LdifAuditType }
     * {@link PluginAuditType }
     * 
     * 
     */
    public List<AuditType> getCsvAuditOrLdifAuditOrPluginAudit() {
        if (csvAuditOrLdifAuditOrPluginAudit == null) {
            csvAuditOrLdifAuditOrPluginAudit = new ArrayList<AuditType>();
        }
        return this.csvAuditOrLdifAuditOrPluginAudit;
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
