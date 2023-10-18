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
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.lsc.beans.IBean;
import org.lsc.beans.InfoCounter;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscConfigurationException;
import org.lsc.jmx.LscServerImpl;
import org.lsc.runnable.SynchronizeEntryRunner;
import org.lsc.service.IAsynchronousService;
import org.lsc.utils.LSCStructuralLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends AbstractSynchronize to instantiate a simple synchronization engine
 * This class is responsible for reading LSC properties and using specified classes
 * and objects to avoid implementing each every time. You may want to override
 * this class to implement your own way of synchronizing - but you also need
 * to rewrite the org.lsc.Launcher class.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SimpleSynchronize extends AbstractSynchronize {

	/** the magic keyword for all synchronization. */
	public static final String ALL_TASKS_KEYWORD = "all";

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSynchronize.class);

	public static final List<String> EMPTY_LIST = new ArrayList<String>();

	Map<String, Task> cache;

	/**
	 * Default constructor
	 */
	public SimpleSynchronize() {
		super();
		setThreads(5); 
		cache = new TreeMap<String, Task>();
	}

	public void init() throws LscConfigurationException {
		Collection<TaskType> tasks = LscConfiguration.getTasks();
		for(TaskType t: tasks) {
			cache.put(t.getName(), new Task(t));
		}
	}

	private void close() {
		for (Task task: cache.values()) {
			if (task.getSourceService() instanceof Closeable) {
				try {
					((Closeable)task.getSourceService()).close();
				} catch (IOException e) {
					LOGGER.error("Error while closing service.");
				}
			}
			if (task.getDestinationService() instanceof Closeable) {
				try {
					((Closeable)task.getDestinationService()).close();
				} catch (IOException e) {
					LOGGER.error("Error while closing service.");
				}
			}
		}
	}

	/**
	 * Main method Check properties, and for each task, launch the
	 * synchronization and the cleaning phases.
	 * @param asyncTasks string list of the asynchronous synchronization tasks to launch
	 * @param syncTasks string list of the synchronization tasks to launch
	 * @param cleanTasks string list of the cleaning tasks to launch
	 * @return the launch status - true if all tasks executed successfully, false if no tasks were executed or any failed
	 * @throws Exception
	 */
	public final boolean launch(final List<String> asyncTasks, final List<String> syncTasks,
			final List<String> cleanTasks) throws Exception {
		boolean foundATask = false;
		boolean canClose = true;
		boolean launchResult = true;

		if(getTasksName() == null) {
			return false;
		} else if(getTasks().length == 0) {
			init();
		}
		if(!asyncTasks.isEmpty()) {
			LscServerImpl.startJmx(this);
		}

		Collection<Task> asyncTasksToLaunch = userOrderedTask(asyncTasks, asyncTasks.contains(ALL_TASKS_KEYWORD));
		Collection<Task> syncTasksToLaunch = userOrderedTask(syncTasks, syncTasks.contains(ALL_TASKS_KEYWORD));
		Collection<Task> cleanTasksToLaunch = userOrderedTask(cleanTasks, cleanTasks.contains(ALL_TASKS_KEYWORD));

		for (Task task: syncTasksToLaunch) {
			foundATask = true;

			if (!launchTask(task, Task.Mode.sync)) {
				launchResult = false;
			} else {
				if(task.getSyncHook() != null && task.getSyncHook() != "") {
					runPostHook(task.getName(), task.getSyncHook(), task.getTaskType());
				}
			}
		}
		for (Task task: cleanTasksToLaunch) {
			foundATask = true;

			if (!launchTask(task, Task.Mode.clean)) {
				launchResult = false;
			} else {
				if(task.getCleanHook() != null && task.getCleanHook() != "") {
					runPostHook(task.getName(), task.getCleanHook(), task.getTaskType());
				}
			}
		}
		for (Task task: asyncTasksToLaunch) {
			foundATask = true;

			canClose = false;

			if(!launchTask(task, Task.Mode.async)) {
				launchResult = false;
			}
		}

		if (canClose) {
			close();
		}

		if (!foundATask) {
			LOGGER.error("No specified tasks could be launched! Check spelling and that they exist in the configuration file.");
			return false;
		}

		return launchResult;
	}

	private Collection<Task> userOrderedTask(List<String> userValues, boolean all) {
		Collection<Task> allTasks = cache.values();
		if (all) {
			return allTasks;
		}
		Collection<String> allTaskNames = allTasks.stream().map(Task::getName).collect(Collectors.toList());
		return userValues.stream()
				.filter(allTaskNames::contains)
				.map(cache::get)
				.collect(Collectors.toList());
	}

	/**
	 * Launch a task. Call this for once each task type and task mode.
	 *
	 * @param taskName the task name (historically the LDAP object class name, but can be any string)
	 * @param taskMode the task mode (clean or sync)
	 * @return boolean true on success, false if an error occurred
	 * @throws Exception
	 */
	private boolean launchTask(final Task task, final Task.Mode taskMode) throws Exception {
		boolean status = true;

		addScriptingContext(task);

		try {
			LSCStructuralLogger.DESTINATION.info("Starting {} for {}", taskMode.name(), task.getName());
			// Do the work!
			switch (taskMode) {
			case clean:
				status = clean2Ldap(task);
				break;
			case sync:
				status = synchronize2Ldap(task);
				break;
			case async:
				if(task.getSourceService() instanceof IAsynchronousService
						|| task.getDestinationService() instanceof IAsynchronousService) {
					startAsynchronousSynchronize2Ldap(task);
				} else {
					LOGGER.error("Requested asynchronous source service does not implement IAsynchronousService ! (" + task.getSourceService().getClass().getName() + ")");
				}
				break;
			default:
				//Should not happen
				LOGGER.error("Unknown task mode type {}", taskMode.toString());
				throw new InvalidParameterException("Unknown task mode type " + taskMode.toString());
			}

			// Manage exceptions
		} catch (Exception e) {
			Class<?>[] exceptionsCaught = {InstantiationException.class, IllegalAccessException.class,
					ClassNotFoundException.class, SecurityException.class, NoSuchMethodException.class,
					IllegalArgumentException.class, InvocationTargetException.class};

			if (ArrayUtils.contains(exceptionsCaught, e.getClass())) {
				String errorDetail;
				if (e instanceof InvocationTargetException && e.getCause() != null) {
					errorDetail = e.getCause().toString();
				} else {
					errorDetail = e.toString();
				}

				LOGGER.error("Error while launching task \"{}\". Please check your configuration! ({})", task.getName(), errorDetail);
				LOGGER.debug(e.toString(), e);
				return false;
			}
			throw e;
		}

		return status;
	}

	private void addScriptingContext(Task task) {
		task.addScriptingVar("nocreate", nocreate);
		task.addScriptingVar("noupdate", noupdate);
		task.addScriptingVar("nodelete", nodelete);
		task.addScriptingVar("nomodrdn", nomodrdn);
	}

	/**
	 * Invoke the hook method whether it's a postsync or postclean
	 * 
	 * @param taskName the task name
	 * @param servicePostHook the fully qualified name of the method to invoke
	 * @param taskType the TaskType used to initialize the task 
	 */
	private void runPostHook(String taskName, String servicePostHook, TaskType taskType) {
		if (servicePostHook != null && servicePostHook.length() > 0) {
			LOGGER.debug("Service Post Hook found: " + servicePostHook);
			String hookClass = servicePostHook.substring(0, servicePostHook.lastIndexOf('.'));
			String hookMethod = servicePostHook.substring(servicePostHook.lastIndexOf('.') + 1);

			LOGGER.debug("Hook Class: " + hookClass);
			LOGGER.debug("Hook Method: " + hookMethod);

			if (hookClass.length() > 0 && hookMethod.length() > 0) {
				try {
					Class<?> clazz = Class.forName(hookClass);
					try {
						// Try with a TaskType parameter
						Method hook = clazz.getMethod(
								hookMethod, new Class<?>[] {TaskType.class});
						hook.invoke(null, taskType);
					} catch (NoSuchMethodException e) {
						// Try without parameter
						Method hook = clazz.getMethod(
								hookMethod, new Class<?>[] {});
						hook.invoke(null, new Object[0]);
					}
				} catch (ClassNotFoundException e) {
					LOGGER.error("Invalid Hook Class specified " + hookClass
							+ " for task " + taskName);
					LOGGER.debug(e.toString(), e);
				} catch (NoSuchMethodException e) {
					LOGGER.error("Invalid hook method " + hookMethod
							+ " specified for task " + taskName);
					LOGGER.debug(e.toString(), e);
				} catch (IllegalArgumentException e) {
					LOGGER.error("Invalid argument exception for hook method "
							+ hookClass + "." + hookMethod);
					LOGGER.debug(e.toString(), e);
				} catch (IllegalAccessException e) {
					LOGGER.error("Illegal access exception for hook method "
							+ hookClass + "." + hookMethod);
					LOGGER.debug(e.toString(), e);
				} catch (InvocationTargetException e) {
					LOGGER.error("Invocation target exception for hook method "
							+ hookClass + "." + hookMethod);
					LOGGER.debug(e.toString(), e);
				}
			}
		}
	}

	public Set<Entry<String, Task>> getTasksName() {
		return cache.entrySet();
	}

	public boolean isAsynchronousTask(String taskName) {
		return cache.get(taskName).getSourceService() instanceof IAsynchronousService;
	}

	@Override
	public Task getTask(String taskName) {
		return cache.get(taskName);
	}

	@Override
	public Task[] getTasks() {
		return cache.values().toArray(new Task[cache.values().size()]);
	}

	/**
	 * Launch a sequential synchronization based on identifiers got from the source
	 * @param taskName the task name to launch
	 * @param entries the entries to synchronize
	 * @return false if at least one synchronization has failed, true if all of them have succeeded
	 */
	public final boolean launchById(String taskName, Map<String, LscDatasets> entries) {
		Task task = cache.get(taskName);
		InfoCounter counter = new InfoCounter();
		for(Entry<String, LscDatasets> entry : entries.entrySet()) {
			new SynchronizeEntryRunner(task, counter, this, entry, true).run();
		}
		return counter.getCountError() == 0; 
	}

	public final boolean launch(String taskName, IBean bean) {
		Task task = cache.get(taskName);
		InfoCounter counter = new InfoCounter();
		return new SynchronizeEntryRunner(task, counter, this, null, true).run(bean);
	}
}
