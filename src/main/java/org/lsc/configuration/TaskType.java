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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Classe Java pour taskType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="taskType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}ID"/>
 *         &lt;element name="bean" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="cleanHook" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="syncHook" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="databaseSourceService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}databaseSourceServiceType"/>
 *           &lt;element name="ldapSourceService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}ldapSourceServiceType"/>
 *           &lt;element name="asyncLdapSourceService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}asyncLdapSourceServiceType"/>
 *           &lt;element name="pluginSourceService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}pluginSourceServiceType"/>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;element name="databaseDestinationService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}databaseDestinationServiceType"/>
 *           &lt;element name="ldapDestinationService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}ldapDestinationServiceType"/>
 *           &lt;element name="multiDestinationService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}multiDestinationServiceType"/>
 *           &lt;element name="xaFileDestinationService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}xaFileDestinationServiceType"/>
 *           &lt;element name="pluginDestinationService" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}pluginDestinationServiceType"/>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;element name="propertiesBasedSyncOptions" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}propertiesBasedSyncOptionsType"/>
 *           &lt;element name="forceSyncOptions" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}forceSyncOptionsType"/>
 *           &lt;element name="pluginSyncOptions" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}pluginSyncOptionsType"/>
 *         &lt;/choice>
 *         &lt;element name="customLibrary" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="scriptInclude" type="{http://lsc-project.org/XSD/lsc-core-2.2.xsd}valuesType" minOccurs="0"/>
 *         &lt;element name="auditLog" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="reference" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
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
@XmlType(name = "taskType", propOrder = {
    "name",
    "bean",
    "cleanHook",
    "syncHook",
    "databaseSourceService",
    "ldapSourceService",
    "asyncLdapSourceService",
    "pluginSourceService",
    "databaseDestinationService",
    "ldapDestinationService",
    "multiDestinationService",
    "xaFileDestinationService",
    "pluginDestinationService",
    "propertiesBasedSyncOptions",
    "forceSyncOptions",
    "pluginSyncOptions",
    "customLibrary",
    "scriptInclude",
    "auditLog"
})
public class TaskType {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String name;
    @XmlElement(required = true, defaultValue = "org.lsc.beans.SimpleBean")
    protected String bean = "org.lsc.beans.SimpleBean";
    protected String cleanHook;
    protected String syncHook;
    protected DatabaseSourceServiceType databaseSourceService;
    protected LdapSourceServiceType ldapSourceService;
    protected AsyncLdapSourceServiceType asyncLdapSourceService;
    protected PluginSourceServiceType pluginSourceService;
    protected DatabaseDestinationServiceType databaseDestinationService;
    protected LdapDestinationServiceType ldapDestinationService;
    protected MultiDestinationServiceType multiDestinationService;
    protected XaFileDestinationServiceType xaFileDestinationService;
    protected PluginDestinationServiceType pluginDestinationService;
    protected PropertiesBasedSyncOptionsType propertiesBasedSyncOptions;
    protected ForceSyncOptionsType forceSyncOptions;
    protected PluginSyncOptionsType pluginSyncOptions;
    protected ValuesType customLibrary;
    protected ValuesType scriptInclude;
    protected List<TaskType.AuditLog> auditLog;
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
     * Obtient la valeur de la propriété bean.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBean() {
        return bean;
    }

    /**
     * Définit la valeur de la propriété bean.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBean(String value) {
        this.bean = value;
    }

    /**
     * Obtient la valeur de la propriété cleanHook.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCleanHook() {
        return cleanHook;
    }

    /**
     * Définit la valeur de la propriété cleanHook.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCleanHook(String value) {
        this.cleanHook = value;
    }

    /**
     * Obtient la valeur de la propriété syncHook.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSyncHook() {
        return syncHook;
    }

    /**
     * Définit la valeur de la propriété syncHook.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSyncHook(String value) {
        this.syncHook = value;
    }

    /**
     * Obtient la valeur de la propriété databaseSourceService.
     * 
     * @return
     *     possible object is
     *     {@link DatabaseSourceServiceType }
     *     
     */
    public DatabaseSourceServiceType getDatabaseSourceService() {
        return databaseSourceService;
    }

