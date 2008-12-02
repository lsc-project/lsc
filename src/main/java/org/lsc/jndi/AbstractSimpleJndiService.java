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

import java.util.Properties;
import java.util.Set;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.Configuration;

/**
 * This class is an abstract generic but configurable implementation to get data
 * from the directory.
 * 
 * You can specify where (baseDn) and what (filterId & attr) information will be
 * read on which type of entries (filterAll and attrId).
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @deprecated Merge with AbstractExtendedJndiService
 */
public abstract class AbstractSimpleJndiService extends AbstractJndiSrcService {

	/**
	 * The filter to be completed by replacing {0} by the id to find a
	 * unique entry.
	 */
	private String filterId;

	/**
	 * The filter used to identify all the entries that have to be
	 * synchronized by this JndiSrcService.
	 */
	private String filterAll;

	/** Where to find the entries. */
	private String baseDn;

	/**
	 * When finding entries with 'filterAll' filter, the attribute to read
	 * to reuse it in 'filterId' filter.
	 */
	private String attrId;

	/**
	 * When a single entry is read in the directory, the attributes array to
	 * read - Used to limit at the source, the synchronization perimeter.
	 */
	private String[] attrs;

	/**
	 * The default initializer.
	 * 
	 * @param serviceProps The default simple JNDI properties
	 */
	public AbstractSimpleJndiService(final Properties serviceProps) {
		baseDn = serviceProps.getProperty("baseDn");
		filterId = serviceProps.getProperty("filterId");
		filterAll = serviceProps.getProperty("filterAll");
		String attrsValue = serviceProps.getProperty("attrs");
		if (attrsValue != null) {
			Set<String> values = Configuration.getSetFromString(attrsValue);
			attrs = new String[values.size()];
			attrs = values.toArray(attrs);
		}

		attrId = serviceProps.getProperty("attrId");
	}

	/**
	 * Get the ldap search result according the specified identifier.
	 * 
	 * @param id The object identifier - used in the directory filter as {0}
	 * @return The ldap search result
	 * @throws NamingException
	 *                 thrown if an directory exception is encountered while
	 *                 getting the identified object
	 */
	public final SearchResult get(final String id) throws NamingException {
		SearchControls sc = new SearchControls();
		sc.setReturningAttributes(attrs);
		return getJndiServices().getEntry(baseDn, filterId.replaceAll("\\{0\\}", id), sc);
	}

	/**
	 * LDAP Services getter to fit to the context - source or destination. 
	 * @return the JndiServices object used to apply directory operations
	 */
	public abstract JndiServices getJndiServices();

	/**
	 * Default attrId getter.
	 * @return the attrId value
	 */
	public final String getAttrId() {
		return attrId;
	}

	/**
	 * Default attributes getter.
	 * @return the attrs array
	 */
	public final String[] getAttrs() {
		return attrs;
	}

	/**
	 * Default base distinguish name getter.
	 * @return the baseDn value
	 */
	public final String getBaseDn() {
		return baseDn;
	}

	/**
	 * Default filter getter, for all corresponding entries.
	 * @return the filterAll value
	 */
	public final String getFilterAll() {
		return filterAll;
	}

	/**
	 * Default filter getter, for one corresponding entry.
	 * @return the attrId value
	 */
	public final String getFilterId() {
		return filterId;
	}
}
