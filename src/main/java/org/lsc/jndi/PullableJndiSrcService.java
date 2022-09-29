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
 ****************************************************************************
 */
package org.lsc.jndi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.lsc.LscDatasets;
import org.lsc.configuration.LdapSourceServiceType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IAsynchronousService;

/**
 * This directory service class is handling either full 
 * synchronization, or partial updates.
 *  
 * TODO: Manage an history of updates to avoid resynchronizing two
 * 		 or three times partial identical updates
 * TODO: Handling of timezone
 * 
 * @author S. Bahloul &lt;seb@lsc-project.org&gt;
 */
public class PullableJndiSrcService extends SimpleJndiSrcService implements
				IAsynchronousService {

	/** This field is storing the last successful synchronization date */
	private Date lastSuccessfulSync;
	/** */
	private Set<Entry<String, LscDatasets>> listPivots;
	/** The filter to use to get partial updates
	 * {0} will be replaced by the lastSuccessful synchronization date formatted according to dateFormat
	 */
	private String filterTimestamp;
	/** The date format to use with SimpleDateFormat to handle Date to String rewriting */
	private String dateFormat;
	/** The formater object */
	private SimpleDateFormat dateFormater;
	/** The interval in seconds */
	private int interval;

	@Deprecated
	public PullableJndiSrcService(Properties props, String beanClassName) throws LscServiceConfigurationException {
		super(props, beanClassName);
		// Default LDAP date filter
		filterTimestamp = props.getProperty("filterAsync", "modifytimestamp>={0}");
		// Default LDAP date format
		dateFormat = props.getProperty("dateFormat", "yyyyMMddHHmmss'Z'");
		// Default interval
		interval = Integer.parseInt(props.getProperty("interval", "5"));
		try {
			dateFormater = new SimpleDateFormat(dateFormat);
			dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
		} catch(IllegalArgumentException e) {
			throw new LscServiceConfigurationException(e);
		}
	}

	public PullableJndiSrcService(TaskType task) throws LscServiceConfigurationException {
		super(task);
		LdapSourceServiceType asyncService = (LdapSourceServiceType) LscConfiguration.getSourceService(task);
		// Default LDAP date filter
		filterTimestamp = (asyncService.getFilterAsync() != null ? asyncService.getFilterAsync() :  "modifytimestamp>={0}");
		// Default LDAP date format
		dateFormat = (asyncService.getDateFormat() != null ? asyncService.getDateFormat() : "yyyyMMddHHmmss'Z'");
		// Default interval
		interval = (asyncService.getInterval() != null ? asyncService.getInterval().intValue() : 5);
		try {
			dateFormater = new SimpleDateFormat(dateFormat);
			dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
		} catch(IllegalArgumentException e) {
			throw new LscServiceConfigurationException(e);
		}
	}

	public Entry<String, LscDatasets> getNextId() throws LscServiceException {
		if (listPivots != null && !listPivots.isEmpty()) {
			Entry<String, LscDatasets> entry = listPivots.iterator().next();
			listPivots.remove(entry);
			if (listPivots.isEmpty()) {
				listPivots = null;
			}
			return entry;
		} else if (lastSuccessfulSync == null) {
			// The fist time, we must resynchronize everything !
			lastSuccessfulSync = new Date();
			listPivots = this.getListPivots().entrySet();
		} else {
			String date = dateFormater.format(lastSuccessfulSync);

			// Reset the last successful synchronization date
			// Must be done now to avoid loosing entries modification while synchronizing
			// We may replay some synchronization two times but there's now simple way to avoid it
			lastSuccessfulSync = new Date();

			try {
				listPivots = jndiServices.getAttrsList(getBaseDn(),
								filterTimestamp.replaceAll("\\{0\\}", date), SearchControls.SUBTREE_SCOPE,
								getAttrsId()).entrySet();
			} catch (NamingException e) {
				throw new LscServiceException(e);
			}
			if (listPivots.isEmpty()) {
				return null;
			}
		}
		return getNextId();
	}

	/**
	 * The interval accessor
	 */
	@Override
	public long getInterval() {
		return interval*1000;
	}
}
