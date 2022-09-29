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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.LdapServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IWritableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a generic but configurable implementation to read data from the destination directory.
 * 
 * You can specify where (baseDn) and what (filterId &amp; attr) information will be read on which type of entries
 * (filterAll and attrId).
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SimpleJndiDstService extends AbstractSimpleJndiService implements IWritableService {

	/**
	 * Preceding the object feeding, it will be instantiated from this class.
	 */
	private Class<IBean> beanClass;

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJndiDstService.class);
	
    private List<String> writableDatasetIds;
	
	/**
	 * Constructor adapted to the context properties and the bean class name to instantiate.
	 * 
	 * @param task Initialized task containing all necessary pieces of information to initiate connection
	 * 				and load settings 
	 * @throws LscServiceException 
	 */
	@SuppressWarnings({ "unchecked" })
	public SimpleJndiDstService(final TaskType task) throws LscServiceConfigurationException {
		super(task.getLdapDestinationService());
        writableDatasetIds = task.getLdapDestinationService().getFetchedAttributes().getString();
		try {
			this.beanClass = (Class<IBean>) Class.forName(task.getBean());
		} catch (ClassNotFoundException e) {
			LOGGER.error("Bean class {} not found. Check this class name really exists.", task.getBean());
			throw new LscServiceConfigurationException(e);
		}
	}

    /**
     * @param ldapService 
     * @param writableDatasetIds
     * @param beanClass
     * @throws LscServiceException 
     */
    public SimpleJndiDstService(final LdapServiceType ldapService, List<String> writableDatasetIds, Class<IBean> beanClass) throws LscServiceConfigurationException {
        super(ldapService);
        this.writableDatasetIds = writableDatasetIds; 
        this.beanClass = beanClass;
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
	public final IBean getBean(String pivotName, LscDatasets pivotAttributes, boolean fromSameService) throws LscServiceException {
		try {
			SearchResult srObject = get(pivotName, pivotAttributes, filterIdSync);
			Method method = beanClass.getMethod("getInstance", 
							new Class[] { SearchResult.class, String.class, Class.class });
			return (IBean) method.invoke(null, new Object[] { srObject, jndiServices.completeDn(getBaseDn()), beanClass });
		} catch (SecurityException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (NoSuchMethodException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (InvocationTargetException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
							beanClass.getName(), e.toString());
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
	 * @throws LscServiceException May throw a {@link NamingException} if an error occurs while
	 *             searching the directory.
	 */
	public Map<String, LscDatasets> getListPivots() throws LscServiceException  {
        try {
			return jndiServices.getAttrsList(getBaseDn(), getFilterAll(), SearchControls.SUBTREE_SCOPE,
			        getAttrsId());
		} catch (NamingException e) {
			throw new LscServiceException(e);
		}
    }

	/**
	 * Apply directory modifications.
	 *
	 * @param jm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status
	 * @throws CommunicationException If the connection to the service is lost,
	 * and all other attempts to use this service should fail.
	 */
	public boolean apply(JndiModifications jm) throws LscServiceCommunicationException {
		try {
			return jndiServices.apply(jm);
		} catch (CommunicationException e) {
			throw new LscServiceCommunicationException(e);
		}
	}

	/**
	 * Apply directory modifications.
	 *
	 * @param lm Modifications to apply in a {@link JndiModifications} object.
	 * @return Operation status
	 * @throws CommunicationException If the connection to the service is lost,
	 * and all other attempts to use this service should fail.
	 */
	public boolean apply(LscModifications lm) throws LscServiceException {
		JndiModifications jm = new JndiModifications(JndiModificationType.getFromLscModificationType(lm.getOperation()), lm.getTaskName());
		jm.setDistinguishName(lm.getMainIdentifier());
		jm.setNewDistinguishName(lm.getNewMainIdentifier());
		jm.setModificationItems(JndiModifications.fromLscAttributeModifications(lm.getLscAttributeModifications()));
		try {
			return jndiServices.apply(jm);
		} catch (CommunicationException e) {
			throw new LscServiceException(e);
		}
	}
	
	public List<String> getWriteDatasetIds() {
		return writableDatasetIds;
	}
}
