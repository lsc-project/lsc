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
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.jndi;

import java.util.Map;
import java.util.Properties;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;

/**
 * This class is a generic implementation to simulate an empty destination directory.
 * 
 * @author Jonathan Clarke &lt;jonathan@lsc-project.org&gt;
 */
public class EmptyJndiDstService extends AbstractSimpleJndiService implements IJndiWritableService {

	/**
	 * Constructor adapted to the context properties and the bean class name to instantiate.
	 * 
	 * @param task the task parameter - unused
	 * @throws LscServiceConfigurationException 
	 */
	public EmptyJndiDstService(final TaskType task) throws LscServiceConfigurationException {
		super(task.getLdapDestinationService());
	}

	@Deprecated
	public EmptyJndiDstService(final Properties props, final String beanClassName) throws LscServiceConfigurationException {
		super(props);
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param pivotName Name of the entry to be returned, which is the name returned by {@link #getListPivots()}
	 *            (used for display only)
	 * @param pivotAttributes Map of attribute names and values, which is the data identifier in the
	 *            source such as returned by {@link #getListPivots()}. It must identify a unique entry in the
	 *            source.
	 * @param fromSameService are the pivot attributes provided by the same service
	 * @return Always returns null since this simulates an empty directory
	 * @throws NamingException Never thrown.
	 */
	public IBean getBean(String pivotName, LscDatasets pivotAttributes, boolean fromSameService) {
		return null;
	}

    /**
     * Returns a list of all the objects' identifiers.
     * 
     * @return Map Always null since this simulates an empty directory
     * @throws NamingException Never thrown.
     */
	public Map<String, LscDatasets> getListPivots() {
		return null;
	}

	/**
	 * Apply directory modifications. Always accept them in this Empty service.
	 *
	 * @param jm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status, always true.
	 * @throws CommunicationException Never thrown.
	 */
	public boolean apply(JndiModifications jm) {
		return true;
	}
}
