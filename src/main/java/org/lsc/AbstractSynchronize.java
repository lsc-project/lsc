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
package org.lsc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.lsc.beans.BeanComparator;
import org.lsc.beans.IBean;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.configuration.objects.Task;
import org.lsc.jndi.IJndiWritableService;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.service.IAsynchronousService;
import org.lsc.service.IService;
import org.lsc.utils.JScriptEvaluator;
import org.lsc.utils.LSCStructuralLogger;
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
	protected boolean nocreate = false;

	/**
	 * This is the flag to prevent entries update operation in the target
	 * directory.
	 */
	protected boolean noupdate = false;

	/**
	 * This is the flag to prevent entries delete operation in the target
	 * directory.
	 */
	protected boolean nodelete = false;

	/**
	 * This is the flag to prevent entries modrdn operation in the target
	 * directory.
	 */
	protected boolean nomodrdn = false;

	/**
	 * Number of parallel threads handling synchronization and cleaning.
	 * Defaults to 5.
	 */
	private int threads;

	/**
	 * Maximum time waiting for synchronizing threads tasks to finish (in seconds).
	 * This is the global synchronization task time - 3600 by default.
	 */
	private int timeLimit;

	/**
	 * Map used to keep trace of all running threads.
	 */
	private Map<String, Thread> asynchronousThreads;

	/**
	 * Default constructor.
	 */
	protected AbstractSynchronize() {
		timeLimit = 3600;
		asynchronousThreads = new HashMap<String, Thread>();
	}

	/**
	 * Clean the destination LDAP directory (delete objects not present in source).
	 * 
	 * @param task TODO
	 */
	protected final void clean2Ldap(Task task) {

		InfoCounter counter = new InfoCounter();

		// Get list of all entries from the destination
		Set<Entry<String, LscAttributes>> ids = null;
		try {
			ids = task.getDestinationService().getListPivots().entrySet();
		} catch (NamingException e) {
			LOGGER.error("Error getting list of IDs in the destination for task {}", task.getName());
			LOGGER.debug(e.toString(), e);
			return;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant destination (no IDs found)");
			return;
		}

		ISyncOptions syncOptions = task.getSyncOptions();

		JndiModifications jm = null;

		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		IBean taskBean;

		// Loop on all entries in the destination and delete them if they're not found in the source
		for (Entry<String, LscAttributes> id : ids) {
			counter.incrementCountAll();

			try {
				// Search for the corresponding object in the source
				taskBean = task.getSourceService().getBean(id.getKey(), id.getValue());

				// If we didn't find the object in the source, delete it in the destination
				if (taskBean == null) {
					// Retrieve condition to evaluate before deleting
					Boolean doDelete = null;
					String conditionString = syncOptions.getDeleteCondition();

					// Don't use JavaScript evaluator for primitive cases
					if (conditionString.matches("true")) {
						doDelete = true;
					} else if (conditionString.matches("false")) {
						doDelete = false;
					} else {
						// If condition is based on dstBean, retrieve the full object from destination
						if (conditionString.contains("dstBean")) {

							IBean dstBean = task.getDestinationService().getBean(id.getKey(), id.getValue());
							// Log an error if the bean could not be retrieved! This shouldn't happen.
							if (dstBean == null) {
								LOGGER.error("Could not retrieve the object {} from the directory!", id.getKey());
								counter.incrementCountError();
								continue;
							}

							// Put the bean in a map to pass to JavaScript evaluator
							conditionObjects = new HashMap<String, Object>();
							conditionObjects.put("dstBean", dstBean);
						}

						// Evaluate if we have to do something
						doDelete = JScriptEvaluator.evalToBoolean(conditionString, conditionObjects);
					}

					// Only create delete modification object if (or):
					// 	1)	the condition is true (obviously)
					//	2)	the condition is false and we would delete an object
					//		and "nodelete" was specified in command line options
					// Case 2 is for debugging purposes.
					if (doDelete || nodelete) {
						jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, task.getName());
						jm.setDistinguishName(id.getKey());

						// if "nodelete" was specified in command line options,
						// or if the condition is false,
						// log action for debugging purposes and continue
						if (nodelete) {
							logShouldAction(jm, id, task.getName());
							continue;
						}
					} else {
						continue;
					}

					// if we got here, we have a modification to apply - let's do it!
					counter.incrementCountInitiated();
					if (task.getDestinationService().apply(jm)) {
						counter.incrementCountCompleted();
						logAction(jm, id, task.getName());
					} else {
						counter.incrementCountError();
						logActionError(jm, id, new Exception("Technical problem while applying modifications to directory"));
					}
				}
			} catch (CommunicationException e) {
				// we lost the connection to the source or destination, stop everything!
				counter.incrementCountError();
				LOGGER.error("Connection lost! Aborting.");
				logActionError(jm, id, e);
				return;
			} catch (NamingException e) {
				counter.incrementCountError();
				LOGGER.error("Unable to delete object {} ({})", id.getKey(), e.toString());
				logActionError(jm, id, e);
			}
		}

		String totalsLogMessage = "All entries: {}, to modify entries: {}, modified entries: {}, errors: {}";
		Object[] objects = new Object[] { counter.getCountAll(), counter.getCountInitiated(), counter.getCountCompleted(), counter.getCountError() };
		if (counter.getCountError() > 0) {
			LOGGER.error(totalsLogMessage, objects);
		} else {
			LOGGER.info(totalsLogMessage, objects);
		}
	}

	/**
	 * Synchronize the destination LDAP directory (create and update objects from source).
	 * 
	 * @param task TODO
	 */
	protected final void synchronize2Ldap(final Task task) {
		/*final String syncName,
		final IService srcService, final IService dstService,
		final Object customLibrary*/

		InfoCounter counter = new InfoCounter();
		// Get list of all entries from the source
		Set<Entry<String, LscAttributes>> ids = null;
		SynchronizeThreadPoolExecutor threadPool = null;

		try {
			ids = task.getSourceService().getListPivots().entrySet();
		} catch (Exception e) {
			LOGGER.error("Error getting list of IDs in the source for task {}", task.getName());
			LOGGER.debug(e.toString(), e);
			return;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant source (no IDs found)");
			return;
		}

		threadPool = new SynchronizeThreadPoolExecutor(getThreads());

		/* Loop on all entries in the source and add or update them in the destination */
		for (Entry<String, LscAttributes> id : ids) {
			threadPool.runTask(new SynchronizeTask(task, counter, this, id));
		}
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(timeLimit, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Tasks terminated according to time limit");
			LOGGER.debug(e.toString(), e);
			LOGGER.info("If you want to avoid this message, increase the time limit by using dedicated parameter.");
		}

		String totalsLogMessage = "All entries: {}, to modify entries: {}, modified entries: {}, errors: {}";
		Object[] objects = new Object[] { counter.getCountAll(), counter.getCountInitiated(), counter.getCountCompleted(), counter.getCountError() };
		if (counter.getCountError() > 0) {
			LOGGER.error(totalsLogMessage, objects);
		} else {
			LOGGER.info(totalsLogMessage, objects);
		}
	}

	public final synchronized void startAsynchronousSynchronize2Ldap(Task task) {

		InfoCounter counter = new InfoCounter();

		Thread thread = new Thread(new SynchronizeTask(task, counter, this, null));
		thread.setName(task.getName());
		asynchronousThreads.put(task.getName(), thread);
		thread.run();
	}

	public final synchronized void shutdownAsynchronousSynchronize2Ldap(final String syncName, boolean forceStop) {
		Thread asyncThread = asynchronousThreads.get(syncName);
        long startTime = System.currentTimeMillis();
        
        while (asyncThread.isAlive()) {
        	try {
    			asyncThread.join(1000);
                if ((System.currentTimeMillis() - startTime) > 5000) {
                	if (forceStop) {
                    	// After 5 secondes, leaving
            			asyncThread.interrupt();
            			asyncThread.join();
                	} else {
                		break;
                	}
                }
        	} catch (InterruptedException ie) {
        		// Thread has been interrupted, doing nothing
        	}
        }
        if (!asyncThread.isAlive()) {
    		asynchronousThreads.remove(syncName);
        }
	}

	public final synchronized boolean isAsynchronousTaskRunning(final String syncName) {
		if (asynchronousThreads.get(syncName).isAlive()) {
			return true; 
		} else {
			asynchronousThreads.remove(syncName);
			return false;
		}
	}
	
	public abstract boolean isAsynchronousTask(String taskName);
	public abstract Task[] getTasks();
	public abstract Task getTask(String taskName);
        
	/**
	 * Log all effective action.
	 *
	 * @param jm
	 *                List of modification to do on the Ldap server
	 * @param identifier
	 *                object identifier
	 * @param except
	 *                synchronization process name
	 */
	protected final void logActionError(final JndiModifications jm,
					final Entry<String, LscAttributes> identifier,
					final Exception except) {

		LOGGER.error("Error while synchronizing ID {}: {}", (jm != null ? jm.getDistinguishName() : identifier.getValue()), except.toString());
		LOGGER.debug(except.toString(), except);

		if (jm != null) {
			// TODO Fix LdifLogger to avoid this
			LOGGER.error("", jm);
		}
	}

	/**
	 * Log all effective action.
	 * 
	 * @param jm
	 *            List of modification to do on the Ldap server
	 * @param id
	 *            object identifier
	 * @param syncName
	 *            synchronization process name
	 */
	protected final void logAction(final JndiModifications jm,
					final Entry<String, LscAttributes> id,
					final String syncName) {
		switch (jm.getOperation()) {
			case ADD_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Adding new entry {} for {}", jm.getDistinguishName(), syncName);
				break;

			case MODIFY_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Updating entry {} for {}", jm.getDistinguishName(), syncName);
				break;

			case MODRDN_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Renaming entry {} for {}", jm.getDistinguishName(), syncName);
				break;

			case DELETE_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Removing entry {} for {}", jm.getDistinguishName(), syncName);
				break;

			default:
				LSCStructuralLogger.DESTINATION.info("Error: unknown changetype ({} for {})", jm.getDistinguishName(), syncName);
		}

		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.info("", jm);
	}

	/**
	 * @param jm
	 * @param id
	 * @param syncName
	 */
	protected final void logShouldAction(final JndiModifications jm,
					final Entry<String, LscAttributes> id,
					final String syncName) {
		switch (jm.getOperation()) {
			case ADD_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Create condition false. Should have added object {}", jm.getDistinguishName());
				break;

			case MODIFY_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Update condition false. Should have modified object {}", jm.getDistinguishName());
				break;

			case MODRDN_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("ModRDN condition false. Should have renamed object {}", jm.getDistinguishName());
				break;

			case DELETE_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Delete condition false. Should have removed object {}", jm.getDistinguishName());
				break;

			default:
				LSCStructuralLogger.DESTINATION.debug("Error: unknown changetype ({} for {})", jm.getDistinguishName(), syncName);
		}

		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.debug("", jm);
	}

	/**
	 * Parse the command line arguments according the selected filter.
	 * 
	 * @param cmdLine Command line options
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
	 * @param threads
	 *            the number of parallels threads dedicated to tasks
	 *            synchronization
	 */
	public void setThreads(int threads) {
		this.threads = threads;
	}

	/**
	 * Time limit accessor
	 * 
	 * @return the number of seconds
	 */
	public int getTimeLimit() {
		return timeLimit;
	}

	/**
	 * Time limit accessor
	 * 
	 * @param timeLimit
	 *            number of seconds
	 */
	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}
}

