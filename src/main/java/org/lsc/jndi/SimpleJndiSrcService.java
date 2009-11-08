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

import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.LscAttributes;
import org.lsc.beans.AbstractBean;
import org.lsc.service.ISrcService;


/**
 * This class is a generic but configurable implementation to get data
 * from the directory.  You can specify where (baseDn) and what (filterId &
 * attr) information will be read on which type of entries (filterAll and
 * attrId). TODO implements JUnit test
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SimpleJndiSrcService extends AbstractSimpleJndiService implements ISrcService {

	/**
	 * Constructor adapted to the context properties and the bean class name
	 * to instantiate.
	 * 
	 * @param props the properties used to identify the directory parameters
	 * and context
	 */
	public SimpleJndiSrcService(final Properties props) {
		super(props);
	}

	/**
	 * The simple object getter according to its identifier.
	 *
	 * @param ids the data identifier in the directory - must return a unique
	 *        directory entry
	 *
	 * @return the corresponding top derivated object or null if failed
	 *
	 * @throws NamingException thrown if an directory exception is encountered
	 *         while getting the identified bean
	 */
	public final AbstractBean getBean(AbstractBean bean, final Entry<String, LscAttributes> ids) throws NamingException {
		return this.getBeanFromSR(get(ids), bean);
	}

	/**
	 * Source LDAP Services getter.
	 *
	 * @return the Source JndiServices object used to apply directory
	 *         operations
	 */
	public final JndiServices getJndiServices() {
		return JndiServices.getSrcInstance();
	}

	/**
	 * Get the identifiers list.
	 * 
	 * @return Map of DNs of all entries that are returned by the directory with an associated map of attribute names and values, never null
	 * @throws NamingException
	 *                 thrown if an directory exception is encountered while
	 *                 getting the identifiers list
	 */
    public Map<String, LscAttributes> getListPivots() throws NamingException {
        return JndiServices.getSrcInstance().getAttrsList(getBaseDn(), 
                getFilterAll(), SearchControls.SUBTREE_SCOPE, 
                getAttrsId());
    }
}
