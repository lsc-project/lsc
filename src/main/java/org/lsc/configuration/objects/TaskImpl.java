/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2010, LSC Project 
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
 *               (c) 2008 - 2010 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.configuration.objects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.lsc.Configuration;
import org.lsc.beans.IBean;
import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.SyncOptionsFactory;
import org.lsc.jndi.IJndiWritableService;
import org.lsc.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represent a LSC task
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class TaskImpl implements Task {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskImpl.class);

	/** lsc prefix. */
	public static final String LSC_PROPS_PREFIX = "lsc";

	/** lsc.tasks property. */
	public static final String TASKS_PROPS_PREFIX = "tasks";

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

	/** lsc.tasks.TASKNAME.postSyncHook property. */
	public static final String POST_SYNC_HOOK_PROPS_PREFIX = "postSyncHook";
	
	/** lsc.tasks.TASKNAME.postCleanHook property. */
	public static final String POST_CLEAN_HOOK_PROPS_PREFIX = "postCleanHook";
	    
	private String name;

	private String type;
	
	private String dn;
	
	private IBean bean;
	
	private IJndiWritableService destinationService;
	
	private IService sourceService;

	private ISyncOptions syncOptions;
	
	private Object customLibrary;

	private String cleanHook;
	
	private String syncHook;
	
	public TaskImpl(String name, Properties lscProperties) {
		this.name = name;
		try {
			String prefix = TASKS_PROPS_PREFIX + "." + name + ".";
	
			// Get all properties
			// TODO : nice error message if a class name is specified but doesn't exist
			checkTaskOldProperty(lscProperties, name, OBJECT_PROPS_PREFIX, "Please take a look at upgrade notes at http://lsc-project.org/wiki/documentation/upgrade/1.1-1.2");
			String beanClassName = getTaskPropertyAndCheckNotNull(name, lscProperties, BEAN_PROPS_PREFIX);
			String srcServiceClass = getTaskPropertyAndCheckNotNull(name, lscProperties, SRCSERVICE_PROPS_PREFIX);
			String dstServiceClass = getTaskPropertyAndCheckNotNull(name, lscProperties, DSTSERVICE_PROPS_PREFIX);
			
			cleanHook = lscProperties.getProperty(TASKS_PROPS_PREFIX + "." + name + "." + POST_CLEAN_HOOK_PROPS_PREFIX);
			syncHook = lscProperties.getProperty(TASKS_PROPS_PREFIX + "." + name + "." + POST_SYNC_HOOK_PROPS_PREFIX);
			
			// Instantiate the destination service from properties
			Properties dstServiceProperties = Configuration.getAsProperties(LSC_PROPS_PREFIX + "." + prefix + DSTSERVICE_PROPS_PREFIX);
			Constructor<?> constr = Class.forName(dstServiceClass).getConstructor(new Class[]{Properties.class, String.class});
			destinationService = (IJndiWritableService) constr.newInstance(new Object[]{dstServiceProperties, beanClassName});
	
			// Instantiate custom JavaScript library from properties
			String customLibraryName = lscProperties.getProperty(prefix + CUSTOMLIBRARY_PROPS_PREFIX);
			if (customLibraryName != null) {
				customLibrary = Class.forName(customLibraryName).newInstance();
			}
	
			// Instantiate source service and pass any properties
			Properties srcServiceProperties = Configuration.getAsProperties(LSC_PROPS_PREFIX + "." + prefix + SRCSERVICE_PROPS_PREFIX);
			try {
				Constructor<?> constrSrcService = Class.forName(srcServiceClass).getConstructor(new Class[]{Properties.class, String.class});
				sourceService = (IService) constrSrcService.newInstance(new Object[]{srcServiceProperties, beanClassName});
	
			} catch (NoSuchMethodException e) {
				try {
					// backwards compatibility: if the source service doesn't take any properties,
					// use the parameter less constructor
					Constructor<?> constrSrcService = Class.forName(srcServiceClass).getConstructor(new Class[]{Properties.class});
					sourceService = (IService) constrSrcService.newInstance(new Object[] {srcServiceProperties});
				} catch (NoSuchMethodException e1) {
					// backwards compatibility: if the source service doesn't take any properties,
					// use the parameter less constructor
					Constructor<?> constrSrcService = Class.forName(srcServiceClass).getConstructor(new Class[]{});
					sourceService = (IService) constrSrcService.newInstance();
				}
			}
			
			initializeSyncOptions();
		// Manage exceptions
		} catch (Exception e) {
			Class<?>[] exceptionsCaught = {InstantiationException.class, IllegalAccessException.class,
				ClassNotFoundException.class, SecurityException.class, NoSuchMethodException.class,
				IllegalArgumentException.class, InvocationTargetException.class};

			if (ArrayUtils.contains(exceptionsCaught, e.getClass())) {
				LOGGER.error("Error while launching the following task: {}. Please check your code ! ({})", name, e);
				LOGGER.debug(e.toString(), e);
			} else {
				throw new RuntimeException(e.toString(), e);
			}
		}

	}
	
	private void checkTaskOldProperty(Properties props, String taskName, String propertyName, String message) {
		if (props.getProperty(TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName) != null) {
			String errorMessage = "Deprecated value specified in task " + taskName + " for " + propertyName + "! Please read upgrade notes ! (" + message + ")";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}
	}

	private String getTaskPropertyAndCheckNotNull(String taskName, Properties props, String propertyName) {
		String value = props.getProperty(TASKS_PROPS_PREFIX + "." + taskName + "." + propertyName);

		if (value == null) {
			String errorMessage = "No value specified in task " + taskName + " for " + propertyName + "! Aborting.";
			LOGGER.error(errorMessage);
			throw new ExceptionInInitializerError(errorMessage);
		}

		return value;
	}

	public String getType() {
		return type;
	}
	
	public String getDn() {
		return dn;
	}
	
	public IBean getBean() {
		return bean;
	}
	
	public String getName() {
		return name;
	}
	
	public IService getSourceService() {
		return sourceService;
	}
	
	public IJndiWritableService getDestinationService() {
		return destinationService;
	}
	
	public ISyncOptions getSyncOptions() {
		return syncOptions;
	}
	
	public Object getCustomLibrary() {
		return customLibrary;
	}

	public String getCleanHook() {
		return cleanHook;
	}
	
	public String getSyncHook() {
		return syncHook;
	}

	/**
	 * @param syncName
	 * @return ISyncOptions syncoptions object for the specified syncName
	 */
	protected void initializeSyncOptions() {
		syncOptions = SyncOptionsFactory.getInstance(name);

		if (syncOptions == null) {
			if ((name == null) || (name.length() == 0)) {
				LOGGER.info("No SyncOptions configuration. Defaulting to Force policy ...");
			} else {
				LOGGER.warn("Unknown '{}' synchronization task name. Defaulting to Force policy ...", name);
			}
			syncOptions = new ForceSyncOptions();
		}
	}
}
