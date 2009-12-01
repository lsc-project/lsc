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
import java.util.Map.Entry;

import java.util.Set;
import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.lsc.beans.BeanComparator;
import org.lsc.beans.IBean;
import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.SyncOptionsFactory;
import org.lsc.jndi.IJndiDstService;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;
import org.lsc.service.ISrcService;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSynchronize.class);

	/** List of configured options. */
	private Options options;

	/**
	 * This is the flag to prevent entries add operation in the target
	 * directory.
	 */
	private boolean nocreate = false;

	/**
	 * This is the flag to prevent entries update operation in the target
	 * directory.
	 */
	private boolean noupdate = false;

	/**
	 * This is the flag to prevent entries delete operation in the target
	 * directory.
	 */
	private boolean nodelete = false;

	/**
	 * This is the flag to prevent entries modrdn operation in the target
	 * directory.
	 */
	private boolean nomodrdn = false;

	/**
	 * Default constructor.
	 */
	protected AbstractSynchronize() {
		options = new Options();
		options.addOption("nc", "nocreate", false, "Don't create any entry");
		options.addOption("nu", "noupdate", false, "Don't update");
		options.addOption("nd", "nodelete", false, "Don't delete");
		options.addOption("nr", "nomodrdn", false, "Don't rename (MODRDN)");
		options.addOption("n", "dryrun", false, "Don't update the directory at all");
	}

	/**
	 * Clean the destination LDAP directory (delete objects not present in source).
	 *
	 * @param syncName
	 *                the synchronization name
	 * @param srcService
	 *                the source service (JDBC or JNDI)
	 * @param dstJndiService
	 *                the jndi destination service
	 */
	protected final void clean2Ldap(final String syncName,
					final Class<IBean> taskBeanClass,
					final ISrcService srcService,
					final IJndiDstService dstJndiService) {

		// Get list of all entries from the destination
		Set<Entry<String, LscAttributes>> ids = null;
		try {
			ids = dstJndiService.getListPivots().entrySet();
		} catch (NamingException e) {
			LOGGER.error("Error getting list of IDs in the destination for task {}", syncName);
			LOGGER.debug(e.toString(), e);
			return;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant destination (no IDs found)");
			return;
		}

		ISyncOptions syncOptions = this.getSyncOptions(syncName);

		int countAll = 0;
		int countError = 0;
		int countInitiated = 0;
		int countCompleted = 0;

		JndiModifications jm = null;

		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		IBean taskBean;

		// Loop on all entries in the destination and delete them if they're not found in the source
		for(Entry<String, LscAttributes> id: ids) {
			countAll++;

			try {
				try {
					taskBean = taskBeanClass.newInstance();
				} catch (InstantiationException e) {
					LOGGER.error("Error while instanciating taskbean class: {}", e.toString());
					LOGGER.debug(e.toString(), e);
					return;
				} catch (IllegalAccessException e) {
					LOGGER.error("Error while instanciating taskbean class: {}", e.toString());
					LOGGER.debug(e.toString(), e);
					return;
				}
				// Search for the corresponding object in the source
				taskBean = srcService.getBean(taskBean, id);

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

							IBean dstBean = dstJndiService.getBean(id);
							// Log an error if the bean could not be retrieved! This shouldn't happen.
							if (dstBean == null) {
								LOGGER.error("Could not retrieve the object {} from the directory!", id.getKey());
								countError++;
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
						jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, syncName);
						jm.setDistinguishName(id.getKey());

						// if "nodelete" was specified in command line options,
						// or if the condition is false,
						// log action for debugging purposes and continue
						if (nodelete) {
							logShouldAction(jm, id, syncName);
							continue;
						}
					} else {
						continue;
					}

					// if we got here, we have a modification to apply - let's do it!
					countInitiated++;
					if (JndiServices.getDstInstance().apply(jm)) {
						countCompleted++;
						logAction(jm, id, syncName);
					} else {
						countError++;
						logActionError(jm, id, new Exception());
					}
				}
			} catch (CommunicationException e) {
				// we lost the connection to the source or destination, stop everything!
				countError++;
				LOGGER.error("Connection lost! Aborting.");
				logActionError(jm, id, e);
				return;
			} catch (NamingException e) {
				countError++;
				LOGGER.error("Unable to delete object {} ({})", id.getKey(), e.toString());
				logActionError(jm, id, e);
			}
		}

		String totalsLogMessage = "All entries: {}, to modify entries: {}, modified entries: {}, errors: {}";

		if (countError > 0) {
			LSCStructuralLogger.GLOBAL.error(totalsLogMessage, new Object[]{
							countAll, countInitiated, countCompleted,
							countError});
		} else {
			LSCStructuralLogger.GLOBAL.warn(totalsLogMessage, new Object[]{
							countAll, countInitiated, countCompleted,
							countError});
		}
	}

	/**
	 * Synchronize the destination LDAP directory (create and update objects from source).
	 *
	 * @param syncName
	 *                the synchronization name
	 * @param srcService
	 *                the source service (JDBC or JNDI or anything else)
	 * @param dstService
	 *                the JNDI destination service
	 * @param objectBean
	 * @param customLibrary
	 */
	protected final void synchronize2Ldap(final String syncName,
					final ISrcService srcService,
					final IJndiDstService dstService,
					final Class<IBean> objectBean,
					final Object customLibrary) {

		// Get list of all entries from the source
		Set<Entry<String, LscAttributes>> ids = null;
		try {
			ids = srcService.getListPivots().entrySet();
		} catch (Exception e) {
			LOGGER.error("Error getting list of IDs in the source for task {}", syncName);
			LOGGER.debug(e.toString(), e);
			return;
		}

		// Make sure we have at least one entry to work on
		if (ids.isEmpty()) {
			LOGGER.error("Empty or non existant source (no IDs found)");
			return;
		}

		int countAll = 0;
		int countError = 0;
		int countInitiated = 0;
		int countCompleted = 0;

		JndiModifications jm = null;
		ISyncOptions syncOptions = this.getSyncOptions(syncName);
		// store method to obtain source bean
		IBean srcBean = null;
		IBean dstBean = null;

		/** Hash table to pass objects into JavaScript condition */
		Map<String, Object> conditionObjects = null;

		/* Loop on all entries in the source and add or update them in the destination */
		for(Entry<String, LscAttributes> id: ids) {
			countAll++;

			LOGGER.debug("Synchronizing {} for {}", syncName, id.getValue());

			try {
				srcBean = srcService.getBean(objectBean.newInstance(), id);

				/* Log an error if the source object could not be retrieved! This shouldn't happen. */
				if (srcBean == null) {
					countError++;
					LOGGER.error("Unable to get object for id={}", id.getKey());
					continue;
				}

				// Search destination for matching object
				dstBean = dstService.getBean(id);

				// Calculate operation that would be performed
				JndiModificationType modificationType = BeanComparator.calculateModificationType(syncOptions, srcBean, dstBean, customLibrary);

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
					conditionObjects.put("srcBean", srcBean);

					// Evaluate if we have to do something
					applyCondition = JScriptEvaluator.evalToBoolean(conditionString, conditionObjects);
				}

				// Only evaluate modifications if (or):
				// 	1) the condition is true (obviously)
				//	2) the condition is false and
				//		a) we would create an object and "nocreate" was specified in command line options
				//		b) we would update an object and "noupdate" was specified in command line options
				// Case 2 is for debugging purposes.
				Boolean calculateForDebugOnly = (modificationType == JndiModificationType.ADD_ENTRY && nocreate) || (modificationType == JndiModificationType.MODIFY_ENTRY && noupdate) || (modificationType == JndiModificationType.MODRDN_ENTRY && (nomodrdn || noupdate));

				if (applyCondition || calculateForDebugOnly) {
					jm = BeanComparator.calculateModifications(syncOptions, srcBean, dstBean, customLibrary, (applyCondition && !calculateForDebugOnly));

					// if there's nothing to do, skip to the next object
					if (jm == null) {
						continue;
					}

					// apply condition is false, log action for debugging purposes and forget
					if (!applyCondition || calculateForDebugOnly) {
						logShouldAction(jm, id, syncName);
						continue;
					}
				} else {
					continue;
				}

				// if we got here, we have a modification to apply - let's do it!
				countInitiated++;
				if (JndiServices.getDstInstance().apply(jm)) {
					countCompleted++;
					logAction(jm, id, syncName);
				} else {
					countError++;
					logActionError(jm, id, new Exception());
				}
			} catch (CommunicationException e) {
				// we lost the connection to the source or destination, stop everything!
				countError++;
				LOGGER.error("Connection lost! Aborting.");
				logActionError(jm, id, e);
				return;
			} catch (ExceptionInInitializerError e) {
				// this type of exception should stop everything, too
				countError++;
				throw e;
			} catch (RuntimeException e) {
				countError++;
				logActionError(jm, id, e);
			} catch (Exception e) {
				countError++;
				logActionError(jm, id, e);
			}
		}

		String totalsLogMessage = "All entries: {}, to modify entries: {}, modified entries: {}, errors: {}";
		Object[] objects = new Object[] { countAll, countInitiated, countCompleted, countError };
		if (countError > 0) {
			LSCStructuralLogger.DESTINATION.error(totalsLogMessage, objects);
		} else {
			LSCStructuralLogger.DESTINATION.warn(totalsLogMessage, objects);
		}
	}

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
					final Entry<String, LscAttributes> identifier, final Exception except) {

		LOGGER.error("Error while synchronizing ID {}: {}", 
							(jm != null ? jm.getDistinguishName() : ""), except.toString());
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
	 *                List of modification to do on the Ldap server
	 * @param id
	 *                object identifier
	 * @param syncName
	 *                synchronization process name
	 */
	protected final void logAction(final JndiModifications jm, final Entry<String, LscAttributes> id,
					final String syncName) {
		switch (jm.getOperation()) {
			case ADD_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Adding new entry {} for {}",
								jm.getDistinguishName(), syncName);
				break;

			case MODIFY_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Updating entry {} for {}",
								jm.getDistinguishName(), syncName);
				break;

			case MODRDN_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Renaming entry {} for {}",
								jm.getDistinguishName(), syncName);
				break;

			case DELETE_ENTRY:
				LSCStructuralLogger.DESTINATION.info("# Removing entry {} for {}",
								jm.getDistinguishName(), syncName);
				break;

			default:
				LSCStructuralLogger.DESTINATION.info("Error: unknown changetype ({} for {})",
								jm.getDistinguishName(), syncName);
		}

		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.info("", jm);
	}

	/**
	 * @param jm
	 * @param id
	 * @param syncName
	 *
	 */
	protected final void logShouldAction(final JndiModifications jm, final Entry<String, LscAttributes> id,
					final String syncName) {
		switch (jm.getOperation()) {
			case ADD_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Create condition false. Should have added object {}",
								jm.getDistinguishName());
				break;

			case MODIFY_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Update condition false. Should have modified object {}",
								jm.getDistinguishName());
				break;

			case MODRDN_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("ModRDN condition false. Should have renamed object {}",
								jm.getDistinguishName());
				break;

			case DELETE_ENTRY:
				LSCStructuralLogger.DESTINATION.debug("Delete condition false. Should have removed object {}",
								jm.getDistinguishName());
				break;

			default:
				LSCStructuralLogger.DESTINATION.debug("Error: unknown changetype ({} for {})",
								jm.getDistinguishName(), syncName);
		}
		
		// TODO Fix LdifLogger to avoid this
		LSCStructuralLogger.DESTINATION.debug("", jm);
	}

	/**
	 * Parse the command line arguments according the selected filter.
	 *
	 * @param args
	 *                the command line arguments
	 *
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
	public final Options getOptions() {
		return options;
	}

	/**
	 * @param syncName
	 * @return ISyncOptions syncoptions object for the specified syncName
	 */
	protected ISyncOptions getSyncOptions(final String syncName) {
		ISyncOptions syncOptions = SyncOptionsFactory.getInstance(syncName);

		if (syncOptions == null) {
			if ((syncName == null) || (syncName.length() == 0)) {
				LOGGER.info("No SyncOptions configuration. Defaulting to Force policy ...");
			} else {
				LOGGER.warn("Unknown '{}' synchronization task name. Defaulting to Force policy ...", syncName);
			}
			syncOptions = new ForceSyncOptions();
		}

		return syncOptions;
	}
}
