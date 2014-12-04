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
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.exception.LscServiceCommunicationException;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IAsynchronousService;
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
	 * @param syncName
	 *            the synchronization name
	 * @param srcService
	 *            the source service (JDBC or JNDI)
	 * @param dstJndiService
	 *            the jndi destination service
	 */
	protected final boolean clean2Ldap(Task task) {

		InfoCounter counter = new InfoCounter();

		// Get list of all entries from the destination
		Set<Entry<String, LscDatasets>> ids = null;
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

		ISyncOptions syncOptions = task.getSyncOptions();

		LscModifications lm = null;

		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		IBean taskBean;

		// Loop on all entries in the destination and delete them if they're not
		// found in the source
		for (Entry<String, LscDatasets> id : ids) {
			counter.incrementCountAll();

			try {
				// Search for the corresponding object in the source
				taskBean = task.getSourceService().getBean(id.getKey(), id.getValue(), false);

				// If we didn't find the object in the source, delete it in the
				// destination
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
						IBean dstBean = task.getDestinationService().getBean(id.getKey(), id.getValue(), true);
						// Log an error if the bean could not be retrieved!
						// This shouldn't happen.
						if (dstBean == null) {
							LOGGER.error("Could not retrieve the object {} from the directory!", id.getKey());
							counter.incrementCountError();
							continue;
						}

						// Put the bean in a map to pass to JavaScript
						// evaluator
						conditionObjects = new HashMap<String, Object>();
						conditionObjects.put("dstBean", dstBean);
						conditionObjects.putAll(task.getScriptingVars());

						// Evaluate if we have to do something
						doDelete = ScriptingEvaluator.evalToBoolean(task, conditionString, conditionObjects);
					}

					if (doDelete) {
						lm = new LscModifications(LscModificationType.DELETE_OBJECT, task.getName());
						lm.setMainIdentifer(id.getKey());

						List<LscDatasetModification> attrsMod = new ArrayList<LscDatasetModification>();
						for (Entry<String,Object> attr : id.getValue().getDatasets().entrySet()) {
							attrsMod.add(new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES, attr.getKey(), Collections.singletonList(attr.getValue())));
						}
						lm.setLscAttributeModifications(attrsMod);

	                    counter.incrementCountModifiable();
	                    
						// if "nodelete" was specified in command line options,
						// log action for debugging purposes and continue
	                    if (nodelete) {
							logShouldAction(lm, task.getName());
	                    	continue;
	                    }
					} else {
						continue;
					}

					// if we got here, we have a modification to apply - let's
					// do it!
					if (task.getDestinationService().apply(lm)) {
						counter.incrementCountCompleted();
						logAction(lm, id, task.getName());
					} else {
						counter.incrementCountError();
						logActionError(lm, id.getValue(), new Exception("Technical problem while applying modifications to destination service"));
					}
				}
			} catch (LscServiceException e) {
				counter.incrementCountError();
				logActionError(lm, id.getValue(), e);
				if(e.getCause().getClass().isAssignableFrom(CommunicationException.class)) {
					// we lost the connection to the source or destination, stop
					// everything!
					LOGGER.error("Connection lost! Aborting.");
					return false;
				} else {
					LOGGER.error("Unable to delete object {} ({})", id.getKey(), e.toString());
				}
			}
		}

		logStatus(counter);
		return counter.getCountError() == 0;
	}

	/**
	 * Synchronize the destination LDAP directory (create and update objects
	 * from source).
	 * 
	 * @param syncName
	 *            the synchronization name
	 * @param srcService
	 *            the source service (JDBC or JNDI or anything else)
	 * @param dstService
	 *            the JNDI destination service
	 * @param objectBean
	 * @param customLibrary
	 */
	protected final boolean synchronize2Ldap(final Task task) {
		/*final String syncName,
		final IService srcService, final IService dstService,
		final Object customLibrary*/

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
//		SynchronizeTask syncTask;
		int threadCount = 0;
		int threadPoolAwaitTerminationTimeOut = 900;
		for (Entry<String, LscDatasets> id : ids) {
//			syncTask = new SynchronizeTask(task, counter, this, id, true);
//			syncTask.run();
//			threadPool.runTask(new SynchronizeTask(task, counter, this, id, true));
			threadPool.runTask(new SynchronizeTask(task, counter, this, id, true));
			threadCount++;
			if (threadCount == 10000) {
				try {
					threadPool.shutdown();
					threadPool.awaitTermination(threadPoolAwaitTerminationTimeOut, TimeUnit.SECONDS);
					threadCount = 0;
					threadPool = null;
					threadPool = new SynchronizeThreadPoolExecutor(getThreads());
				} catch (InterruptedException e) {
					LOGGER.error("Error while shutting down the threadpool and re initializing it: " + e.toString(), e);
				}
			}
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
	 * @param jm
	 *            List of modification to do on the Ldap server
	 * @param identifier
	 *            object identifier
	 * @param except
	 *            synchronization process name
	 */
	protected final void logActionError(final LscModifications lm,
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
	 * @param jm
	 *            List of modification to do on the Ldap server
	 * @param id
	 *            object identifier
	 * @param syncName
	 *            synchronization process name
	 */
	protected final void logAction(final LscModifications lm,
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
	protected final void logShouldAction(final LscModifications lm, final String syncName) {
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

	/**
	 * Parse the command line arguments according the selected filter.
	 * 
	 * @param cmdLine
	 *            Command line options
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

class AsynchronousRunner implements Runnable {
    
    static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousRunner.class);

    private AbstractSynchronize abstractSynchronize;
    private Task task;
    private InfoCounter counter;
    
    public AsynchronousRunner(Task task, AbstractSynchronize abstractSynchronize) {
        this.task = task;
        this.abstractSynchronize = abstractSynchronize;
    }

    public void run() {
        counter = new InfoCounter();

        SynchronizeThreadPoolExecutor threadPool = new SynchronizeThreadPoolExecutor(abstractSynchronize.getThreads());

        Entry<String, LscDatasets> nextId = null;
        try {
            IAsynchronousService aService = null;
            boolean fromSource = true;
            if (task.getDestinationService() instanceof IAsynchronousService) {
                aService = (IAsynchronousService) task.getDestinationService();
                fromSource = false;
            } else if (task.getSourceService() instanceof IAsynchronousService) {
                aService = (IAsynchronousService) task.getSourceService();
            } else {
                LOGGER.error("LSC should never reach this point ! Please consider debugging the code because we are trying to launch an asynchronous sync without any asynchronous servoice !"); 
                return;
            }

            AbstractSynchronize.LOGGER.debug("Asynchronous synchronize {}", task.getName());

            boolean interrupted = false;
            while (!interrupted) {
                nextId = aService.getNextId();
                if (nextId != null) {
                    threadPool.runTask(new SynchronizeTask(task, counter, abstractSynchronize, nextId, fromSource));
                } else {
                    try {
                        Thread.sleep(aService.getInterval());
                    } catch (InterruptedException e) {
                        AbstractSynchronize.LOGGER.debug("Synchronization thread interrupted !");
                        interrupted = true;
                    }
                }
            }
        } catch (LscServiceException e) {
            counter.incrementCountError();
            abstractSynchronize.logActionError(null, nextId, e);
        }
        
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(abstractSynchronize.getTimeLimit(), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Tasks terminated according to time limit: " + e.toString(), e);
			LOGGER.info("If you want to avoid this message, " + "increase the time limit by using dedicated parameter.");
		}

    }
    
    public InfoCounter getCounter() {
    	return counter;
    }
}

/**
 * @author sbahloul
 */
class SynchronizeTask implements Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeTask.class);

    private String syncName;
	private InfoCounter counter;
	private AbstractSynchronize abstractSynchronize;
	private Entry<String, LscDatasets> id;
	private Task task;
	private boolean fromSource;

	public SynchronizeTask(final Task task, InfoCounter counter,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscDatasets> id,
			boolean fromSource) {
		this.syncName = task.getName();
		this.counter = counter;
		this.task = task;
		this.abstractSynchronize = abstractSynchronize;
		this.id = id;
		this.fromSource = fromSource;
	}

	public void run() {
        counter.incrementCountAll();
		try {
            run((fromSource ? task.getSourceService() : task.getDestinationService()).getBean(id.getKey(), id.getValue(), fromSource));
		} catch (RuntimeException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, id.getValue(), e);
			
			if (e.getCause() instanceof LscServiceCommunicationException) {
				AbstractSynchronize.LOGGER.error("Connection lost! Aborting.");
			}
		} catch (Exception e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, id.getValue(), e);
		}
	}

	public boolean run(IBean entry) {
		
		LscModifications lm = null;
		IBean dstBean = null;
		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		try {
			/*
			 * Log an error if the source object could not be retrieved! This
			 * shouldn't happen.
			 */
			if (entry == null) {
				counter.incrementCountError();
				AbstractSynchronize.LOGGER.error("Synchronization aborted because no source object has been found !");
				return false;
			}

			// Search destination for matching object
			if(id != null) {
				dstBean = task.getDestinationService().getBean(id.getKey(), id.getValue(), true);
			} else {
				LscDatasets entryDatasets = new LscDatasets();
				for(String datasetName: entry.datasets().getAttributesNames()) {
					entryDatasets.getDatasets().put(datasetName, entry.getDatasetById(datasetName));
				}
				dstBean = task.getDestinationService().getBean(entry.getMainIdentifier(), entryDatasets, true);
			}

			// Calculate operation that would be performed
			LscModificationType modificationType = BeanComparator.calculateModificationType(task, entry, dstBean);

			// Retrieve condition to evaluate before creating/updating
			Boolean applyCondition = null;
			String conditionString = task.getSyncOptions().getCondition(modificationType);

			// Don't use JavaScript evaluator for primitive cases
			if (conditionString.matches("true")) {
				applyCondition = true;
			} else if (conditionString.matches("false")) {
				applyCondition = false;
			} else {
				conditionObjects = new HashMap<String, Object>();
				conditionObjects.put("dstBean", dstBean);
				conditionObjects.put("srcBean", entry);
				conditionObjects.putAll(task.getScriptingVars());

				// Evaluate if we have to do something
				applyCondition = ScriptingEvaluator.evalToBoolean(task, conditionString, conditionObjects);
			}

			if (applyCondition) {
				lm = BeanComparator.calculateModifications(task, entry, dstBean);

				// if there's nothing to do, skip to the next object
				if (lm == null) {
					return true;
				}

	            counter.incrementCountModifiable();

	            // no modification: log action for debugging purposes and forget
				if ((modificationType == LscModificationType.CREATE_OBJECT && abstractSynchronize.nocreate)
						|| (modificationType == LscModificationType.UPDATE_OBJECT && abstractSynchronize.noupdate)
						|| (modificationType == LscModificationType.CHANGE_ID && (abstractSynchronize.nomodrdn || abstractSynchronize.noupdate))) {
					abstractSynchronize.logShouldAction(lm, syncName);
					return true;
				}

			} else {
				return true;
			}

			// if we got here, we have a modification to apply - let's do it!
			if (task.getDestinationService().apply(lm)) {
				counter.incrementCountCompleted();
				abstractSynchronize.logAction(lm, id, syncName);
				return true;
			} else {
				counter.incrementCountError();
				abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : entry.getMainIdentifier()), new Exception("Technical problem while applying modifications to the destination"));
				return false;
			}
		} catch (RuntimeException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : ( entry != null ? entry.getMainIdentifier() : e.toString())), e);
			
			if (e.getCause() instanceof LscServiceCommunicationException) {
				AbstractSynchronize.LOGGER.error("Connection lost! Aborting.");
			}
			return false;
		} catch (Exception e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(lm, (id != null ? id.getValue() : entry.getMainIdentifier()), e);
			return false;
		}
	}

	public String getSyncName() {
		return syncName;
	}

	public InfoCounter getCounter() {
		return counter;
	}

	public AbstractSynchronize getAbstractSynchronize() {
		return abstractSynchronize;
	}

	public Entry<String, LscDatasets> getId() {
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
	private int countModifiable = 0;
	private int countCompleted = 0;

	public synchronized void incrementCountAll() {
		countAll++;
	}

	public synchronized void incrementCountError() {
		countError++;
	}

	public synchronized void incrementCountModifiable() {
		countModifiable++;
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
	 * Return the count of all objects that should be modify
	 * 
	 * @return the count of all updates to do
	 */
	public synchronized int getCountModifiable() {
		return countModifiable;
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
