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
import org.lsc.jndi.IJndiSrcService;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;
import org.lsc.objects.top;
import org.lsc.objects.flat.fTop;
import org.lsc.service.IJdbcSrcService;
import org.lsc.utils.I18n;
import org.lsc.utils.JScriptEvaluator;
import org.lsc.utils.LSCStructuralLogger;

/**
 * Abstract main class to derive.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @deprecated
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
     * @deprecated
     */
    protected AbstractSynchronize() {
        PropertyConfigurator.configure(this.getClass().getClassLoader()
                .getResource(LOG4J_CONFIGURATION_FILE));
        options = new Options();
        options.addOption("nc", "nocreate", false, "Don't create any entry");
        options.addOption("nu", "noupdate", false, "Don't update");
        options.addOption("nd", "nodelete", false, "Don't delete");
        options.addOption("n", "dryrun", false,
        "Don't update the directory at all");
        jmFilter = new JndiModificationsFilter();
    }

    /**
     * This method is used to clean different type of data.
     * 
     * @param jdbcService
     *                the jdbc source service
     * @param dstJndiService
     *                the jndi destination service
     * @deprecated
     */
    protected final void cleanDb2Ldap(final IJdbcSrcService jdbcService,
            final IJndiDstService dstJndiService) {
        cleanDb2Ldap(jdbcService.getClass().getName(), jdbcService,
                dstJndiService);
    }

    /**
     * This method is used to clean different type of data.
     * 
     * @param syncName
     *                the synchronization name
     * @param jdbcService
     *                the jdbc source service
     * @param dstJndiService
     *                the jndi destination service
     * @deprecated
     */
    protected final void cleanDb2Ldap(final String syncName,
            final IJdbcSrcService jdbcService,
            final IJndiDstService dstJndiService) {
        
        LOGGER.info("Starting clean for " + syncName);
        ISyncOptions syncOptions = this.getSyncOptions(syncName);

        try {
            int countAll = 0;
            int countError = 0;
            int countInitiated = 0;
            int countCompleted = 0;
            JndiModifications jm = null;
            Iterator<String> ids = dstJndiService.getIdsList();

            while (ids.hasNext()) {
                countAll++;

                String id = ids.next();
                fTop object = jdbcService.getFlatObject(id);

                try {
                    if (object == null) {
                        AbstractBean bean = dstJndiService.getBean(id);
                        jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, syncName);
                        jm.setDistinguishName(bean.getDistinguishName());

                        /* Evaluate if you have to do something */
                        Map<String, Object> table = new HashMap<String, Object>();
                        table.put("dstBean", bean);
                        String condition = syncOptions.getDeleteCondition();
                        Boolean doDelete = JScriptEvaluator.evalToBoolean(condition, table);

                        if (jm != null) {
                            countInitiated++;

                            if(doDelete) {
                                if (JndiServices.getDstInstance().apply(jmFilter.filter(jm))) {
                                    countCompleted++;
                                    logAction(jm, id, syncName);
                                } else {
                                    countError++;
                                    logActionError(jm, id, null);
                                }
                            } else {
                                logShouldAction(jm, id, syncName);
                            }
                        }
                    }
                } catch (NamingException e) {
                    LOGGER.error("Unable to clean objects (" + e.toString() + ")", e);
                    countError++;
                    logActionError(jm, id, e);
                }
            }
        } catch (NamingException e) {
            LOGGER.error("Unable to clean objects (" + e.toString() + ")", e);
        }
    }

    /**
     * Clean data objects.
     * 
     * @param srcJndiService
     *                the jndi source service
     * @param dstJndiService
     *                the jndi destination service
     * @deprecated
     */
    protected final void cleanLdap2Ldap(final IJndiSrcService srcJndiService,
            final IJndiDstService dstJndiService) {
        cleanLdap2Ldap(srcJndiService.getClass().getName(), srcJndiService,
                dstJndiService);
    }

    /**
     * Clean data objects.
     * 
     * @param syncName
     *                the synchronization name
     * @param srcJndiService
     *                the jndi source service
     * @param dstJndiService
     *                the jndi destination service
     * @deprecated
     */
    protected final void cleanLdap2Ldap(final String syncName,
            final IJndiSrcService srcJndiService,
            final IJndiDstService dstJndiService) {
        
        LOGGER.info("Starting clean for " + syncName);
        ISyncOptions syncOptions = this.getSyncOptions(syncName);

        Iterator<String> ids = null;

        try {
            ids = dstJndiService.getIdsList();
        } catch (NamingException e1) {
            LOGGER.fatal("Error getting list of IDs in the destination for task " + syncName);
            return;
        }

        if (!ids.hasNext()) {
            LOGGER.error("Empty or non existant destination (no IDs found)");
            return;
        }

        int countAll = 0;
        int countError = 0;
        int countInitiated = 0;
        int countCompleted = 0;
        JndiModifications jm = null;
        top object = null;

        while (ids.hasNext()) {
            countAll++;

            String id = ids.next();

            try {
                object = srcJndiService.getObject(id);
                if (object == null) {
                    countInitiated++;

                    AbstractBean bean = dstJndiService.getBean(id);
                    jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, syncName);
                    jm.setDistinguishName(bean.getDistinguishName());
                    
                    /* Evaluate if you have to do something */
                    Map<String, Object> table = new HashMap<String, Object>();
                    table.put("dstBean", bean);
                    String condition = syncOptions.getDeleteCondition();
                    Boolean doDelete = JScriptEvaluator.evalToBoolean(condition, table);

                    if (jm != null) {
                        countInitiated++;

                        if(doDelete) {
                            if (JndiServices.getDstInstance().apply(jmFilter.filter(jm))) {
                                countCompleted++;
                                logAction(jm, id, syncName);
                            } else {
                                countError++;
                                logActionError(jm, id, null);
                            }
                        } else {
                            logShouldAction(jm, id, syncName);
                        }
                    }
                }
            } catch (CommunicationException e) { 
                // we lost the connection to the directory
                LOGGER.fatal("Connection to directory lost! Aborting.");
                countError++;
                logActionError(jm, id, e);
                return;
            } catch (NamingException e) {
                countError++;
                LOGGER.error("Unable to delete object " + id + " (" + e.toString()	+ ")", e);
                logActionError(jm, id, e);
            }
        }

        LSCStructuralLogger.GLOBAL.info(I18n.getMessage(null,
                "org.lsc.messages.NB_CHANGES", new Object[] {
                countAll, countInitiated, countCompleted,
                countError }));
    }

    /**
     * Synchronize objects from a database to a directory.
     * 
     * @param jdbcService
     *                the source jdbc service
     * @param dstJndiService
     *                the destination jndi service
     * @param object
     *                the object to duplicate
     * @param objectBean
     *                the object bean operations to use
     * 
     * @deprecated
     */
    protected final void synchronizeDb2Ldap(final IJdbcSrcService jdbcService,
            final IJndiDstService dstJndiService, final top object,
            final Class<?> objectBean) {
        synchronizeDb2Ldap(jdbcService.getClass().getName(), jdbcService,
                dstJndiService, object, objectBean, null);
    }

    /**
     * Synchronize objects from a database to a directory.
     * 
     * @param syncName
     *                the synchronization process name
     * @param jdbcService
     *                the source jdbc service
     * @param dstJndiService
     *                the destination jndi service
     * @param object
     *                the object to duplicate
     * @param objectBean
     *                the object bean operations to use
     * @deprecated
     */
    protected final void synchronizeDb2Ldap(final String syncName,
            final IJdbcSrcService jdbcService,
            final IJndiDstService dstJndiService, final top object,
            final Class<?> objectBean, final Object customLibrary) {
        
        LSCStructuralLogger.GLOBAL.info("# Starting synchronization task " + syncName);
        ISyncOptions syncOptions = this.getSyncOptions(syncName);

        int countAll = 0;
        int countError = 0;
        int countInitiated = 0;
        int countCompleted = 0;
        Iterator<String> ids = jdbcService.getIdsList();
        JndiModifications jm = null;
        top newObject = null;

        while (ids.hasNext()) {
            jm = null;
            countAll++;

            String id = ids.next();
            LOGGER.debug("Synchronizing " + syncName + " for id=" + id);

            try {
                fTop fObj = (fTop) jdbcService.getFlatObject(id);

                if (fObj == null) {
                    countError++;
                    LOGGER.error("Unable to get object from database for id="
                            + id + " in " + syncName + " synchronize process");
                    continue;
                }

                newObject = object.getClass().newInstance();
                newObject.setUpFromObject(fObj);

                AbstractBean jdbcSt = null;

                try {
                    jdbcSt = (AbstractBean) objectBean.getMethod("getInstance",
                            new Class[] { newObject.getClass() }).invoke(null,
                                    new Object[] { newObject });
                } catch (InvocationTargetException ite) {
                    throw ite.getCause();
                }

                AbstractBean jndiSt = dstJndiService.getBean(id);
                jm = BeanComparator.calculateModifications(syncOptions, jdbcSt,
                        jndiSt, customLibrary);

                // Apply modifications to the directory
                if (jm != null) {
                    /* Evaluate if you have to do something */
                    Map<String, Object> table = new HashMap<String, Object>();
                    table.put("dstBean", jndiSt);
                    table.put("srcBean", jdbcSt);
                    String conditionString = syncOptions.getCondition(jm.getOperation());
                    Boolean condition = JScriptEvaluator.evalToBoolean(conditionString, table);
                    
                    if(condition) {
                        countInitiated++;
                        JndiModifications jmFiltered = jmFilter.filter(jm);
                        if (jmFiltered != null) {
                            if (JndiServices.getDstInstance().apply(jmFiltered)) {
                                countCompleted++;
                                logAction(jm, id, syncName);
                            } else {
                                countError++;
                                logActionError(jm, id, null);
                            }
                        }
                    } else {
                        logShouldAction(jm, id, syncName);
                    }
                }
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

        LSCStructuralLogger.GLOBAL.info("# Ended synchronization task " + syncName);
        LSCStructuralLogger.GLOBAL.info(
        		I18n.getMessage(null, "org.lsc.messages.NB_CHANGES",
        						new Object[] { countAll, countInitiated, countCompleted, countError }
        						)
				);
    }

    /**
     * Synchronize objects from a directory to a directory.
     * 
     * @param srcJndiService
     *                the source jndi service
     * @param dstJndiService
     *                the destination jndi service
     * @param object
     *                the object to duplicate
     * @param objectBean
     *                the object bean operations to use
     * @deprecated
     */
    protected final void synchronizeLdap2Ldap(
            final IJndiSrcService srcJndiService,
            final IJndiDstService dstJndiService, final top object,
            final Class<?> objectBean) {
        synchronizeLdap2Ldap("", srcJndiService, dstJndiService, object,
                objectBean, null);
    }

    /**
     * Synchronize objects from a directory to a directory.
     * 
     * @param syncName
     *                the synchronization process name
     * @param srcJndiService
     *                the source jndi service
     * @param dstJndiService
     *                the destination jndi service
     * @param object
     *                the object to duplicate
     * @param objectBean
     *                the object bean operations to use
     * @deprecated
     */
    protected final void synchronizeLdap2Ldap(final String syncName,
            final IJndiSrcService srcJndiService,
            final IJndiDstService dstJndiService, final top object,
            final Class<?> objectBean, final Object customLibrary) {
        
    	LSCStructuralLogger.GLOBAL.info("# Starting synchronization task " + syncName);
        ISyncOptions syncOptions = this.getSyncOptions(syncName);

        Iterator<String> ids = null;

        try {
            ids = srcJndiService.getIdsList();
        } catch (NamingException e1) {
            LOGGER.fatal("Unable to find any object for service "
                    + srcJndiService.getClass().getName());

            return;
        }

        if (!ids.hasNext()) {
            LOGGER.error("Empty or non existant data source : "
                    + srcJndiService);
            return;
        }

        int countAll = 0;
        int countError = 0;
        int countInitiated = 0;
        int countCompleted = 0;
        JndiModifications jm = null;
        top newObject = null;
        
        // store method to obtain source bean
        Method beanGetInstanceMethod = null;

        while (ids.hasNext()) {
            countAll++;

            String id = ids.next();
            LOGGER.debug("Synchronizing " + object.getClass().getName() + " for " + id);
            try {
                newObject = srcJndiService.getObject(id);

                if (newObject == null) {
                    countError++;
                    LOGGER.error("Unable to get object from directory for id=" + id);
                    continue;
                }

                if (beanGetInstanceMethod == null) {
                	/*
                	 * Once all old LSC installations that have Beans with a getInstance(<not top> myclass) methods
                	 * have been upgraded, we can simplify all code in this if by the following line:
                	 */
                	//beanGetInstanceMethod = objectBean.getMethod("getInstance", new Class[] { top.class })
                	
                	// get the list of all superclasses of newObject
                    HashMap<Class<?>,Object> classHierarchy = new HashMap<Class<?>,Object>();
                    Class<?> currentClass = newObject.getClass();
                    while (currentClass != null) {
                    	classHierarchy.put(currentClass, null);
                    	currentClass = currentClass.getSuperclass();
                    }
                    
                    // get all methods in the objectBean, and find the right "getInstance" method
                    Method[] methods = objectBean.getMethods();
                    Class<?>[] parameterTypes = null;
                    for (Method method : methods) {
            			if (method.getName().matches("getInstance")) {
            				parameterTypes = method.getParameterTypes();
            				if (parameterTypes.length == 1 && classHierarchy.containsKey(method.getParameterTypes()[0])) {
            					beanGetInstanceMethod = method;
            					break;
            				}
            			}
            		}
                }

                AbstractBean srcJndiSt = (AbstractBean) beanGetInstanceMethod.invoke(null, new Object[] { newObject });

                AbstractBean dstJndiSt = dstJndiService.getBean(id);
                jm = BeanComparator.calculateModifications(syncOptions,
                        srcJndiSt, dstJndiSt, customLibrary);

                // Apply modifications to the directory
                if (jm != null) {
                    /* Evaluate if you have to do something */
                    Map<String, Object> table = new HashMap<String, Object>();
                    table.put("dstBean", dstJndiSt);
                    table.put("srcBean", srcJndiSt);
                    String conditionString = syncOptions.getCondition(jm.getOperation());
                    Boolean condition = JScriptEvaluator.evalToBoolean(conditionString, table);

                    if(condition) {
                        countInitiated++;
                        JndiModifications jmFiltered = jmFilter.filter(jm);
                        if (jmFiltered != null) {
                            if (JndiServices.getDstInstance().apply(jmFiltered)) {
                                countCompleted++;
                                logAction(jm, id, syncName);
                            } else {
                                countError++;
                                logActionError(jm, id, null);
                            }
                        }
                    } else {
                        logShouldAction(jm, id, syncName);
                    }
                }
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

        LSCStructuralLogger.GLOBAL.info("# Ended synchronization task " + syncName);
        LSCStructuralLogger.GLOBAL.info(
        		I18n.getMessage(null, "org.lsc.messages.NB_CHANGES",
        						new Object[] { countAll, countInitiated, countCompleted, countError }
        						)
        		);
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
     * @deprecated
     */
    protected final void logActionError(final JndiModifications jm,
            final String identifier, final Throwable except) {
        Object str = null;

        if (except != null) {
            str = except.toString();
        } else {
            str = "";
        }

        LOGGER.error(I18n.getMessage(null,
                "org.lsc.messages.SYNC_ERROR", new Object[] {
                identifier, str, "", except }), except);

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
     * @deprecated
     */
    protected final void logAction(final JndiModifications jm, final String id,
            final String syncName) {
        switch (jm.getOperation()) {
        case ADD_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.ADD_ENTRY", new Object[] { id,
                    syncName }));

            break;

        case MODIFY_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.UPDATE_ENTRY", new Object[] {
                    id, syncName }));

            break;

        case MODRDN_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.RENAME_ENTRY", new Object[] {
                    id, syncName }));

            break;

        case DELETE_ENTRY:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.REMOVE_ENTRY", new Object[] {
                    id, syncName }));

            break;

        default:
            LSCStructuralLogger.DESTINATION.info(I18n.getMessage(null,
                    "org.lsc.messages.UNKNOWN_CHANGE", new Object[] {
                    id, syncName }));
        }

        LSCStructuralLogger.DESTINATION.info(jm);
    }

    
    /**
     * @param jm
     * @param id
     * @param syncName
     * 
     * @deprecated
     */
    protected final void logShouldAction(final JndiModifications jm, final String id,
            final String syncName) {
        switch (jm.getOperation()) {
            case ADD_ENTRY:
                LOGGER.debug("Create condition false. Should have added object " + id);
                break;
    
            case MODIFY_ENTRY:
                LOGGER.debug("Update condition false. Should have modified object " + id);
                break;
    
            case MODRDN_ENTRY:
                LOGGER.debug("ModRDN condition false. Should have renamed object " + id);
                break;
    
            case DELETE_ENTRY:
                LOGGER.debug("Delete condition false. Should have removed object " + id);
                break;
    
            default:
                LSCStructuralLogger.DESTINATION.debug(I18n.getMessage(null,
                        "org.lsc.messages.UNKNOWN_CHANGE", new Object[] {
                        id, syncName }));
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
     * @return
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
