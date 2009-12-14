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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;

import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.beans.SimpleBean;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 *
 */
public class SimpleJdbcSrcService extends AbstractJdbcService
{

	private final String requestNameForList;
	private final String requestNameForObject;

	/**
	 * Simple JDBC source service that gets SQL request names from lsc.properties
	 * and calls the appropriate SQL requests defined in sql-map-config.d
	 * 
	 * @param props Configuration properties
	 */
	public SimpleJdbcSrcService(Properties props) {
		requestNameForList = props.getProperty("requestNameForList");
		requestNameForObject = props.getProperty("requestNameForObject");
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
	public IBean getBean(IBean nonUsed, Entry<String, LscAttributes> ids) throws NamingException {
		String id = ids.getKey();
		Map<String, Object> attributeMap = ids.getValue().getAttributes();
		try {
			List records = sqlMapper.queryForList(getRequestNameForObject(), attributeMap);
			if(records.size() > 1) {
				throw new RuntimeException("Only a single record can be returned from a getObject request ! " +
						"For id=" + attributeMap + ", there are " + records.size() + " records !");
			} else if (records.size() == 0) {
				return null;
			}
			SimpleBean sb = new SimpleBean();
			Map record = (Map) records.get(0);
			for(Object recordKey: record.keySet()) {
				sb.setAttribute(new BasicAttribute((String)recordKey, record.get(recordKey)));
			}
			return sb;
		} catch (SQLException e) {
			LOGGER.warn("Error while looking for a specific entry with id={} ({})", id, e);
			LOGGER.debug(e.toString(), e);
			// TODO This SQLException may mean we lost the connection to the DB
			// This is a dirty hack to make sure we stop everything, and don't risk deleting everything...
			throw new CommunicationException(e.getMessage());
		}
	}
}
