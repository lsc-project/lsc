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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;

import org.lsc.LscAttributes;
import org.lsc.beans.IBean;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 *
 */
public class SimpleJdbcSrcService extends AbstractJdbcService implements IAsynchronousService {

	private final String requestNameForList;
	private final String requestNameForNextId;
	private final String requestNameForObject;
	
	private Class<IBean> beanClass;
	private int interval;

	/**
	 * Simple JDBC source service that gets SQL request names from lsc.properties
	 * and calls the appropriate SQL requests defined in sql-map-config.d
	 * 
	 * @param props Configuration properties
	 */
	@SuppressWarnings("unchecked")
	public SimpleJdbcSrcService(Properties props, String beanClassName) {
		requestNameForList = props.getProperty("requestNameForList");
		requestNameForObject = props.getProperty("requestNameForObject");
		requestNameForNextId = props.getProperty("requestNameForNextId");
		interval = Integer.parseInt(props.getProperty("interval"));
		try {
			this.beanClass = (Class<IBean>) Class.forName(beanClassName);
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.lsc.service.AbstractJdbcService#getRequestNameForList()
	 */
	@Override
	public String getRequestNameForList()
	{
		return requestNameForList;
	}

	/* (non-Javadoc)
	 * @see org.lsc.service.AbstractJdbcService#getRequestNameForObject()
	 */
	@Override
	public String getRequestNameForObject()
	{
		return requestNameForObject;
	}

	/**
	 * Override default AbstractJdbcSrcService to get a SimpleBean
	 * @TODO 1.3 Move this to AbstractJdbcSrcService and replace return type with a simple Map 
	 */
	@Override
	public IBean getBean(String id, LscAttributes attributes) throws NamingException {
		IBean srcBean = null;
		try {
			srcBean = beanClass.newInstance();
			Map<String, Object> attributeMap = attributes.getAttributes();
			List records = sqlMapper.queryForList(getRequestNameForObject(), attributeMap);
			if(records.size() > 1) {
				throw new RuntimeException("Only a single record can be returned from a getObject request ! " +
						"For id=" + attributeMap + ", there are " + records.size() + " records !");
			} else if (records.size() == 0) {
				return null;
			}
			Map record = (Map) records.get(0);
			for(Object recordKey: record.keySet()) {
				srcBean.setAttribute(new BasicAttribute((String)recordKey, record.get(recordKey)));
			}
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
			throw new CommunicationException(e.getMessage());
		}
		return null;
	}

	@Override
	public String getRequestNameForNextId() {
		return requestNameForNextId;
	}

	static int count = 0;

	@SuppressWarnings("unchecked")
	public Entry<String, LscAttributes> getNextId() {
		Map<String, Object> idMap;
		try {
			idMap = (Map<String, Object>) sqlMapper.queryForObject(getRequestNameForNextId());
			String key = getMapKey(idMap, count++);
			Map<String, LscAttributes> ret = new HashMap();
			ret.put(key, new LscAttributes(idMap));
			return ret.entrySet().iterator().next();
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for next entry ({})", e);
			LOGGER.debug(e.toString(), e);
		}
		
		return null;
	}
	
	public long getInterval() {
		return interval;
	}
}
