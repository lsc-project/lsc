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
package org.lsc.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.LscAttributes;
import org.lsc.beans.AbstractBean;
import org.lsc.persistence.DaoConfig;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Generic JDBC iBatis Service
 * Manage retrieving of list and object according t
 * Can be override by a specific implementation in the final class if needed :
 * Get a look at org.lsc.service.StructureJdbcService class
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
public abstract class AbstractJdbcService implements ISrcService {

	protected static Logger LOGGER = LoggerFactory.getLogger(AbstractJdbcService.class);
	protected SqlMapClient sqlMapper;

	public abstract String getRequestNameForList();

	public abstract String getRequestNameForObject();
	private DataSchemaProvider cb;

	public AbstractJdbcService() {
		sqlMapper = DaoConfig.getSqlMapClient();
	}

	public void setCallback(DataSchemaProvider cb) {
		this.cb = cb;
	}

	public AbstractBean getBean(AbstractBean nonUsed, Entry<String, LscAttributes> ids) throws NamingException {
		String id = ids.getKey();
		Map<String, Object> attributeMap = ids.getValue().getAttributes();
		try {
			Object o = sqlMapper.queryForObject(getRequestNameForObject(), attributeMap);
			return (AbstractBean) o;
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for a specific entry with id={} ({})", id, e);
			LOGGER.debug(e.toString(), e);
			// TODO This SQLException may mean we lost the connection to the DB
			// This is a dirty hack to make sure we stop everything, and don't risk deleting everything...
			throw new CommunicationException(e.getMessage());
		}
	}

	/**
	 * Execute a database request to get a list of object identifiers. This request
	 * must be a very simple and efficient request because it will get all the requested
	 * identifiers.
	 * @return Map of DNs of all entries that are returned by the directory with an associated map of attribute names and values (never null)
	 */
	@SuppressWarnings("unchecked")
	public Map<String, LscAttributes> getListPivots() {
		/* TODO: This is a bit of a hack - we use ListOrderedMap to keep order of the list returned,
		 * since it may be important when coming from a database.
		 * This is really an API bug, getListPivots() should return a List, not a Map.
		 */
		Map<String, LscAttributes> ret = new ListOrderedMap();

		try {
			List<HashMap<String, Object>> ids = (List<HashMap<String, Object>>) sqlMapper.queryForList(getRequestNameForList(), null);
			Iterator<HashMap<String, Object>> idsIter = ids.iterator();
			String key;
			HashMap<String, Object> idMap;
			LscAttributes la;
			
			for (int count = 1; idsIter.hasNext(); count++) {
				idMap = idsIter.next();

				// the key of the result Map is usually the DN
				// since we don't have a DN from a database, we use a concatenation of:
				//     - all pivot attributes
				//     - a count of all objects (to make sure the key is unique)
				// unless there's only one pivot, to be backwards compatible
				if (idMap.values().size() == 1) {
					key = idMap.values().iterator().next().toString();
				}
				else {
					key = StringUtils.join(idMap.values().iterator(), ", ") + " (" + count + ")";
				}
				la = new LscAttributes(idMap);
				ret.put(key, la);
			}
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for the entries list: {}", e.toString());
			LOGGER.debug(e.toString(), e);
		}

		return ret;
	}
}
