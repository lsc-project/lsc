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
package org.lsc.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.ServiceType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service allows a multiple referential commit two-phase
 * Get object and list object ids calls are done on the first service
 * Updates are done on every one through a XA transaction
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class MultipleDstService implements IWritableService {

	protected static final Logger LOGGER = LoggerFactory.getLogger(MultipleDstService.class);

	private List<IXAWritableService> xaServices;
	
	@SuppressWarnings("unchecked")
	public MultipleDstService(TaskType task) throws LscServiceConfigurationException {
		xaServices = new ArrayList<IXAWritableService>();
		try {
			for(Object service: task.getMultiDestinationService().getXaServices().getReference()){
				if(service instanceof ServiceType) {
					ServiceType wrService = (ServiceType)service;
					Constructor<IXAWritableService> xaServiceConstructor = (Constructor<IXAWritableService>) LscConfiguration.getServiceImplementation(wrService).getConstructor(TaskType.class);
					IXAWritableService xaService = xaServiceConstructor.newInstance(task);
					xaServices.add(xaService);
				} else {
					LOGGER.error("Unknown referenced service: " + service.toString());
				}
			}
		} catch(InstantiationException e) {
			throw new LscServiceConfigurationException(e);
		} catch (SecurityException e) {
			throw new LscServiceConfigurationException(e);
		} catch (NoSuchMethodException e) {
			throw new LscServiceConfigurationException(e);
		} catch (IllegalArgumentException e) {
			throw new LscServiceConfigurationException(e);
		} catch (IllegalAccessException e) {
			throw new LscServiceConfigurationException(e);
		} catch (InvocationTargetException e) {
			throw new LscServiceConfigurationException(e);
		}
	}
	
	@Override
	public IBean getBean(String pivotName, LscDatasets pivotAttributes,
			boolean fromSameService) throws LscServiceException {
		// use the first service getBean result
		return xaServices.get(0).getBean(pivotName, pivotAttributes, fromSameService);
	}

	@Override
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		// use the first service getBean result
		return xaServices.get(0).getListPivots();
	}

	@Override
	public boolean apply(LscModifications lm) throws LscServiceException {
		Map<String, String> transactionIds = new HashMap<String, String>();
		try {
			boolean doNotCommit = false;
			for(IXAWritableService iws : xaServices) {
				String transactionId = iws.start();
				transactionIds.put(iws.getId(), transactionId);
			}
			for(IXAWritableService iws : xaServices) {
				iws.submit(transactionIds.get(iws.getId()), lm);
			}
			for(IXAWritableService iws : xaServices) {
				iws.end(transactionIds.get(iws.getId()));
			}
			for(IXAWritableService iws : xaServices) {
				int retCode = iws.prepare(transactionIds.get(iws.getId()));
				if (retCode != XAResource.XA_OK && retCode != XAResource.XA_RDONLY) {
			           doNotCommit = true;
				}
			}
			if(doNotCommit) {
				return false;
			}
			for(IXAWritableService iws : xaServices) {
				iws.commit(transactionIds.get(iws.getId()));
			}
		} catch ( LscServiceException lse ) {
			for(IXAWritableService iws : xaServices) {
				if(transactionIds.get(iws.getId()) != null) {
					iws.rollback(transactionIds.get(iws.getId()));
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public List<String> getWriteDatasetIds() {
		List<String> writableDatasetIds = new ArrayList<String>();
		for(IXAWritableService xaService: xaServices) {
			writableDatasetIds.addAll(xaService.getWriteDatasetIds());
		}
		return writableDatasetIds;
	}

    /**
     * @see org.lsc.service.IService.getSupportedConnectionType()
     */
    public Collection<Class<? extends ConnectionType>> getSupportedConnectionType() {
        Collection<Class<? extends ConnectionType>> list = new ArrayList<Class<? extends ConnectionType>>();
        return list;
    }
}
