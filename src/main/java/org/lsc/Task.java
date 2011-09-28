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
package org.lsc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.ArrayUtils;
import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.SyncOptionsFactory;
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
	
	private Object customLibrary;

	private String cleanHook;
	
	private String syncHook;
	
	public Task(org.lsc.configuration.objects.Task t) {
		this.name = t.getName();
		try {
			cleanHook = t.getCleanHook();
			syncHook = t.getSyncHook();
			
			// Instantiate the destination service from properties
			Constructor<?> constr = t.getDestinationService().getImplementation().getConstructor(new Class[]{org.lsc.configuration.objects.Task.class});
			destinationService = (IWritableService) constr.newInstance(new Object[]{t});
	
			// Instantiate custom JavaScript library from properties
			if (t.getCustomLibrary() != null) {
				String custumLibraryClassName = t.getCustomLibrary();
				customLibrary = Class.forName(custumLibraryClassName).newInstance();
			}
	
			// Instantiate source service and pass parameters
			Constructor<?> constrSrcService = t.getSourceService().getImplementation().getConstructor(new Class[]{org.lsc.configuration.objects.Task.class});
			sourceService = (IService) constrSrcService.newInstance(new Object[]{t});
	
			initializeSyncOptions(t);
		// Manage exceptions
		} catch (Exception e) {
			Class<?>[] exceptionsCaught = {InstantiationException.class, IllegalAccessException.class,
				ClassNotFoundException.class, SecurityException.class, NoSuchMethodException.class,
				IllegalArgumentException.class, InvocationTargetException.class};

			if (ArrayUtils.contains(exceptionsCaught, e.getClass())) {
				LOGGER.error("Error while launching the following task: {}. Please check your code ! ({})", name, e.getCause().getMessage());
				LOGGER.debug(e.toString(), e);
			} else {
				throw new RuntimeException(e.toString(), e);
			}
		}

	}
	
	/**
	 * @param t 
	 * @param syncName
	 * @return ISyncOptions syncoptions object for the specified syncName
	 */
	protected void initializeSyncOptions(org.lsc.configuration.objects.Task t) {
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

	public Object getCustomLibrary() {
		return customLibrary;
	}
}
