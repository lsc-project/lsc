/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008, LSC Project 
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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jndi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.collections.map.ListOrderedMap;
import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.utils.StringLengthComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a generic but configurable implementation to read data from the destination directory.
 * 
 * You can specify where (baseDn) and what (filterId & attr) information will be read on which type of entries
 * (filterAll and attrId).
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class FullDNJndiDstService extends AbstractSimpleJndiService implements IJndiWritableService {

	/**
	 * Preceding the object feeding, it will be instantiated from this class.
	 */
	private Class<IBean> beanClass;
	private static final Logger LOGGER = LoggerFactory.getLogger(FullDNJndiDstService.class);

	/**
	 * Constructor adapted to the context properties and the bean class name to instantiate.
	 *
	 * @param props
	 *            the properties used to identify the directory parameters and context
	 * @param beanClassName
	 *            the bean class name that will be instantiated and feed up
	 */
	@SuppressWarnings("unchecked")
	public FullDNJndiDstService(final Properties props, final String beanClassName) {
		super(props);
		try {
			this.beanClass = (Class<IBean>) Class.forName(beanClassName);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Bean class {} not found. Check this class name really exists.", beanClassName);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Destination LDAP Services getter.
	 *
	 * @return the Destination JndiServices object used to apply directory operations
	 */
	public final JndiServices getJndiServices() {
		return JndiServices.getDstInstance();
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param dn DN of the entry to be returned, which is the name returned by {@link #getListPivots()}
	 * @param pivotAttributes Unused.
	 * @return The bean, or null if not found
	 * @throws NamingException May throw a {@link NamingException} if the object is not found in the
	 *             directory, or if more than one object would be returned.
	 */
	public IBean getBean(String dn, LscAttributes pivotAttributes) throws NamingException {

		try {
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);
			List<String> attrs = getAttrs();
			if (attrs != null) {
				sc.setReturningAttributes(attrs.toArray(new String[attrs.size()]));
			}
			SearchResult srObject = getJndiServices().readEntry(dn, getFilterId(), true, sc);
			Method method = beanClass.getMethod("getInstance", new Class[]{SearchResult.class, String.class,
								Class.class});
			return (IBean) method.invoke(null, new Object[]{srObject, dn, beanClass});
			
		} catch (SecurityException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (InvocationTargetException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		}
		return null;
	}

    /**
     * Returns a list of all the objects' identifiers.
     * 
     * @return	Map of all entries DNs (this is not for display only!)
     * 			that are returned by the directory with an associated map of attribute names and values (never null)
     * @throws NamingException 
     */
	@SuppressWarnings("unchecked")
	public Map<String, LscAttributes> getListPivots() throws NamingException {
		// get list of DNs
		List<String> idList = JndiServices.getDstInstance().getDnList(getBaseDn(), getFilterAll(), SearchControls.SUBTREE_SCOPE);

		// sort the list by shortest first - this makes sure clean operations delete leaf elements first
		try {
			Collections.sort(idList, new StringLengthComparator());
		} catch (ClassCastException e) {
			// ignore errors, just leave list unsorted
		} catch (UnsupportedOperationException e) {
			// ignore errors, just leave list unsorted
		}

		// add DN suffix to obtain full DN
		String contextDN = getJndiServices().getContextDn();
		for (int i = 0; i < idList.size(); i++) {
			String id = idList.get(i);
			if (!id.endsWith("," + contextDN)) {
				idList.set(i, idList.get(i) + "," + contextDN);
			}
		}

		// convert to correct return format

		/* TODO: This is a bit of a hack - we use ListOrderedMap to keep order of the list returned,
		 * since it may be important when cleaning by full DN (for different levels).
		 * This is really an API bug, getListPivots() should return a List, not a Map.
		 */
		Map<String, LscAttributes> ids = new ListOrderedMap();

		for (String dn : idList) {
			LscAttributes attrs = new LscAttributes();
			attrs.put("dn", dn);
			ids.put(dn, attrs);
		}

		return ids;
	}

	/**
	 * Apply directory modifications.
	 *
	 * @param jm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status
	 * @throws CommunicationException If the connection to the service is lost,
	 * and all other attempts to use this service should fail.
	 */
	public boolean apply(JndiModifications jm) throws CommunicationException {
		return JndiServices.getDstInstance().apply(jm);
	}
}
