//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.05.15 à 12:14:31 PM CEST 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour anonymous complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="connections" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}connectionsType"/>
 *         &lt;element name="audits" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}auditsType" minOccurs="0"/>
 *         &lt;element name="tasks" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}tasksType"/>
 *         &lt;element name="security" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}securityType" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="revision" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "lsc")
public class Lsc {

    @XmlElement(required = true)
    protected ConnectionsType connections;
    protected AuditsType audits;
    @XmlElement(required = true)
    protected TasksType tasks;
    protected SecurityType security;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "revision")
    protected Integer revision;

    /**
     * Obtient la valeur de la propriété connections.
     * 
     * @return
     *     possible object is
     *     {@link ConnectionsType }
     *     
     */
    public ConnectionsType getConnections() {
        return connections;
    }

    /**
     * Définit la valeur de la propriété connections.
     * 
     * @param value
     *     allowed object is
     *     {@link ConnectionsType }
     *     
     */
    public void setConnections(ConnectionsType value) {
        this.connections = value;
    }

    /**
     * Obtient la valeur de la propriété audits.
     * 
     * @return
     *     possible object is
     *     {@link AuditsType }
     *     
     */
    public AuditsType getAudits() {
        return audits;
    }

    /**
     * Définit la valeur de la propriété audits.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditsType }
     *     
     */
    public void setAudits(AuditsType value) {
        this.audits = value;
    }

    /**
     * Obtient la valeur de la propriété tasks.
     * 
     * @return
     *     possible object is
     *     {@link TasksType }
     *     
     */
    public TasksType getTasks() {
        return tasks;
    }

    /**
     * Définit la valeur de la propriété tasks.
     * 
     * @param value
     *     allowed object is
     *     {@link TasksType }
     *     
     */
    public void setTasks(TasksType value) {
        this.tasks = value;
    }

    /**
     * Obtient la valeur de la propriété security.
     * 
     * @return
     *     possible object is
     *     {@link SecurityType }
     *     
     */
    public SecurityType getSecurity() {
        return security;
    }

    /**
     * Définit la valeur de la propriété security.
     * 
     * @param value
     *     allowed object is
     *     {@link SecurityType }
     *     
     */
    public void setSecurity(SecurityType value) {
        this.security = value;
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

    /**
     * Obtient la valeur de la propriété revision.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getRevision() {
        return revision;
    }

    /**
     * Définit la valeur de la propriété revision.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setRevision(Integer value) {
        this.revision = value;
    }

}
