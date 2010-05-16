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
package org.lsc.connectors.executable;

import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.jndi.SimpleJndiDstService;

/**
 * This class is a generic but configurable implementation to provision data to
 * any referential which can be scripted. This is based on ExecutableLdifService
 * for updating executables and SimpleJndiDstService to look for data 
 * 
 * It just requires 4 scripts to :
 * <ul>
 *   <li>add a new</li>  
 *   <li>update a existing data</li>  
 *   <li>rename - or change the identifier</li>  
 *   <li>delete or archive an unused data</li>  
 * </ul>
 * 
 * The 4 scripts which change data are responsible for consistency. No explicit 
 * check neither rollback is achieved by the LSC engine, so a successful result 
 * for any of these 4 operations must be fully checked.
 * 
 * At this time, no time out is managed. So please consider handling provisioned
 * referential availability and/or time limit handling directly in the executable.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiExecutableLdifService extends ExecutableLdifService {

	/** The destination JNDI service to use */
	private SimpleJndiDstService sjds;
	
	public JndiExecutableLdifService(Properties props, String beanClassName) {
		super(props, beanClassName);
		sjds = new SimpleJndiDstService(props, beanClassName);
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param pivotName Name of the entry to be returned, which is the name returned by {@link #getListPivots()}
	 *            (used for display only)
	 * @param pivotAttributes Map of attribute names and values, which is the data identifier in the
	 *            source such as returned by {@link #getListPivots()}. It must identify a unique entry in the
	 *            source.
	 * @return The bean, or null if not found
	 * @throws NamingException May throw a {@link NamingException} if the object is not found in the
	 *             directory, or if more than one object would be returned.
	 */
	public IBean getBean(String pivotName, LscAttributes pivotAttributes) throws NamingException {
		return sjds.getBean(pivotName, pivotAttributes);
	}

    /**
     * Returns a list of all the objects' identifiers.
     * 
	 * @return Map of all entries names that are returned by the directory with an associated map of
	 *         attribute names and values (never null)
     * @throws NamingException 
     */
	public Map<String, LscAttributes> getListPivots() throws NamingException {
		return sjds.getListPivots();
	}
}