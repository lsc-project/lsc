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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lsc.beans.AbstractBean;
import org.lsc.beans.BeanComparator;
import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.SyncOptionsFactory;
import org.lsc.jndi.IJndiDstService;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;
import org.lsc.objects.top;
import org.lsc.objects.flat.fTop;
import org.lsc.service.ISrcService;
import org.lsc.utils.I18n;
import org.lsc.utils.JScriptEvaluator;
import org.lsc.utils.LSCStructuralLogger;

/**
 * Abstract main class to derive.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractSynchronize {
    /** Log4j configuration file. */
    public static final String LOG4J_CONFIGURATION_FILE = "log4j.properties";

    /** The local LOG4J logger. */
    private static final Logger LOGGER = Logger.getLogger(AbstractSynchronize.class);

    /** The directory modifications filter. */
    private JndiModificationsFilter jmFilter;

    /** List of configured options. */
    private Options options;

    /**
     * Default constructor.
     */
    protected AbstractSynchronize() {
        //PropertyConfigurator.configure(this.getClass().getClassLoader()
        //        .getResource(LOG4J_CONFIGURATION_FILE));
        options = new Options();
        options.addOption("nc", "nocreate", false, "Don't create any entry");
        options.addOption("nu", "noupdate", false, "Don't update");
        options.addOption("nd", "nodelete", false, "Don't delete");
        options.addOption("n", "dryrun", false, "Don't update the directory at all");
        jmFilter = new JndiModificationsFilter();
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
            final ISrcService srcService,
            final IJndiDstService dstJndiService) {

    	// Get list of all entries from the destination
        Iterator<Entry<String, LscAttributes>> ids = null;
        try {
            ids = dstJndiService.getListPivots().entrySet().iterator();
        } catch (NamingException e) {
            LOGGER.fatal("Error getting list of IDs in the destination for task " + syncName);
            LOGGER.debug(e);
            return;
        }
        
        // Make sure we have at least one entry to work on
        if (!ids.hasNext()) {
            LOGGER.error("Empty or non existant destination (no IDs found)");
            return;
        }

        ISyncOptions syncOptions = this.getSyncOptions(syncName);

        int countAll = 0;
        int countError = 0;
        int countInitiated = 0;
        int countCompleted = 0;
        
        JndiModifications jm = null;
        LscObject srcObject = null;
        
        /** Hash table to pass objects into JavaScript condition */
        Map<String, Object> conditionObjects = null;
        
        // Loop on all entries in the destination and delete them if they're not found in the source
        while (ids.hasNext()) {
            countAll++;

            Entry<String, LscAttributes> id = ids.next();

            try {
                // Search for the corresponding object in the source
                srcObject = srcService.getObject(id);

                // If we didn't find the object in the source, delete it in the destination
                if (srcObject == null) {
                    jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, syncName);
                    jm.setDistinguishName(id.getKey());

                    // Retrieve condition to evaluate before deleting
                    Boolean doDelete = null;
                    String conditionString = syncOptions.getDeleteCondition();
 
                    /* Don't use JavaScript evaluator for primitive cases */
                    if (conditionString.matches("true")) {
                    	doDelete = true;
                    } else if (conditionString.matches("false")) {
                    	doDelete = false;
                    } else {
                        // If condition is based on dstBean, retrieve the full object from destination
	                    if (conditionString.contains("dstBean")) {
	                        AbstractBean bean = dstJndiService.getBean(id);

	                        // Log an error if the bean could not be retrieved! This shouldn't happen.
	                        if (bean == null) {
	                            LOGGER.error("Could not retrieve the object " + id.getKey() + " from the directory!");
	                            countError++;
	                            continue;
	                        }

	                        // Put the bean in a map to pass to JavaScript evaluator
	                        conditionObjects = new HashMap<String, Object>();
	                        conditionObjects.put("dstBean", bean);	                    	
	                    }

	                    /* Evaluate if we have to do something */
	                    doDelete = JScriptEvaluator.evalToBoolean(conditionString, conditionObjects);
                    }
                    
                    if (doDelete) {
                        countInitiated++;
                        JndiModifications jmFiltered = jmFilter.filter(jm);
                        if (jmFiltered != null) {
                            if (JndiServices.getDstInstance().apply(jmFiltered)) {
                                countCompleted++;
                                logAction(jmFiltered, id, syncName);
                            } else {
                                countError++;
                                logActionError(jmFiltered, id, null);
                            }
                        }
                    } else {
                    	// If the delete condition is false, log action for debugging purposes
                        logShouldAction(jm, id, syncName);
                    }
                }
            } catch (CommunicationException e) { 
                // we lost the connection to the source or destination, stop everything!
                countError++;
                LOGGER.fatal("Connection lost! Aborting.");
                logActionError(jm, id, e);
                return;
            } catch (NamingException e) {
                countError++;
                LOGGER.error("Unable to delete object " + id.getKey() + " (" + e.toString() + ")", e);
                logActionError(jm, id, e);
            }
        }
        
        String totalsLogMessage = I18n.getMessage(null,
                "org.lsc.messages.NB_CHANGES", new Object[] {
                countAll, countInitiated, countCompleted,
                countError });
        if (countError > 0) {
        	LSCStructuralLogger.GLOBAL.error(totalsLogMessage);
        } else {
        	LSCStructuralLogger.GLOBAL.warn(totalsLogMessage);
        }
    }

    /**
     * Synchronize the destination LDAP directory (create and update objects from source).
     * 
     * @param syncName
     *                the synchronization name
     * @param srcService
     *                the source service (JDBC or JNDI)
     * @param dstService
     *                the JNDI destination service
     * @param object
     * @param objectBean
     * @param customLibrary
     */
    protected final void synchronize2Ldap(final String syncName,
            final ISrcService srcService,
            final IJndiDstService dstService,
            final top object,
            final Class<? extends AbstractBean> objectBean,
            final Object customLibrary) {

        // Get list of all entries from the source
        Iterator<Entry<String, LscAttributes>> ids = null;
        try {
        	ids = srcService.getListPivots().entrySet().iterator();
        } catch(Exception e) {
            LOGGER.fatal("Error getting list of IDs in the source for task " + syncName);
            if (LOGGER.isDebugEnabled()) e.printStackTrace();
            return;
        }

        // Make sure we have at least one entry to work on
        if (!ids.hasNext()) {
            LOGGER.error("Empty or non existant source (no IDs found)");
            return;
        }

        int countAll = 0;
        int countError = 0;
        int countInitiated = 0;
        int countCompleted = 0;

        JndiModifications jm = null;
        top srcObject = null;
        ISyncOptions syncOptions = this.getSyncOptions(syncName);
        // store method to obtain source bean
        Method beanGetInstanceMethod = null;
        AbstractBean srcBean = null;
        AbstractBean dstBean = null;

        /** Hash table to pass objects into JavaScript condition */
        Map<String, Object> conditionObjects = null;
        
        /* Loop on all entries in the source and add or update them in the destination */
        while (ids.hasNext()) {
            countAll++;

            Entry<String, LscAttributes> id = ids.next();
            LOGGER.debug("Synchronizing " + object.getClass().getName() + " for " + id.getKey());

            try {
                LscObject lscObject = srcService.getObject(id);

                /* Log an error if the source object could not be retrieved! This shouldn't happen. */
                if(lscObject == null) {
                    countError++;
                    LOGGER.error("Unable to get object for id=" + id.getKey());
                    continue;
                }

                // Specific JDBC - transform flat object into a normal object
                if(fTop.class.isAssignableFrom(lscObject.getClass())) {
                    srcObject = object.getClass().newInstance();
                    srcObject.setUpFromObject((fTop)lscObject);
                } else {
                    // Specific LDAP
                    srcObject = (top)srcService.getObject(id);
                }

                if (beanGetInstanceMethod == null) {
                    beanGetInstanceMethod = objectBean.getMethod("getInstance", new Class[] { top.class });
                }

                try {
                    srcBean = (AbstractBean) beanGetInstanceMethod.invoke(null, new Object[] { srcObject });
                } catch (InvocationTargetException ite) {
                    throw ite.getCause();
                }

                dstBean = dstService.getBean(id);
                jm = BeanComparator.calculateModifications(syncOptions, srcBean, dstBean, customLibrary);

                // Apply any modifications to the directory
                if (jm != null) {
                    /* Retrieve condition to evaluate before creating/updating */ 
                    Boolean condition = null;
                    String conditionString = syncOptions.getCondition(jm.getOperation());

                    /* Don't use JavaScript evaluator for primitive cases */
                    if (conditionString.matches("true")) {
                    	condition = true;
                    } else if (conditionString.matches("false")) {
                    	condition = false;
                    } else {
	                    conditionObjects = new HashMap<String, Object>();
	                    conditionObjects.put("dstBean", dstBean);
	                    conditionObjects.put("srcBean", srcBean);
	                    
	                    /* Evaluate if we have to do something */
	                    condition = JScriptEvaluator.evalToBoolean(conditionString, conditionObjects);
                    }
                    
                    if(condition) {
                        countInitiated++;
                        JndiModifications jmFiltered = jmFilter.filter(jm);
                        if (jmFiltered != null) {
                            if (JndiServices.getDstInstance().apply(jmFiltered)) {
                                countCompleted++;
                                logAction(jmFiltered, id, syncName);
                            } else {
                                countError++;
                                logActionError(jmFiltered, id, null);
                            }
                        }
                    } else {
                    	/* If the condition is false, log action for debugging purposes */
                        logShouldAction(jm, id, syncName);
                    }
                }
            } catch (CommunicationException e) { 
                // we lost the connection to the source or destination, stop everything!
                countError++;
                LOGGER.fatal("Connection lost! Aborting.");
                logActionError(jm, id, e);
                return;
            } catch (RuntimeException e) {
                countError++;
                logActionError(jm, id, e);
            } catch (Exception e) {
                countError++;
                logActionError(jm, id, e);
            } catch (Throwable e) {
                countError++;
                logActionError(jm, id, e);
            }
        }

        String totalsLogMessage = I18n.getMessage(null,
                "org.lsc.messages.NB_CHANGES", new Object[] {
                countAll, countInitiated, countCompleted,
                countError });
        if (countError > 0) {
        	LSCStructuralLogger.DESTINATION.error(totalsLogMessage);
        } else {
        	LSCStructuralLogger.DESTINATION.warn(totalsLogMessage);
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
            final Entry<String, LscAttributes> identifier, final Throwable except) {
        Object str = null;

        if (except != null) {
            str = except.toString();
        } else {
            str = "";
        }

        LOGGER.error(I18n.getMessage(null,
                "org.lsc.messages.SYNC_ERROR", new Object[] {
                identifier.getKey(), str, "", except }), except);

        if (jm != null) {
            LOGGER.error(jm);
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
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.ADD_ENTRY", new Object[] { id.getKey(),
                    syncName }));

            break;

        case MODIFY_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.UPDATE_ENTRY", new Object[] {
                    id.getKey(), syncName }));

            break;

        case MODRDN_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.RENAME_ENTRY", new Object[] {
                    id.getKey(), syncName }));

            break;

        case DELETE_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.REMOVE_ENTRY", new Object[] {
                    id.getKey(), syncName }));

            break;

        default:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.UNKNOWN_CHANGE", new Object[] {
                    id.getKey(), syncName }));
        }

        LSCStructuralLogger.DESTINATION.info(jm);
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
            LSCStructuralLogger.DESTINATION.debug("Create condition false. Should have added object " + id.getKey());
            break;

        case MODIFY_ENTRY:
            LSCStructuralLogger.DESTINATION.debug("Update condition false. Should have modified object " + id.getKey());
            break;

        case MODRDN_ENTRY:
            LSCStructuralLogger.DESTINATION.debug("ModRDN condition false. Should have renamed object " + id.getKey());
            break;

        case DELETE_ENTRY:
            LSCStructuralLogger.DESTINATION.debug("Delete condition false. Should have removed object " + id.getKey());
            break;

        default:
            LSCStructuralLogger.DESTINATION.debug(I18n.getMessage(null,
                    "org.lsc.messages.UNKNOWN_CHANGE", new Object[] {
                    id.getKey(), syncName }));
        }

        LSCStructuralLogger.DESTINATION.debug(jm);
    }

    /**
     * Parse the command line arguments according the selected filter.
     * 
     * @param args
     *                the command line arguments
     * 
     * @return the parsing status
     */
    public final boolean parseOptions(final String[] args) {
        return jmFilter.parseCommandLine(args, options);
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
                LOGGER.info("No SyncOptions configuration. "
                        + "Defaulting to Force policy ...");
            } else {
                LOGGER.warn("Unknown '" + syncName
                        + "' synchronization task name. "
                        + "Defaulting to Force policy ...");
            }
            syncOptions = new ForceSyncOptions();
        }

        return syncOptions;
    }
}
