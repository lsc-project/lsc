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
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.SyncOptionsFactory;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscConfigurationException;
import org.lsc.service.IService;
import org.lsc.service.IWritableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represent a LSC task
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class Task {

	/**
	 * Enum for the type of mode
	 */
	public enum Mode {
		clean,
		sync,
		async;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Task.class);

	private String name;

	private IWritableService destinationService;
	
	private IService sourceService;

	private ISyncOptions syncOptions;
	
	private Object[] customLibraries;
	
	private List<File> scriptIncludes;

	private Map<String, Object> scriptingVars = new HashMap<String, Object>();

	private String cleanHook;
	
	private String syncHook;
	
	private TaskType taskType;
	
	public Task(TaskType t) throws LscConfigurationException {
		this.name = t.getName();
		this.taskType = t;
		try {
			cleanHook = t.getCleanHook();
			syncHook = t.getSyncHook();
			
			// Instantiate the destination service from properties
			if (LscConfiguration.getSourceService(t) == null) {
				throw new LscConfigurationException("Missing source service for task=" + t.getName());
			} else if(LscConfiguration.getDestinationService(t) == null) {
				throw new LscConfigurationException("Missing destination service for task=" + t.getName());
			}
			Constructor<?> constr = LscConfiguration.getServiceImplementation(LscConfiguration.getDestinationService(t)).getConstructor(new Class[]{TaskType.class});
			destinationService = (IWritableService) constr.newInstance(new Object[]{t});
	
			// Instantiate custom JavaScript library from properties
			if (t.getCustomLibrary() != null && t.getCustomLibrary().getString() != null) {
				customLibraries = new Object[t.getCustomLibrary().getString().size()];
				int customLibrariesIndex = 0;
				for(String custumLibraryClassName: t.getCustomLibrary().getString()) {
					customLibraries[customLibrariesIndex++] = Class.forName(custumLibraryClassName).newInstance();
				}
			}
			
			// Load custom scripts
			if (t.getScriptInclude() != null && t.getScriptInclude().getString() != null) {
				scriptIncludes = new ArrayList<File>();
				for (String script: t.getScriptInclude().getString()) {
					File scriptFile = new File(Configuration.getConfigurationDirectory() + "/" + script);
					if (scriptFile.exists()) {
						scriptIncludes.add(scriptFile);
					} else {
						LOGGER.warn("File " + scriptFile.getAbsolutePath() + "doesn't exist.");
					}
				}
			}
	
			// Instantiate source service and pass parameters
			Constructor<?> constrSrcService = LscConfiguration.getServiceImplementation(LscConfiguration.getSourceService(t)).getConstructor(new Class[]{TaskType.class});
			sourceService = (IService) constrSrcService.newInstance(new Object[]{t});
	
			initializeSyncOptions(t);
		// Manage exceptions
        } catch (InvocationTargetException e) {
            throw new LscConfigurationException(e.getCause());
		} catch (LscConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new LscConfigurationException(e);
		}

	}
	
	/**
	 * @param t 
	 * @param syncName
	 * @return ISyncOptions syncoptions object for the specified syncName
	 * @throws LscConfigurationException 
	 */
	protected void initializeSyncOptions(TaskType t) throws LscConfigurationException {
		syncOptions = SyncOptionsFactory.convert(t);

		if (syncOptions == null) {
			if ((name == null) || (name.length() == 0)) {
				LOGGER.info("No SyncOptions configuration. Defaulting to Force policy ...");
			} else {
				LOGGER.warn("Unknown '{}' synchronization task name. Defaulting to Force policy ...", name);
			}
			syncOptions = new ForceSyncOptions();
		}
	}
	
	public String getCleanHook() {
		return cleanHook;
	}
	
	public String getSyncHook() {
		return syncHook;
	}

	public String getName() {
		return name;
	}
	
	public IService getSourceService() {
		return sourceService;
	}

	public IWritableService getDestinationService() {
		return destinationService;
	}
	
	public ISyncOptions getSyncOptions() {
		return syncOptions;
	}

	public Object[] getCustomLibraries() {
		return customLibraries;
	}
	
	public List<File> getScriptIncludes() {
		return scriptIncludes;
	}

	public Map<String, Object> getScriptingVars() {
		return scriptingVars;
	}
	
	public void addScriptingVar(String identifier, Object value) {
		scriptingVars.put(identifier, value);
	}

	public TaskType getTaskType() {
		return taskType;
	}
}