/**
 * @author sbahloul
 */
class SynchronizeTask implements Runnable {

	private String syncName;
	private InfoCounter counter;
	private IService srcService;
	private IJndiWritableService dstService;
	private Object customLibrary;
	private ISyncOptions syncOptions;
	private AbstractSynchronize abstractSynchronize;
	private Entry<String, LscAttributes> id;

	public SynchronizeTask(final Task task, InfoCounter counter,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscAttributes> id) {
		this.syncName = task.getName();
		this.counter = counter;
		this.srcService = task.getSourceService();
		this.dstService = task.getDestinationService();
		this.customLibrary = task.getCustomLibrary();
		this.syncOptions = task.getSyncOptions();
		this.abstractSynchronize = abstractSynchronize;
		this.id = id;
	}

	/**
	 * @deprecated Use SynchronizeTask constructor using a Task
	 */
	public SynchronizeTask(final String syncName, InfoCounter counter,
			final IService srcService, final IJndiWritableService dstService,
			final Object customLibrary, ISyncOptions syncOptions,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscAttributes> id) {
		this.syncName = syncName;
		this.counter = counter;
		this.srcService = srcService;
		this.dstService = dstService;
		this.customLibrary = customLibrary;
		this.syncOptions = syncOptions;
		this.abstractSynchronize = abstractSynchronize;
		this.id = id;
	}

