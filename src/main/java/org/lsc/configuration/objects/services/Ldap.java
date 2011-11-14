/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008 - 2011 LSC Project 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *                  ==LICENSE NOTICE==
 *
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.configuration.objects.services;

import java.util.StringTokenizer;

import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.configuration.objects.Service;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.jndi.JndiServices;


/**
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class Ldap extends Service {

	/**
	 * <service type="SimpleJndiSrcService">
			<name>myADAccount</name>
			<connection>myAdServer</connection>
			<baseDn>ou=people</baseDn>
			<pivotAttributes>dn, cn</pivotAttributes>
			<fetchAttributes>uid, mail, cn</fetchAttributes>
			<dn>'cn=' + srcBean.getAttributeValueById('uid')</dn>
			<getAllFilter>(&(objectClass=inetOrgPerson)(uid=*))</getAllFilter>
			<getOneFilter>(&(objectClass=inetOrgPerson)(uid={uid}))</getOneFilter>
		</service>
	 */
	
	
	private String baseDn;
	
	@Validate("required")
	private String[] pivotAttributes;
	
	@Validate("required")
	private String[] fetchedAttributes;
	
	private String dn;
	
	@Validate("required")
	private String getAllFilter;
	
	@Validate("required")
	private String getOneFilter;

	public String getBaseDn() {
		return baseDn;
	}

	public void setBaseDn(String baseDn) {
		this.baseDn = baseDn;
	}

	public String[] getPivotAttributes() {
		return pivotAttributes;
	}

	public void setPivotAttributes(String pivotAttributes) {
		StringTokenizer sTok = new StringTokenizer(pivotAttributes, " ");
		this.pivotAttributes = new String[sTok.countTokens()];
		for(int i = 0; sTok.hasMoreElements(); i++) {
			this.pivotAttributes[i] = sTok.nextToken();
		}
	}

	public void setFetchedAttributes(String fetchedAttributes) {
		StringTokenizer sTok = new StringTokenizer(fetchedAttributes, " ");
		this.fetchedAttributes = new String[sTok.countTokens()];
		for(int i = 0; sTok.hasMoreElements(); i++) {
			this.fetchedAttributes[i] = sTok.nextToken();
		}
	}
	
	public void setPivotAttributes(String[] pivotAttributes) {
		this.pivotAttributes = pivotAttributes;
	}

	public String[] getFetchedAttributes() {
		return fetchedAttributes;
	}

	public void setFetchedAttributes(String[] fetchedAttributes) {
		this.fetchedAttributes = fetchedAttributes;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getGetAllFilter() {
		return getAllFilter;
	}

	public void setGetAllFilter(String getAllFilter) {
		this.getAllFilter = getAllFilter;
	}

	public String getGetOneFilter() {
		return getOneFilter;
	}

	public void setGetOneFilter(String getOneFilter) {
		this.getOneFilter = getOneFilter;
	}

	@Override
	public String getAlias(String name) {
		if("pivotAttrs".equals(name)) {
			return "pivotAttributes";
		} else if("filterId".equals(name)) {
			return "getOneFilter";
		} else if("filterIdClean".equals(name)) {
			return "getCleanFilter";
		} else if("filterAll".equals(name)) {
			return "getAllFilter";
		} else if("attrs".equals(name)) {
			return "fetchedAttributes";
		}
		return name;
	}

	@Override
	public void validate() throws LscServiceConfigurationException,
			LscServiceCommunicationException {
		try {
			// Need to check LDAP settings
			JndiServices.getInstance((org.lsc.configuration.objects.connection.directory.Ldap)this.getConnection());
		} catch (RuntimeException re) {
			if(re.getCause() instanceof LscServiceConfigurationException) {
				throw (LscServiceConfigurationException)re.getCause();
			} else if(re.getCause() instanceof LscServiceCommunicationException) {
				throw (LscServiceCommunicationException)re.getCause();
			} else {
				throw re;
			}
		}
	}
}
