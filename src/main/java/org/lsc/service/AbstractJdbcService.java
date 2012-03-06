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
package org.lsc.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.naming.CommunicationException;
import javax.naming.directory.BasicAttribute;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.DatabaseConnectionType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.persistence.DaoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Generic JDBC iBatis Service
 * Manage retrieving of list and object according t
 * Can be override by a specific implementation in the final class if needed :
 * Get a look at org.lsc.service.StructureJdbcService class
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
public abstract class AbstractJdbcService implements IService {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractJdbcService.class);
	protected SqlMapClient sqlMapper;
	
	private Class<IBean> beanClass;

	public abstract String getRequestNameForList();

	public abstract String getRequestNameForObject();

	public abstract String getRequestNameForNextId();

    public abstract String getRequestNameForClean();

	@Deprecated
	public AbstractJdbcService(Properties databaseProps) throws LscServiceConfigurationException {
		sqlMapper = DaoConfig.getSqlMapClient(databaseProps);
	}

	@SuppressWarnings("unchecked")
	public AbstractJdbcService(DatabaseConnectionType destinationConnection, String beanClassname) throws LscServiceConfigurationException {
		sqlMapper = DaoConfig.getSqlMapClient(destinationConnection);
		
		try {
			this.beanClass = (Class<IBean>) Class.forName(beanClassname);
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
	 * @return The bean, or null if not found
	 * @throws LscServiceException May throw a embedded {@link CommunicationException} if an SQLException is encountered 
	 */
	public IBean getBean(String pivotName, LscDatasets pivotAttributes) throws LscServiceException {
		Map<String, Object> attributeMap = pivotAttributes.getDatasets();
		try {
			return (IBean) sqlMapper.queryForObject(getRequestNameForObject(), attributeMap);
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for a specific entry with id={} ({})", pivotName, e);
			LOGGER.debug(e.toString(), e);
			// TODO This SQLException may mean we lost the connection to the DB
			// This is a dirty hack to make sure we stop everything, and don't risk deleting everything...
			throw new LscServiceException(new CommunicationException(e.getMessage()));
		}
	}

	/**
	 * Execute a database request to get a list of object identifiers. This request
	 * must be a very simple and efficient request because it will get all the requested
	 * identifiers.
	 * @return Map of all entries names that are returned by the directory with an associated map of
	 *         attribute names and values (never null)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, LscDatasets> getListPivots() {
		/* TODO: This is a bit of a hack - we use ListOrderedMap to keep order of the list returned,
		 * since it may be important when coming from a database.
		 * This is really an API bug, getListPivots() should return a List, not a Map.
		 */
		Map<String, LscDatasets> ret = new ListOrderedMap();

		try {
			List<HashMap<String, Object>> ids = (List<HashMap<String, Object>>) sqlMapper.queryForList(getRequestNameForList());
			Iterator<HashMap<String, Object>> idsIter = ids.iterator();
			Map<String, Object> idMap;
			
			for (int count = 1; idsIter.hasNext(); count++) {
				idMap = idsIter.next();
				ret.put(getMapKey(idMap, count), new LscDatasets(idMap));
			}
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for the entries list: {}", e.toString());
			LOGGER.debug(e.toString(), e);
		}

		return ret;
	}
	
	protected String getMapKey(Map<String, Object> idMap, int count) {

		String key;
		// the key of the result Map is usually the DN
		// since we don't have a DN from a database, we use a concatenation of:
		//     - all pivot attributes
		//     - a count of all objects (to make sure the key is unique)
		// unless there's only one pivot, to be backwards compatible
		if (idMap.values().size() == 1) {
			key = idMap.values().iterator().next().toString();
		} else {
			key = StringUtils.join(idMap.values().iterator(), ", ") + " (" + count + ")";
		}
		return key;
	}
	
	/**
	 * Override default AbstractJdbcSrcService to get a SimpleBean
	 * TODO 1.3 Move this to AbstractJdbcSrcService and replace return type with a simple Map 
	 * @throws LscServiceException 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public IBean getBean(String id, LscDatasets attributes, boolean fromSource) throws LscServiceException {
		IBean srcBean = null;
		try {
			srcBean = beanClass.newInstance();
            List<?> records = sqlMapper.queryForList((fromSource ? getRequestNameForObject() : getRequestNameForClean()), getAttributesMap(attributes));
			if(records.size() > 1) {
				throw new LscServiceException("Only a single record can be returned from a getObject request ! " +
						"For id=" + id + ", there are " + records.size() + " records !");
			} else if (records.size() == 0) {
				return null;
			}
			Map<String, Object> record = (Map<String, Object>) records.get(0);
			for(Entry<String, Object> entry: record.entrySet()) {
				if(entry.getValue() != null) {
					srcBean.setAttribute(new BasicAttribute(entry.getKey(), entry.getValue()));
				} else {
					srcBean.setAttribute(new BasicAttribute(entry.getKey()));
				}
			}
			srcBean.setMainIdentifier(id);
			return srcBean;
		} catch (InstantiationException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
					beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Unable to get static method getInstance on {} ! This is probably a programmer's error ({})",
					beanClass.getName(), e.toString());
			LOGGER.debug(e.toString(), e);
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for a specific entry with id={} ({})", id, e);
			LOGGER.debug(e.toString(), e);
			// TODO This SQLException may mean we lost the connection to the DB
			// This is a dirty hack to make sure we stop everything, and don't risk deleting everything...
			throw new LscServiceException(new CommunicationException(e.getMessage()));
		}
		return null;
	}


	public static Map<String, Object> fillAttributesMap(
			Map<String, Object> datasets, IBean destinationBean) {
		for(String attributeName : destinationBean.getAttributesNames()) {
			if(!datasets.containsKey(attributeName)) {
				if(destinationBean.getDatasetById(attributeName) != null && destinationBean.getDatasetById(attributeName).size() > 0) {
					datasets.put(attributeName, destinationBean.getDatasetById(attributeName).iterator().next().toString());
				}
			}
		}
		return datasets;
	}

	public static Map<String, Object> getAttributesMap(
			List<LscDatasetModification> lscAttributeModifications) {
		Map<String, Object> values = new HashMap<String, Object>();
		for(LscDatasetModification lam : lscAttributeModifications) {
			if(lam.getValues().size() > 0) {
				values.put(lam.getAttributeName(), lam.getValues().get(0));
			}
		}
		return values;
	}
	
	public static Map<String, String> getAttributesMap(
			LscDatasets lscAttributes) {
		Map<String, String> values = new HashMap<String, String>(lscAttributes.getDatasets().size());
		for(Entry<String, Object> entry : lscAttributes.getDatasets().entrySet()) {
			if(entry.getValue() != null) {
				values.put(entry.getKey(), getValue(entry.getValue()));
			}
		}
		return values;
	}
	
	public static String getValue(Object value) {
		if(value instanceof List) {
			return ((List<?>)value).iterator().next().toString();
		} else if(value instanceof Set) {
			return ((Set<?>)value).iterator().next().toString();
		} else {
			return value.toString();
		}
	}
}