	public void run() {

		counter.incrementCountAll();

		try {
			if (id != null) {
				AbstractSynchronize.LOGGER.debug("Synchronizing {} for {}", syncName, id.getValue());
				run(id);
			} else if (srcService instanceof IAsynchronousService) {
				AbstractSynchronize.LOGGER.debug("Asynchronous synchronize {}", syncName);

				IAsynchronousService aSrcService = (IAsynchronousService) srcService;
				while (!Thread.interrupted()) {
					Entry<String, LscAttributes> nextId = aSrcService.getNextId();
					if (nextId != null) {
						run(nextId);
					} else {
						try {
							Thread.sleep(aSrcService.getInterval() * 1000);
						} catch (InterruptedException e) {
							AbstractSynchronize.LOGGER.debug("Synchronization thread interrupted !");
						}
					}
				}
			}
		} catch (NamingException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, id, e);
		}
	}

	public boolean run(Entry<String, LscAttributes> id) {

		JndiModifications jm = null;
		IBean itmBean = null;
		IBean dstBean = null;
		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		try {
			IBean entry = srcService.getBean(id.getKey(), id.getValue());
			/*
			 * Log an error if the source object could not be retrieved! This
			 * shouldn't happen.
			 */
			if (entry == null) {
				counter.incrementCountError();
				AbstractSynchronize.LOGGER.error("Unable to get object for id={}", id.getKey());
				return false;
			}

			// Search destination for matching object
			dstBean = dstService.getBean(id.getKey(), id.getValue());

			// Clone the srcBean for comparaisons (this means the srcBean is *never* changed)
			itmBean = BeanComparator.cloneSrcBean(entry, dstBean, syncOptions, customLibrary);
			
			// Calculate operation that would be performed
			JndiModificationType modificationType = BeanComparator.calculateModificationType(syncOptions, entry, itmBean, dstBean, customLibrary);

			// Retrieve condition to evaluate before creating/updating
			Boolean applyCondition = null;
			String conditionString = syncOptions.getCondition(modificationType);

			// Don't use JavaScript evaluator for primitive cases
			if (conditionString.matches("true")) {
				applyCondition = true;
			} else if (conditionString.matches("false")) {
				applyCondition = false;
			} else {
				conditionObjects = new HashMap<String, Object>();
				conditionObjects.put("dstBean", dstBean);
				conditionObjects.put("srcBean", entry);

				// Evaluate if we have to do something
				applyCondition = JScriptEvaluator.evalToBoolean(conditionString, conditionObjects);
			}

			// Only evaluate modifications if (or):
			// 1) the condition is true (obviously)
			// 2) the condition is false and
			// a) we would create an object and "nocreate" was specified in
			// command line options
			// b) we would update an object and "noupdate" was specified in
			// command line options
			// Case 2 is for debugging purposes.
			Boolean calculateForDebugOnly = (modificationType == JndiModificationType.ADD_ENTRY && abstractSynchronize.nocreate) || (modificationType == JndiModificationType.MODIFY_ENTRY && abstractSynchronize.noupdate) || (modificationType == JndiModificationType.MODRDN_ENTRY && (abstractSynchronize.nomodrdn || abstractSynchronize.noupdate));

			if (applyCondition || calculateForDebugOnly) {
				jm = BeanComparator.calculateModifications(syncOptions, entry, itmBean, dstBean, customLibrary, (applyCondition && !calculateForDebugOnly));

				// if there's nothing to do, skip to the next object
				if (jm == null) {
					return true;
				}

				// apply condition is false, log action for debugging purposes
				// and forget
				if (!applyCondition || calculateForDebugOnly) {
					abstractSynchronize.logShouldAction(jm, id, syncName);
					return true;
				}
			} else {
				return true;
			}

			// if we got here, we have a modification to apply - let's do it!
			counter.incrementCountInitiated();
			if (dstService.apply(jm)) {
				counter.incrementCountCompleted();
				abstractSynchronize.logAction(jm, id, syncName);
				return true;
			} else {
				counter.incrementCountError();
				abstractSynchronize.logActionError(jm, id, new Exception("Technical problem while applying modifications to directory"));
				return false;
			}
		} catch (CommunicationException e) {
			// we lost the connection to the source or destination, stop
			// everything!
			counter.incrementCountError();
			AbstractSynchronize.LOGGER.error("Connection lost! Aborting.");
			abstractSynchronize.logActionError(jm, id, e);
			return false;
		} catch (RuntimeException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(jm, id, e);
			
			if (e.getCause() instanceof CommunicationException) {
				AbstractSynchronize.LOGGER.error("Connection lost! Aborting.");
			}
			return false;
		} catch (Exception e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(jm, id, e);
			return false;
		}
	}

	public String getSyncName() {
		return syncName;
	}

	public InfoCounter getCounter() {
		return counter;
	}

	public IService getSrcService() {
		return srcService;
	}

	public IService getDstService() {
		return dstService;
	}

	public Object getCustomLibrary() {
		return customLibrary;
	}

	public ISyncOptions getSyncOptions() {
		return syncOptions;
	}

	public AbstractSynchronize getAbstractSynchronize() {
		return abstractSynchronize;
	}

	public Entry<String, LscAttributes> getId() {
		return id;
	}

}

