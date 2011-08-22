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
package org.lsc.configuration.objects.services;

import org.apache.tapestry5.beaneditor.Validate;
import org.lsc.configuration.objects.Service;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.persistence.DaoConfig;
import org.lsc.service.SimpleJdbcSrcService;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
@XStreamAlias("databaseSourceService")
public class Database extends Service {

	/** Contains the name of the SQL request to get an object */
	@Validate("required")
	protected String requestNameForObject;

	/** Contains the name of the SQL request to execute to list objects */
	@Validate("required")
	protected String requestNameForList;
	
	/** Contains the name of the SQL request to execute to get next id to synchronize */
	protected String requestNameForNextId;
	
	/** Interval between asynchronous synchronization. Default to -1 to inactivate */
	protected Integer interval; 
	
	@Override
	public Class<?> getImplementation() {
		return SimpleJdbcSrcService.class;
	}

	public String getRequestNameForObject() {
		return requestNameForObject;
	}

	public void setRequestNameForObject(String requestNameForObject) {
		this.requestNameForObject = requestNameForObject;
	}

	public String getRequestNameForList() {
		return requestNameForList;
	}

	public void setRequestNameForList(String requestNameForList) {
		this.requestNameForList = requestNameForList;
	}

	public String getRequestNameForNextId() {
		return requestNameForNextId;
	}

	public void setRequestNameForNextId(String requestNameForNextId) {
		this.requestNameForNextId = requestNameForNextId;
	}
	
	public int getInterval() {
		if(interval == null) {
			return -1;
		}
		return interval;
	}
	
	public void setInterval(int interval) {
		this.interval = interval;
	}

	@Override
	public void validate() throws LscServiceConfigurationException,
			LscServiceCommunicationException {
		try {
			// Need to check LDAP settings
			DaoConfig.getSqlMapClient((org.lsc.configuration.objects.connection.Database)this.getConnection());
		} catch (RuntimeException re) {
			if(re.getCause() instanceof LscServiceConfigurationException) {
				throw (LscServiceConfigurationException)re.getCause();
			} else if(re.getCause() instanceof LscServiceCommunicationException) {
				throw (LscServiceCommunicationException)re.getCause();
			} else {
				throw re;
			}
		}
	}
}
