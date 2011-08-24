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
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.configuration.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tapestry5.beaneditor.Validate;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Value;
import org.lsc.Configuration;
import org.lsc.configuration.PropertiesConfigurationHelper;
import org.lsc.configuration.objects.syncoptions.ForceSyncOptions;
import org.lsc.configuration.objects.syncoptions.PropertiesBasedSyncOptions;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class represent the parameter of a LSC task
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
@XStreamAlias("task")
public class Task {

	private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);

	@Inject @Value("ANewTask")
	@Validate("required")
	private String name;

	@Inject @Value("org.lsc.beans.SimpleBean")
	@Validate("required")
	private String bean;

	@XStreamAlias("destination")
	private Service destinationService;
	
	@XStreamAlias("source")
	private Service sourceService;

	private SyncOptions syncOptions;
	
	private String customLibrary;

	private String cleanHook;
	
	private String syncHook;
	
	@XStreamOmitField
	private Map<String, String> otherSettings;

	@XStreamImplicit
	private List<Audit> auditLogs;
	
	public Task() {
		otherSettings = new HashMap<String, String>();
	}

	@Deprecated
	public Task(String taskName, Properties lscProperties) {
		otherSettings = new HashMap<String, String>();
		this.name = taskName;
		String prefix = PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + ".";
	
		checkTaskOldProperty(lscProperties, taskName, PropertiesConfigurationHelper.OBJECT_PROPS_PREFIX, "Please take a look at upgrade notes at http://lsc-project.org/wiki/documentation/upgrade/1.1-1.2");
		bean = getTaskPropertyAndCheckNotNull(taskName, lscProperties, PropertiesConfigurationHelper.BEAN_PROPS_PREFIX);
		cleanHook = lscProperties.getProperty(prefix + PropertiesConfigurationHelper.POST_CLEAN_HOOK_PROPS_PREFIX);
		syncHook = lscProperties.getProperty(prefix + PropertiesConfigurationHelper.POST_SYNC_HOOK_PROPS_PREFIX);

		Connection sourceConn = null;
		if(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.service.SimpleJdbcSrcService")) {
			sourceConn = LscConfiguration.getConnection("src-jdbc");
		} else if (lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.jndi.SimpleJndiSrcService")
				|| lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.jndi.PullableJndiSrcService")) {
			sourceConn = LscConfiguration.getConnection("src-ldap");
		} else {
			// Unknown connection type !
		}
		Connection destinationConn = LscConfiguration.getConnection("dst-ldap");
		String syncOptionsType = lscProperties.getProperty(PropertiesConfigurationHelper.SYNCOPTIONS_PREFIX + "." + taskName, "org.lsc.beans.syncoptions.ForceSyncOptions");
		try {
			sourceService = (Service) sourceConn.getService(true).newInstance();
			sourceService.setName(taskName + "-src");
			Properties srcProps = Configuration.getPropertiesSubset(lscProperties, prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX);
			for(Object srcProp :  srcProps.keySet()) {
				sourceService.setOtherSetting((String)srcProp, srcProps.getProperty((String)srcProp));
			}
			sourceService.setConnection(sourceConn);
			
			destinationService = (Service) destinationConn.getService(false).newInstance();
			destinationService.setName(taskName + "-dst");
			Properties dstProps = Configuration.getPropertiesSubset(lscProperties, prefix + PropertiesConfigurationHelper.DSTSERVICE_PROPS_PREFIX);
			for(Object dstProp :  dstProps.keySet()) {
				destinationService.setOtherSetting((String)dstProp, dstProps.getProperty((String)dstProp));
			}
			destinationService.setConnection(destinationConn);

			if(syncOptionsType == null || 
					"org.lsc.beans.syncoptions.PropertiesBasedSyncOptions".equals(syncOptionsType)) {
				syncOptions = new PropertiesBasedSyncOptions();
			} else if ("org.lsc.beans.syncoptions.ForceSyncOptions".equals(syncOptionsType)) {
				syncOptions = new ForceSyncOptions();
			} else {
				// Unknown sync options type !
			}
			auditLogs = new ArrayList<Audit>();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		customLibrary = lscProperties.getProperty(prefix + PropertiesConfigurationHelper.CUSTOMLIBRARY_PROPS_PREFIX);
		syncOptions.setMainIdentifier(lscProperties.getProperty(prefix + "dn"));
		syncOptions.load(taskName, Configuration.getPropertiesSubset(lscProperties, PropertiesConfigurationHelper.SYNCOPTIONS_PREFIX + "." + taskName));
	}
	
	private void checkTaskOldProperty(Properties props, String taskName, String propertyName, String message) {
		if (props.getProperty(PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName) != null) {
			String errorMessage = "Deprecated value specified in task " + taskName + " for " + propertyName + "! Please read upgrade notes ! (" + message + ")";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}
	}

	private String getTaskPropertyAndCheckNotNull(String taskName, Properties props, String propertyName) {
		String value = props.getProperty(PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName);

		if (value == null) {
			String errorMessage = "No value specified in task " + taskName + " for " + propertyName + "! Aborting.";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}

		return value;
	}

	public String getBean() {
		return bean;
	}
	
	public String getName() {
		return name;
	}
	
	public Service getSourceService() {
		return sourceService;
	}
	
	public Service getDestinationService() {
		return destinationService;
	}
	
	public SyncOptions getSyncOptions() {
		return syncOptions;
	}
	
	public String getCustomLibrary() {
		return customLibrary;
	}

	public String getCleanHook() {
		return cleanHook;
	}
	
	public String getSyncHook() {
		return syncHook;
	}

	public void setSyncOptions(SyncOptions syncOptions) {
		this.syncOptions = syncOptions;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setObject(String name, Object obj) {
		
	}
	
	public void setOtherSetting(String name, String value) {
		if("bean".equals(name)) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("Setting bean to " + value);
			}
			this.bean = value;
		} else {
			otherSettings.put(name, value);
			//LOGGER.warn("Ignoring unknown parameter " + name + " / " + value);
		}
	}
	
	public String getOtherSetting(String key) {
		return getOtherSetting(key, null);
	}

	public String getOtherSetting(String key, String defaultValue) {
		return (otherSettings != null && otherSettings.containsKey(key) ? otherSettings.get(key) : defaultValue);
	}

	public void setBean(String bean) {
		this.bean = bean;
	}

	public void setDestinationService(Service destinationService) {
		this.destinationService = destinationService;
	}

	public void setSourceService(Service sourceService) {
		this.sourceService = sourceService;
	}

	public void setCustomLibrary(String customLibrary) {
		this.customLibrary = customLibrary;
	}

	public void setCleanHook(String cleanHook) {
		this.cleanHook = cleanHook;
	}

	public void setSyncHook(String syncHook) {
		this.syncHook = syncHook;
	}
	
	public Collection<Audit> getAudits() {
		if(auditLogs == null) {
			auditLogs = new ArrayList<Audit>();
		}
		return auditLogs;
	}
	
	public void setAudits(List<Audit> auditLogs) {
		this.auditLogs = auditLogs;
	}
	
	public void addAudit(Audit audit) {
		if(auditLogs == null) {
			auditLogs = new ArrayList<Audit>();
		}
		auditLogs.add(audit);
	}

	public void validate() throws LscException {
		sourceService.validate();
		destinationService.validate();
		syncOptions.validate();
		for(Audit audit : getAudits()) {
			audit.validate();
		}
		try {
			Class.forName(this.bean).newInstance();
		} catch (ClassNotFoundException e) {
			throw new LscConfigurationException("Unable to resolve bean class " + bean + " for task " + name, e);
		} catch (InstantiationException e) {
			throw new LscConfigurationException("Unable to instanciate bean class " + bean + " for task " + name, e);
		} catch (IllegalAccessException e) {
			throw new LscConfigurationException("Unable to instanciate bean class " + bean + " for task " + name, e);
		}

		// TODO check hooks
//		this.cleanHook
//		this.syncHook
	}
}
