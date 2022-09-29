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

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.LdapSourceServiceType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a generic but configurable implementation to get data
 * from the directory.  You can specify where (baseDn) and what (filterId &amp;
 * attr) information will be read on which type of entries (filterAll and
 * attrId). TODO implements JUnit test
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SimpleJndiSrcService extends AbstractSimpleJndiService implements IService {

	protected static final Logger LOGGER = LoggerFactory.getLogger(SimpleJndiSrcService.class);
	/**
	 * Preceding the object feeding, it will be instantiated from this class.
	 */
	protected Class<IBean> beanClass;
	
	/**
	 * The filter to be completed by replacing {0} by the id to find a unique
	 * entry. Use with destination attributes while getting the object to check for suppression
	 */
	protected String filterIdClean;
	/**
	 * Constructor adapted to the context properties and the bean class name
	 * to instantiate.
	 * 
	 * @param props the properties used to identify the directory parameters
	 * and context
	 * @throws LscServiceConfigurationException 
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public SimpleJndiSrcService(final Properties props, final String beanClassName) throws LscServiceConfigurationException {
		super(props);
		filterIdClean = props.getProperty("filterIdClean");
		try {
			this.beanClass = (Class<IBean>) Class.forName(beanClassName);
		} catch (ClassNotFoundException e) {
			throw new LscServiceConfigurationException(e);
		}
	}

	/**
	 * Constructor adapted to the context properties and the bean class name
	 * to instantiate.
	 * 
	 * @param task Initialized task containing all necessary pieces of information to initiate connection
	 * 				and load settings 
	 * @throws LscServiceConfigurationException 
	 */
	@SuppressWarnings("unchecked")
	public SimpleJndiSrcService(final TaskType task) throws LscServiceConfigurationException {
		super((LdapSourceServiceType)LscConfiguration.getSourceService(task));
		filterIdClean = ((LdapSourceServiceType)LscConfiguration.getSourceService(task)).getCleanFilter();
		if(filterIdClean == null) {
		    LOGGER.warn("No clean filter has been specified for task=" + task.getName() + ". During the clean phase, LSC wouldn't be able to get the right entries and may delete all destination entries !");
		}
		try {
			this.beanClass = (Class<IBean>) Class.forName(task.getBean());
		} catch (ClassNotFoundException e) {
			throw new LscServiceConfigurationException(e);
		}
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param pivotName Name of the entry to be returned, which is the name returned by
	 *            {@link #getListPivots()} (used for display only)
	 * @param pivotAttributes Map of attribute names and values, which is the data identifier in the
	 *            source such as returned by {@link #getListPivots()}. It must identify a unique
	 *            entry in the source.
	 * @param fromSameService are the pivot attributes provided by the same service
	 * @return The bean, or null if not found
	 * @throws LscServiceException May throw a {@link NamingException} if the object is not found in the
	 *             directory, or if more than one object would be returned.
	 */
	public IBean getBean(final String pivotName, final LscDatasets pivotAttributes, boolean fromSameService) throws LscServiceException {
		IBean srcBean;
		try {
			srcBean = this.beanClass.newInstance();
			String searchString = null;
			if(fromSameService || filterIdClean == null) {
				searchString = filterIdSync;
			} else {
				searchString = filterIdClean; 
			}

			return this.getBeanFromSR(get(pivotName, pivotAttributes, searchString), srcBean);
		} catch (InstantiationException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (NamingException e) {
			throw new LscServiceException(e);
		}
		return null;
	}

	/**
	 * Returns a list of all the objects' identifiers.
	 * 
	 * @return Map of all entries names that are returned by the directory with an associated map of
	 *         attribute names and values (never null)
	 * @throws LscServiceException 
	 * @throws NamingException May throw a {@link NamingException} if an error occurs while
	 *             searching the directory.
	 */
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		try {
			return jndiServices.getAttrsList(getBaseDn(),
							getFilterAll(), SearchControls.SUBTREE_SCOPE,
							getAttrsId());
		} catch (NamingException e) {
			throw new LscServiceException(e);
		}
	}

	/*
	 * Default filter getter, for one corresponding entry.
	 * @return the attrId value
	 */
	public final String getFilterIdClean() {
		return filterIdClean;
	}
}