/**
 * This object is storing counters across all tasks Update methods are specified
 * as synchronized to avoid loosing counts of operations
 * 
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
class InfoCounter {

	private int countAll = 0;
	private int countError = 0;
	private int countInitiated = 0;
	private int countCompleted = 0;

	public synchronized void incrementCountAll() {
		countAll++;
	}

	public synchronized void incrementCountError() {
		countError++;
	}

	public synchronized void incrementCountInitiated() {
		countInitiated++;
	}

	public synchronized void incrementCountCompleted() {
		countCompleted++;
	}

	/**
	 * Return the count of all objects concerned by synchronization It does not
	 * include objects in data source that are not selected by requests or
	 * filters, but it includes any of the objects retrieved from the data
	 * source
	 * 
	 * @return the count of all objects taken from the data source
	 */
	public synchronized int getCountAll() {
		return countAll;
	}

	/**
	 * Return the count of all objects that have encountered an error while
	 * synchronizing, either for a technical or for a functional reason
	 * 
	 * @return the number of objects in error
	 */
	public synchronized int getCountError() {
		return countError;
	}

	/**
	 * Return the count of all objects that have been embraced in a data
	 * modification (successfully or not)
	 * 
	 * @return the count of all attempted updates
	 */
	public synchronized int getCountInitiated() {
		return countInitiated;
	}

	/**
	 * Return the count of all objects that have been embraced in a data
	 * modification successfully
	 * 
	 * @return the count of all successful updates
	 */
	public synchronized int getCountCompleted() {
		return countCompleted;
	}
}
