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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.CommunicationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.beans.BeanComparator;
import org.lsc.beans.IBean;
import org.lsc.beans.InfoCounter;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PivotTransformationType.Transformation;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceException;
import org.lsc.runnable.AsynchronousRunner;
import org.lsc.runnable.CleanEntryRunner;
import org.lsc.runnable.SynchronizeEntryRunner;
import org.lsc.service.IAsynchronousService;
import org.lsc.service.IService;
import org.lsc.utils.LSCStructuralLogger;
import org.lsc.utils.ScriptingEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract main class to derive.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractSynchronize {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractSynchronize.class);

	/** List of configured options. */
	private static Options options = new Options();

	static {
		options.addOption("nc", "nocreate", false, "Don't create any entry");
		options.addOption("nu", "noupdate", false, "Don't update");
		options.addOption("nd", "nodelete", false, "Don't delete");
		options.addOption("nr", "nomodrdn", false, "Don't rename (MODRDN)");
		options.addOption("n", "dryrun", false, "Don't update the directory at all");
	}

	/**
	 * This is the flag to prevent entries add operation in the target
	 * directory.
	 */
	public boolean nocreate = false;

	/**
	 * This is the flag to prevent entries update operation in the target
	 * directory.
	 */
	public boolean noupdate = false;

	/**
	 * This is the flag to prevent entries delete operation in the target
	 * directory.
	 */
	public boolean nodelete = false;

	/**
	 * This is the flag to prevent entries modrdn operation in the target
	 * directory.
	 */
	public boolean nomodrdn = false;

	/**
	 * Number of parallel threads handling synchronization and cleaning
	 * Default to 5
	 */
	private int threads;

	/**
	 * Maximum time waiting for synchronizing threads tasks to finish (in seconds)
	 * This is the global synchronization task time - 3600 by default
	 */
	private int timeLimit;

	/**
	 * Map used to keep trace of all running threads
	 */
	private Map<String, Thread> asynchronousThreads;

	/**
	 * Map used to get SyncrhonizeTaks from task name
	 */
	private Map<String, AsynchronousRunner> mapSTasks;

	/**
	 * Default constructor.
	 */
	protected AbstractSynchronize() {
		timeLimit = 3600;
		asynchronousThreads = new HashMap<String, Thread>();
		mapSTasks = new HashMap<String, AsynchronousRunner>();
	}

	/**
	 * Clean the destination LDAP directory (delete objects not present in
	 * source).
	 * 
	 * @param task the task to perform
	 */
	protected final boolean clean2Ldap(Task task) {

		InfoCounter counter = new InfoCounter();
		// Get list of all entries from the destination
		Set<Entry<String, LscDatasets>> ids = null;
		SynchronizeThreadPoolExecutor threadPool = null;

		try {
			ids = task.getDestinationService().getListPivots().entrySet();
		} catch (LscServiceException e) {
			LOGGER.error("Error getting list of IDs in the destination for task {}", task.getName());
			LOGGER.debug(e.toString(), e);
			return false;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant destination (no IDs found)");
			return false;
		}
		
		threadPool = new SynchronizeThreadPoolExecutor(getThreads());
		for (Entry<String, LscDatasets> id : ids) {
			threadPool.runTask(new CleanEntryRunner(task, counter, this, id));
		}
		
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(timeLimit, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Tasks terminated according to time limit: " + e.toString(), e);
			LOGGER.info("If you want to avoid this message, " + "increase the time limit by using dedicated parameter.");
		}

		logStatus(counter);
		return counter.getCountError() == 0;
	}

	/**
	 * Synchronize the destination LDAP directory (create and update objects
	 * from source).
	 * 
	 * @param task the task to perform
	 */
	protected final boolean synchronize2Ldap(final Task task) {
		
		InfoCounter counter = new InfoCounter();
		// Get list of all entries from the source
		Set<Entry<String, LscDatasets>> ids = null;
		SynchronizeThreadPoolExecutor threadPool = null;

		try {
			ids = task.getSourceService().getListPivots().entrySet();
		} catch (Exception e) {
			LOGGER.error("Error getting list of IDs in the source for task {}", task.getName());
			LOGGER.debug(e.toString(), e);
			return false;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant source (no IDs found)");
			return false;
		}

		threadPool = new SynchronizeThreadPoolExecutor(getThreads());

		/*
		 * Loop on all entries in the source and add or update them in the
		 * destination
		 */
		for (Entry<String, LscDatasets> id : ids) {
			threadPool.runTask(new SynchronizeEntryRunner(task, counter, this, id, true));
		}
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(timeLimit, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Tasks terminated according to time limit: " + e.toString(), e);
			LOGGER.info("If you want to avoid this message, " + "increase the time limit by using dedicated parameter.");
		}

		logStatus(counter);
		return counter.getCountError() == 0;
	}

	public final synchronized void startAsynchronousSynchronize2Ldap(Task task) {

		AsynchronousRunner asyncRunner = new AsynchronousRunner(task, this);
		String taskName = task.getName();

		Thread thread = new Thread(asyncRunner);
		thread.setName(taskName);
		asynchronousThreads.put(taskName, thread);
		mapSTasks.put(taskName, asyncRunner);
		thread.start();
	}

	public final synchronized void shutdownAsynchronousSynchronize2Ldap(final String syncName, boolean forceStop) {
		Thread asyncThread = asynchronousThreads.get(syncName);
		long startTime = System.currentTimeMillis();

		if(asyncThread == null) {
			LOGGER.info("Trying to stop a non running asynchronous task: " + syncName);
			return;
		}

		while(asyncThread.isAlive()) {
			try {
				asyncThread.join(1000);
				if ((System.currentTimeMillis() - startTime) > 5000) {
					if(forceStop) {
						// After 5 secondes, leaving
						asyncThread.interrupt();
						asyncThread.join(1000);
					} else {
						break;
					}
				}
			} catch(InterruptedException ie) {
				// Thread has been interrupted, doing nothing
			}
		}
		if(!asyncThread.isAlive()) {
			asynchronousThreads.remove(syncName);
			mapSTasks.remove(syncName);
		}
	}

	public final boolean isAsynchronousTaskRunning(final String syncName) {
		Thread asyncThread = asynchronousThreads.get(syncName);
		if(asyncThread != null) {
			if(asyncThread.isAlive()) {
				return true;
			} else {
				asynchronousThreads.remove(syncName);
				mapSTasks.remove(syncName);
				return false;
			}
		} else {
			return false;
		}
	}

	public final String getTaskFullStatus(final String syncName) {
		Thread asyncThread = asynchronousThreads.get(syncName);
		if(asyncThread != null && asyncThread.isAlive()) {
			AsynchronousRunner asyncRunner = mapSTasks.get(syncName);
			InfoCounter counter = asyncRunner.getCounter();
			return getLogStatus(counter);
		} else {
			return null;
		}
	}

	public abstract boolean isAsynchronousTask(String taskName);
	public abstract Task[] getTasks();
	public abstract Task getTask(String taskName);

	/**
	 * Log all effective action.
	 * 
	 * @param jm List of modification to do on the Ldap server
	 * @param identifier object identifier
	 * @param except synchronization process name
	 */
	public final void logActionError(final LscModifications lm,
			final Object data,
			final Exception except) {

		LOGGER.error("Error while synchronizing ID {}: {}", (lm != null ? lm.getMainIdentifier() : data), except.toString());
		LOGGER.debug(except.toString(), except);

		if (lm != null) {
			// TODO Fix LdifLogger to avoid this
			LOGGER.error("", lm);
		}
	}

	/**
	 * Log all effective action.
	 * 
	 * @param jm List of modification to do on the Ldap server
	 * @param id object identifier
	 * @param syncName synchronization process name
	 */
	public final void logAction(final LscModifications lm,
			final Entry<String, LscDatasets> id, final String syncName) {
		switch (lm.getOperation()) {
		case CREATE_OBJECT:
			LSCStructuralLogger.DESTINATION.info("# Adding new object {} for {}", lm.getMainIdentifier(), syncName);
			break;

		case UPDATE_OBJECT:
			LSCStructuralLogger.DESTINATION.info("# Updating object {} for {}", lm.getMainIdentifier(), syncName);
			break;

		case CHANGE_ID:
			LSCStructuralLogger.DESTINATION.info("# Renaming object {} for {}", lm.getMainIdentifier(), syncName);
			break;

		case DELETE_OBJECT:
			LSCStructuralLogger.DESTINATION.info("# Removing object {} for {}", lm.getMainIdentifier(), syncName);
			break;

		default:
			LSCStructuralLogger.DESTINATION.info("Error: unknown changetype ({} for {})", lm.getMainIdentifier(), syncName);
		}

		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.info("", lm);
	}

	/**
	 * @param jm
	 * @param id
	 * @param syncName
	 */
	public final void logShouldAction(final LscModifications lm, final String syncName) {
		switch (lm.getOperation()) {
		case CREATE_OBJECT:
			LSCStructuralLogger.DESTINATION.debug("Create condition false. Should have added object {}", lm.getMainIdentifier());
			break;

		case UPDATE_OBJECT:
			LSCStructuralLogger.DESTINATION.debug("Update condition false. Should have modified object {}", lm.getMainIdentifier());
			break;

		case CHANGE_ID:
			LSCStructuralLogger.DESTINATION.debug("ModRDN condition false. Should have renamed object {}", lm.getMainIdentifier());
			break;

		case DELETE_OBJECT:
			LSCStructuralLogger.DESTINATION.debug("Delete condition false. Should have removed object {}", lm.getMainIdentifier());
			break;

		default:
			LSCStructuralLogger.DESTINATION.debug("Error: unknown changetype ({} for {})", lm.getMainIdentifier(), syncName);
		}

		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.debug("", lm);
	}

	protected void logStatus(InfoCounter counter) {
		String totalsLogMessage = getLogStatus(counter);
		if (counter.getCountError() > 0) {
			LOGGER.error(totalsLogMessage);
		} else {
			LOGGER.info(totalsLogMessage);
		}
	}

	protected String getLogStatus(InfoCounter counter) {
		String totalsLogMessage =
				"All entries: "+ counter.getCountAll() +
				", to modify entries: "+ counter.getCountModifiable() +
				", successfully modified entries: "+counter.getCountCompleted()+
				", errors: "+counter.getCountError();
		return totalsLogMessage;
	}

	public IBean getBean(Task task, IService service, String pivotName, LscDatasets pivotAttributes, boolean fromSameService, boolean fromSource) throws LscServiceException {
		List<Transformation> transformations = LscConfiguration.getPivotTransformation(task.getTaskType());
		if (! fromSameService && transformations != null) {
			LscDatasets newPivots = new LscDatasets(pivotAttributes.getDatasets());
			for (Entry<String, Object> pivot: pivotAttributes.getDatasets().entrySet()) {
				for (Transformation transformation: transformations) {
					if (pivot.getKey().equalsIgnoreCase(transformation.getFromAttribute()) && LscConfiguration.pivotOriginMatchesFromSource(transformation.getPivotOrigin(), fromSource)) {
						newPivots.put(transformation.getToAttribute(), transform(task, transformation, pivot.getValue()));
					}
				}
			}
			return service.getBean(pivotName, newPivots, fromSameService);
		}
		return service.getBean(pivotName, pivotAttributes, fromSameService);
	}

	protected Object transform(Task task, Transformation transformation, Object value) throws LscServiceException{
		Map<String, Object> javaScriptObjects = new HashMap<String, Object>();
		javaScriptObjects.put("value", value);
		if (task.getCustomLibraries() != null) {
			javaScriptObjects.put("custom", task.getCustomLibraries());
		}
		javaScriptObjects.putAll(task.getScriptingVars());
		if (LscConfiguration.isLdapBinaryAttribute(transformation.getToAttribute())) {
			return ScriptingEvaluator.evalToByteArray(task, transformation.getValue(), javaScriptObjects);
		} else {
			return ScriptingEvaluator.evalToString(task, transformation.getValue(), javaScriptObjects);
		}

	}

	/**
	 * Parse the command line arguments according the selected filter.
	 * 
	 * @param cmdLine command line options
	 * @return the parsing status
	 */
	public final boolean parseOptions(final CommandLine cmdLine) {
		if (cmdLine.hasOption("nc")) {
			nocreate = true;
		}
		if (cmdLine.hasOption("nu")) {
			noupdate = true;
		}
		if (cmdLine.hasOption("nd")) {
			nodelete = true;
		}
		if (cmdLine.hasOption("nr")) {
			nomodrdn = true;
		}
		if (cmdLine.hasOption("n")) {
			nocreate = true;
			noupdate = true;
			nodelete = true;
			nomodrdn = true;
		}
		return true;
	}

	/**
	 * Get options against which the command line is analyzed.
	 * 
	 * @return the options
	 */
	public static final Options getOptions() {
		return options;
	}

	/**
	 * Parallel synchronizing threads accessor
	 * 
	 * @return the number of parallels threads dedicated to tasks
	 *         synchronization
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Parallel synchronizing threads accessor
	 * 
	 * @param threads the number of parallels threads dedicated to tasks synchronization
	 */
	public void setThreads(int threads) {
		this.threads = threads;
	}

	/**
	 * Time limit accessor
	 * @return the number of seconds
	 */
	public int getTimeLimit() {
		return timeLimit;
	}

	/**
	 * Time limit accessor
	 * 
	 * @param timeLimit number of seconds
	 */
	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

}
