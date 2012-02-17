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
package org.lsc.configuration;

import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.configuration.ConfigurationException;
import org.lsc.Configuration;
import org.lsc.exception.LscConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties configuration loader
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class PropertiesConfigurationHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesConfigurationHelper.class);
	
	/** lsc prefix. */
	public static final String LSC_PROPS_PREFIX = "lsc";

	/** lsc.tasks property. */
	public static final String TASKS_PROPS_PREFIX = "tasks";

	/** lsc.syncoptions property. */
	public static final String SYNCOPTIONS_PREFIX = "syncoptions";

	/** lsc.tasks.TASKNAME.srcService property. */
	public static final String SRCSERVICE_PROPS_PREFIX = "srcService";

	/** lsc.tasks.TASKNAME.dstService property. */
	public static final String DSTSERVICE_PROPS_PREFIX = "dstService";

	/** lsc.tasks.TASKNAME.customLibrary property. */
	public static final String CUSTOMLIBRARY_PROPS_PREFIX = "customLibrary";

	/** lsc.tasks.TASKNAME.object property. */
	public static final String OBJECT_PROPS_PREFIX = "object";

	/** lsc.tasks.TASKNAME.bean property. */
	public static final String BEAN_PROPS_PREFIX = "bean";

	/** lsc.tasks.TASKNAME.bean property. */
	public static final String ASYNCHRONOUS_PROPS = "async";

	/** lsc.tasks.TASKNAME.postSyncHook property. */
	public static final String POST_SYNC_HOOK_PROPS_PREFIX = "postSyncHook";
	
	/** lsc.tasks.TASKNAME.postCleanHook property. */
	public static final String POST_CLEAN_HOOK_PROPS_PREFIX = "postCleanHook";

	/**
	 * Call by Configuration.setUp() if the new format configuration file is not available
	 * @param filename
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("deprecation")
	public static void loadConfigurationFrom(String filename) throws LscConfigurationException {
		Lsc lscInstance = new Lsc();
		Properties conf = org.lsc.Configuration.getAsProperties(filename, LSC_PROPS_PREFIX);
		
        // Get the "lsc.tasks" property
        String tasks = conf.getProperty(TASKS_PROPS_PREFIX);
        
        lscInstance.setConnections(new ConnectionsType());
        
        if(org.lsc.Configuration.getString("src.java.naming.provider.url") != null) {
        	LdapConnectionType ldapConn = new LdapConnectionType();
            ldapConn.setName("src-ldap");
            ldapConn.setUsername(org.lsc.Configuration.getString("src.java.naming.security.principal"));
            ldapConn.setPassword(org.lsc.Configuration.getString("src.java.naming.security.credentials"));
            if(ldapConn.getUsername() != null) {
            	ldapConn.setAuthentication(LdapAuthenticationType.SIMPLE);
            } else {
            	ldapConn.setAuthentication(LdapAuthenticationType.NONE);
            }
            ldapConn.setUrl(org.lsc.Configuration.getString("src.java.naming.provider.url"));
            lscInstance.getConnections().getLdapConnectionOrDatabaseConnectionOrNisConnection().add(ldapConn);
        }

        if(org.lsc.Configuration.getString("src.database.url") != null)  {
            DatabaseConnectionType jdbcConn = new DatabaseConnectionType();
            jdbcConn.setName("src-jdbc");
            jdbcConn.setUsername(org.lsc.Configuration.getString("src.database.username"));
            jdbcConn.setPassword(org.lsc.Configuration.getString("src.database.password"));
            jdbcConn.setUrl(org.lsc.Configuration.getString("src.database.url"));
            jdbcConn.setDriver(org.lsc.Configuration.getString("src.database.driver"));
            lscInstance.getConnections().getLdapConnectionOrDatabaseConnectionOrNisConnection().add(jdbcConn);
        }

        if(org.lsc.Configuration.getString("dst.java.naming.provider.url") != null) {
	        LdapConnectionType dstConn = new LdapConnectionType();
	        dstConn.setName("dst-ldap");
	        dstConn.setUsername(org.lsc.Configuration.getString("dst.java.naming.security.principal"));
	        dstConn.setPassword(org.lsc.Configuration.getString("dst.java.naming.security.credentials"));
	        if(dstConn.getUsername() != null) {
	        	dstConn.setAuthentication(LdapAuthenticationType.SIMPLE);
	        } else {
	        	dstConn.setAuthentication(LdapAuthenticationType.NONE);
	        }
	        dstConn.setUrl(org.lsc.Configuration.getString("dst.java.naming.provider.url"));
            lscInstance.getConnections().getLdapConnectionOrDatabaseConnectionOrNisConnection().add(dstConn);
        }
        
        lscInstance.setTasks(new TasksType());
        if (tasks != null) {
            // Iterate on each task
            StringTokenizer tasksSt = new StringTokenizer(tasks, ",");
    		while (tasksSt.hasMoreTokens()) {
    			String taskName = tasksSt.nextToken();
    			lscInstance.getTasks().getTask().add(newTask(taskName, conf));
    		}
        }
        SecurityType sec = new SecurityType();
        sec.setEncryption(new EncryptionType());
        if(new File(Configuration.getConfigurationDirectory(), "lsc.key").exists()) {
        	sec.getEncryption().setKeyfile(new File(Configuration.getConfigurationDirectory(), "lsc.key").getAbsolutePath());
        }
        lscInstance.setSecurity(sec);
        
        LscConfiguration.loadFromInstance(lscInstance);
	}

	private static TaskType newTask(String taskName, Properties lscProperties) throws LscConfigurationException {
		TaskType newTask = new TaskType();
		newTask.setName(taskName);

		String prefix = PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + ".";
	
		checkTaskOldProperty(lscProperties, taskName, PropertiesConfigurationHelper.OBJECT_PROPS_PREFIX, "Please take a look at upgrade notes at http://lsc-project.org/wiki/documentation/upgrade/1.1-1.2");
		newTask.setBean(getTaskPropertyAndCheckNotNull(taskName, lscProperties, PropertiesConfigurationHelper.BEAN_PROPS_PREFIX));
		newTask.setCleanHook(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.POST_CLEAN_HOOK_PROPS_PREFIX));
		newTask.setSyncHook(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.POST_SYNC_HOOK_PROPS_PREFIX));

		ConnectionType sourceConn = null;
		ServiceType sourceService = null;
		ServiceType destinationService = new LdapDestinationServiceType();
		if(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.service.SimpleJdbcSrcService")) {
			sourceConn = LscConfiguration.getConnection("src-jdbc");
			sourceService = new DatabaseSourceServiceType();
		} else if (lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.jndi.SimpleJndiSrcService")
				|| lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX).equals("org.lsc.jndi.PullableJndiSrcService")) {
			sourceConn = LscConfiguration.getConnection("src-ldap");
			sourceService = new LdapSourceServiceType();
		} else {
			throw new LscConfigurationException("Unknown connection type: " + lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX));
		}
		ConnectionType destinationConn = LscConfiguration.getConnection("dst-ldap");
		String syncOptionsType = lscProperties.getProperty(PropertiesConfigurationHelper.SYNCOPTIONS_PREFIX + "." + taskName, "org.lsc.beans.syncoptions.ForceSyncOptions");
		sourceService.setName(taskName + "-src");
		Properties srcProps = Configuration.getPropertiesSubset(lscProperties, prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX);
		for(Object srcProp :  srcProps.keySet()) {
			//sourceService.setOtherSetting((String)srcProp, srcProps.getProperty((String)srcProp));
			LOGGER.warn("Unhandled property to set up source service: " + srcProp);
		}
		sourceService.setConnection(new ServiceType.Connection());
		sourceService.getConnection().setReference(sourceConn);
		
		destinationService.setName(taskName + "-dst");
		Properties dstProps = Configuration.getPropertiesSubset(lscProperties, prefix + PropertiesConfigurationHelper.DSTSERVICE_PROPS_PREFIX);
		for(Object dstProp :  dstProps.keySet()) {
			//destinationService.setOtherSetting((String)dstProp, dstProps.getProperty((String)dstProp));
			LOGGER.warn("Unhandled property to set up destination service: " + dstProp);
		}
		destinationService.setConnection(new ServiceType.Connection());
		destinationService.getConnection().setReference(destinationConn);

		if(syncOptionsType == null || 
				"org.lsc.beans.syncoptions.PropertiesBasedSyncOptions".equals(syncOptionsType)) {
			newTask.setPropertiesBasedSyncOptions(new PropertiesBasedSyncOptionsType());
		} else if ("org.lsc.beans.syncoptions.ForceSyncOptions".equals(syncOptionsType)) {
			newTask.setForceSyncOptions(new ForceSyncOptionsType());
		} else {
			throw new LscConfigurationException("Unknown sync options type: " + lscProperties.getProperty(prefix + PropertiesConfigurationHelper.SRCSERVICE_PROPS_PREFIX));
		}
		ValuesType customLibraries = new ValuesType();
		if(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.CUSTOMLIBRARY_PROPS_PREFIX) != null) {
			customLibraries.getString().add(lscProperties.getProperty(prefix + PropertiesConfigurationHelper.CUSTOMLIBRARY_PROPS_PREFIX));
		}
		newTask.setCustomLibrary(customLibraries);
		
		LscConfiguration.getSyncOptions(newTask).setMainIdentifier(lscProperties.getProperty(prefix + "dn"));
		
		LscConfiguration.loadSyncOptions(newTask, taskName, Configuration.getPropertiesSubset(lscProperties, PropertiesConfigurationHelper.SYNCOPTIONS_PREFIX + "." + taskName));
		
		return newTask;
	}
	
	private static void checkTaskOldProperty(Properties props, String taskName, String propertyName, String message) {
		if (props.getProperty(PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName) != null) {
			String errorMessage = "Deprecated value specified in task " + taskName + " for " + propertyName + "! Please read upgrade notes ! (" + message + ")";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}
	}

	private static String getTaskPropertyAndCheckNotNull(String taskName, Properties props, String propertyName) {
		String value = props.getProperty(PropertiesConfigurationHelper.TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName);

		if (value == null) {
			String errorMessage = "No value specified in task " + taskName + " for " + propertyName + "! Aborting.";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}

		return value;
	}
}