    /**
     * Définit la valeur de la propriété databaseSourceService.
     * 
     * @param value
     *     allowed object is
     *     {@link DatabaseSourceServiceType }
     *     
     */
    public void setDatabaseSourceService(DatabaseSourceServiceType value) {
        this.databaseSourceService = value;
    }

    /**
     * Obtient la valeur de la propriété ldapSourceService.
     * 
     * @return
     *     possible object is
     *     {@link LdapSourceServiceType }
     *     
     */
    public LdapSourceServiceType getLdapSourceService() {
        return ldapSourceService;
    }

    /**
     * Définit la valeur de la propriété ldapSourceService.
     * 
     * @param value
     *     allowed object is
     *     {@link LdapSourceServiceType }
     *     
     */
    public void setLdapSourceService(LdapSourceServiceType value) {
        this.ldapSourceService = value;
    }

    /**
     * Obtient la valeur de la propriété asyncLdapSourceService.
     * 
     * @return
     *     possible object is
     *     {@link AsyncLdapSourceServiceType }
     *     
     */
    public AsyncLdapSourceServiceType getAsyncLdapSourceService() {
        return asyncLdapSourceService;
    }

    /**
     * Définit la valeur de la propriété asyncLdapSourceService.
     * 
     * @param value
     *     allowed object is
     *     {@link AsyncLdapSourceServiceType }
     *     
     */
    public void setAsyncLdapSourceService(AsyncLdapSourceServiceType value) {
        this.asyncLdapSourceService = value;
    }

    /**
     * Obtient la valeur de la propriété pluginSourceService.
     * 
     * @return
     *     possible object is
     *     {@link PluginSourceServiceType }
     *     
     */
    public PluginSourceServiceType getPluginSourceService() {
        return pluginSourceService;
    }

    /**
     * Définit la valeur de la propriété pluginSourceService.
     * 
     * @param value
     *     allowed object is
     *     {@link PluginSourceServiceType }
     *     
     */
    public void setPluginSourceService(PluginSourceServiceType value) {
        this.pluginSourceService = value;
    }

    /**
     * Obtient la valeur de la propriété databaseDestinationService.
     * 
     * @return
     *     possible object is
     *     {@link DatabaseDestinationServiceType }
     *     
     */
    public DatabaseDestinationServiceType getDatabaseDestinationService() {
        return databaseDestinationService;
    }

    /**
     * Définit la valeur de la propriété databaseDestinationService.
     * 
     * @param value
     *     allowed object is
     *     {@link DatabaseDestinationServiceType }
     *     
     */
    public void setDatabaseDestinationService(DatabaseDestinationServiceType value) {
        this.databaseDestinationService = value;
    }

    /**
     * Obtient la valeur de la propriété ldapDestinationService.
     * 
     * @return
     *     possible object is
     *     {@link LdapDestinationServiceType }
     *     
     */
    public LdapDestinationServiceType getLdapDestinationService() {
        return ldapDestinationService;
    }

    /**
     * Définit la valeur de la propriété ldapDestinationService.
     * 
     * @param value
     *     allowed object is
     *     {@link LdapDestinationServiceType }
     *     
     */
    public void setLdapDestinationService(LdapDestinationServiceType value) {
        this.ldapDestinationService = value;
    }

    /**
     * Obtient la valeur de la propriété multiDestinationService.
     * 
     * @return
     *     possible object is
     *     {@link MultiDestinationServiceType }
     *     
     */
    public MultiDestinationServiceType getMultiDestinationService() {
        return multiDestinationService;
    }

    /**
     * Définit la valeur de la propriété multiDestinationService.
     * 
     * @param value
     *     allowed object is
     *     {@link MultiDestinationServiceType }
     *     
     */
    public void setMultiDestinationService(MultiDestinationServiceType value) {
        this.multiDestinationService = value;
    }

    /**
     * Obtient la valeur de la propriété xaFileDestinationService.
     * 
     * @return
     *     possible object is
     *     {@link XaFileDestinationServiceType }
     *     
     */
    public XaFileDestinationServiceType getXaFileDestinationService() {
        return xaFileDestinationService;
    }

