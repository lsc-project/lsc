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
package org.lsc.jndi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.Configuration;
import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.configuration.objects.services.Ldap;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an abstract generic but configurable implementation to get data
 * from the directory.
 * 
 * You can specify where (baseDn) and what (filterId & attr) information will be
 * read on which type of entries (filterAll and attrId).
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractSimpleJndiService {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimpleJndiService.class);
	/**
	 * The filter to be completed by replacing {0} by the id to find a unique
	 * entry. Use with source attributes while getting the object to synchronize
	 */
	private String filterIdSync;
	/**
	 * The filter to be completed by replacing {0} by the id to find a unique
	 * entry. Use with destination attributes while getting the object to check for suppression
	 */
	private String filterIdClean;
	/**
	 * The filter used to identify all the entries that have to be synchronized
	 * by this JndiSrcService.
	 */
	private String filterAll;
	/** Where to find the entries. */
	private String baseDn;
	/**
	 * When finding entries with 'filterAll' filter, the attribute to read to
	 * reuse it in 'filterId' filter.
	 */
	private List<String> attrsId;
	/**
	 * When a single entry is read in the directory, the attributes array to
	 * read - Used to limit at the source, the synchronization perimeter.
	 */
	private List<String> attrs;
	private SearchControls _filteredSc;

	protected JndiServices jndiServices;

	/**
	 * The default initializer.
	 * 
	 * @param serviceProps
	 *            The default simple JNDI properties
	 * @throws LscServiceConfigurationException
	 */
	@Deprecated
	public AbstractSimpleJndiService(final Properties serviceProps) throws LscServiceConfigurationException {
		baseDn = serviceProps.getProperty("baseDn");
		filterIdSync = serviceProps.getProperty("filterId");
		filterAll = serviceProps.getProperty("filterAll");
		_filteredSc = new SearchControls();

		String attrsValue = serviceProps.getProperty("attrs");
		if (attrsValue != null) {
			String[] attributes = attrsValue.split(" ");
			attrs = Arrays.asList(attributes);
			_filteredSc.setReturningAttributes(attributes);
		}

		String attrsIdValue = serviceProps.getProperty("pivotAttrs");
		if (attrsIdValue != null) {
			attrsId = Arrays.asList(attrsIdValue.split(" "));
		}

		// check that we have all parameters, or abort
		try {
			Configuration.assertPropertyNotEmpty("filterId", filterIdSync, this.getClass().getName());
			Configuration.assertPropertyNotEmpty("filterAll", filterAll, this.getClass().getName());
		} catch (LscConfigurationException e) {
			throw new LscServiceConfigurationException(e);
		}
		filterIdClean = serviceProps.getProperty("filterIdClean");
		try {
			jndiServices = JndiServices.getInstance(serviceProps);
		} catch (NamingException e) {
			throw new LscServiceConfigurationException(e);
		} catch (IOException e) {
			throw new LscServiceConfigurationException(e);
		}
		LOGGER.warn("Properties configuration is not any more supported ! Please consider upgrading your LSC version !");
	}

	/**
	 * The default initializer.
	 * 
	 * @param ldapService The Ldap settings object
	 * @throws LscServiceConfigurationException 
	 */
	public AbstractSimpleJndiService(final Ldap ldapService) throws LscServiceConfigurationException {
		baseDn = ldapService.getBaseDn();
		filterIdSync = ldapService.getGetOneFilter();
		filterAll = ldapService.getGetAllFilter();
		filterIdClean = ldapService.getGetCleanFilter();
		_filteredSc = new SearchControls();
		_filteredSc.setReturningAttributes(ldapService.getFetchedAttributes());
		attrsId = new ArrayList<String>(ldapService.getPivotAttributes().length); 
		for(String pivotAttr : ldapService.getPivotAttributes()) {
			attrsId.add(pivotAttr);
		}
		jndiServices = JndiServices.getInstance((org.lsc.configuration.objects.connection.directory.Ldap)ldapService.getConnection());
	}

	/**
	 * Map the ldap search result into a AbstractBean inherited object.
	 * 
	 * @param sr the ldap search result
	 * @param beanToFill
	 *            the bean to fill
	 * 
	 * @return the modified bean
	 * 
	 * @throws NamingException
	 *             thrown if a directory exception is encountered while
	 *             switching to the Java POJO
	 */
	public final IBean getBeanFromSR(final SearchResult sr,
					final IBean beanToFill) throws NamingException {

		if (sr == null) {
			return null;
		}

		// get dn
		beanToFill.setDistinguishedName(sr.getNameInNamespace());

		NamingEnumeration<?> ne = sr.getAttributes().getAll();
		while (ne.hasMore()) {
			Attribute attr = (Attribute) ne.next();
			beanToFill.setAttribute(attr);
		}
		return beanToFill;
	}

	/**
	 * Get a list of object values from the NamingEnumeration.
	 * 
	 * @param ne
	 *            the naming enumeration
	 * 
	 * @return the object list
	 * 
	 * @throws NamingException
	 *             thrown if a directory exception is encountered while
	 *             switching to the Java POJO
	 */
	protected static List<?> getValue(final NamingEnumeration<?> ne)
					throws NamingException {
		List<Object> l = new ArrayList<Object>();

		while (ne.hasMore()) {
			l.add(ne.next());
		}
		return l;
	}

	/**
	 * Get the ldap search result according the specified identifier.
	 * 
	 * @param id The object identifiers - used in the directory filter as {0} or any attributes name
	 * @return The ldap search result
	 * @throws NamingException
	 *             thrown if an directory exception is encountered while getting
	 *             the identified object
	 */
	public final SearchResult get(String id, LscAttributes pivotAttrs, boolean fromSource) throws NamingException {
		String searchString = null;
		if(fromSource || filterIdClean == null) {
			searchString = filterIdSync;
		} else {
			searchString = filterIdClean; 
		}

		if (pivotAttrs != null && pivotAttrs.getAttributes() != null && pivotAttrs.getAttributes().size() > 0) {
			for (String attributeName : pivotAttrs.getAttributesNames()) {
				String valueId = pivotAttrs.getStringValueAttribute(attributeName.toLowerCase());
				searchString = Pattern.compile("\\{" + attributeName + "\\}", Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(valueId);
			}
		} else if (attrsId.size() == 1) {
			searchString = Pattern.compile("\\{" + attrsId.get(0) + "\\}", Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(id);
		} else {
			// this is kept for backwards compatibility but will be removed
			searchString = filterIdSync.replaceAll("\\{0\\}", id);
		}

		return getJndiServices().getEntry(baseDn, searchString, _filteredSc);
	}

	/**
	 * LDAP Services getter to fit to the context - source or destination.
	 * 
	 * @return the JndiServices object used to apply directory operations
	 */
	public final JndiServices getJndiServices() {
		return jndiServices;
	}

	/**
	 * Default attrId getter.
	 * 
	 * @return the attrId value
	 */
	public final List<String> getAttrsId() {
		return attrsId;
	}

	/**
	 * Default attributes getter.
	 * 
	 * @return the attrs array
	 */
	public final List<String> getAttrs() {
		return attrs;
	}

	/**
	 * Default base distinguish name getter.
	 * 
	 * @return the baseDn value
	 */
	public final String getBaseDn() {
		return baseDn;
	}

	/**
	 * Default filter getter, for all corresponding entries.
	 * 
	 * @return the filterAll value
	 */
	public final String getFilterAll() {
		return filterAll;
	}

	/**
	 * Default filter getter, for one corresponding entry.
	 * 
	 * @return the attrId value
	 */
	public final String getFilterId() {
		return filterIdSync;
	}

	/*
	 * Default filter getter, for one corresponding entry.
	 * @return the attrId value
	 */
	public final String getFilterIdClean() {
		return filterIdClean;
	}
}
