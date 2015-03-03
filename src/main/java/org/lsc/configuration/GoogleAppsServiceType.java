//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.6 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.12.19 à 03:29:13 PM CET 
//


package org.lsc.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour googleAppsServiceType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="googleAppsServiceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}serviceType">
 *       &lt;sequence>
 *         &lt;element name="apiCategory" type="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}googleAppsProvisioningType"/>
 *         &lt;element name="quotaLimitInMb" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "googleAppsServiceType", propOrder = {
    "apiCategory",
    "quotaLimitInMb"
})
public class GoogleAppsServiceType
    extends ServiceType
{

    @XmlElement(required = true, defaultValue = "UserAccounts")
    protected GoogleAppsProvisioningType apiCategory = GoogleAppsProvisioningType.USER_ACCOUNTS;
    @XmlElement(defaultValue = "25000")
    protected Integer quotaLimitInMb = 25000;

    /**
     * Obtient la valeur de la propriété apiCategory.
     * 
     * @return
     *     possible object is
     *     {@link GoogleAppsProvisioningType }
     *     
     */
    public GoogleAppsProvisioningType getApiCategory() {
        return apiCategory;
    }

    /**
     * Définit la valeur de la propriété apiCategory.
     * 
     * @param value
     *     allowed object is
     *     {@link GoogleAppsProvisioningType }
     *     
     */
    public void setApiCategory(GoogleAppsProvisioningType value) {
        this.apiCategory = value;
    }

    /**
     * Obtient la valeur de la propriété quotaLimitInMb.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getQuotaLimitInMb() {
        return quotaLimitInMb;
    }

    /**
     * Définit la valeur de la propriété quotaLimitInMb.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setQuotaLimitInMb(Integer value) {
        this.quotaLimitInMb = value;
    }

}