    /**
     * Définit la valeur de la propriété xaFileDestinationService.
     * 
     * @param value
     *     allowed object is
     *     {@link XaFileDestinationServiceType }
     *     
     */
    public void setXaFileDestinationService(XaFileDestinationServiceType value) {
        this.xaFileDestinationService = value;
    }

    /**
     * Obtient la valeur de la propriété pluginDestinationService.
     * 
     * @return
     *     possible object is
     *     {@link PluginDestinationServiceType }
     *     
     */
    public PluginDestinationServiceType getPluginDestinationService() {
        return pluginDestinationService;
    }

    /**
     * Définit la valeur de la propriété pluginDestinationService.
     * 
     * @param value
     *     allowed object is
     *     {@link PluginDestinationServiceType }
     *     
     */
    public void setPluginDestinationService(PluginDestinationServiceType value) {
        this.pluginDestinationService = value;
    }

    /**
     * Obtient la valeur de la propriété propertiesBasedSyncOptions.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesBasedSyncOptionsType }
     *     
     */
    public PropertiesBasedSyncOptionsType getPropertiesBasedSyncOptions() {
        return propertiesBasedSyncOptions;
    }

    /**
     * Définit la valeur de la propriété propertiesBasedSyncOptions.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesBasedSyncOptionsType }
     *     
     */
    public void setPropertiesBasedSyncOptions(PropertiesBasedSyncOptionsType value) {
        this.propertiesBasedSyncOptions = value;
    }

    /**
     * Obtient la valeur de la propriété forceSyncOptions.
     * 
     * @return
     *     possible object is
     *     {@link ForceSyncOptionsType }
     *     
     */
    public ForceSyncOptionsType getForceSyncOptions() {
        return forceSyncOptions;
    }

    /**
     * Définit la valeur de la propriété forceSyncOptions.
     * 
     * @param value
     *     allowed object is
     *     {@link ForceSyncOptionsType }
     *     
     */
    public void setForceSyncOptions(ForceSyncOptionsType value) {
        this.forceSyncOptions = value;
    }

    /**
     * Obtient la valeur de la propriété pluginSyncOptions.
     * 
     * @return
     *     possible object is
     *     {@link PluginSyncOptionsType }
     *     
     */
    public PluginSyncOptionsType getPluginSyncOptions() {
        return pluginSyncOptions;
    }

    /**
     * Définit la valeur de la propriété pluginSyncOptions.
     * 
     * @param value
     *     allowed object is
     *     {@link PluginSyncOptionsType }
     *     
     */
    public void setPluginSyncOptions(PluginSyncOptionsType value) {
        this.pluginSyncOptions = value;
    }

    /**
     * Obtient la valeur de la propriété customLibrary.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getCustomLibrary() {
        return customLibrary;
    }

    /**
     * Définit la valeur de la propriété customLibrary.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setCustomLibrary(ValuesType value) {
        this.customLibrary = value;
    }

    /**
     * Obtient la valeur de la propriété scriptInclude.
     * 
     * @return
     *     possible object is
     *     {@link ValuesType }
     *     
     */
    public ValuesType getScriptInclude() {
        return scriptInclude;
    }

    /**
     * Définit la valeur de la propriété scriptInclude.
     * 
     * @param value
     *     allowed object is
     *     {@link ValuesType }
     *     
     */
    public void setScriptInclude(ValuesType value) {
        this.scriptInclude = value;
    }

    /**
     * Gets the value of the auditLog property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditLog property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditLog().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TaskType.AuditLog }
     * 
     * 
     */
    public List<TaskType.AuditLog> getAuditLog() {
        if (auditLog == null) {
            auditLog = new ArrayList<TaskType.AuditLog>();
        }
        return this.auditLog;
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
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="reference" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class AuditLog {

        @XmlAttribute(name = "reference", required = true)
        @XmlIDREF
        @XmlSchemaType(name = "IDREF")
        protected AuditType reference;

        /**
         * Obtient la valeur de la propriété reference.
         * 
         * @return
         *     possible object is
         *     {@link Object }
         *     
         */
        public AuditType getReference() {
            return reference;
        }

        /**
         * Définit la valeur de la propriété reference.
         * 
         * @param value
         *     allowed object is
         *     {@link Object }
         *     
         */
        public void setReference(AuditType value) {
            this.reference = value;
        }

    }

}
