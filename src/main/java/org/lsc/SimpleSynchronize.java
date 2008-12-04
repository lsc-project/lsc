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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.lsc.beans.AbstractBean;
import org.lsc.jndi.IJndiDstService;
import org.lsc.objects.top;
import org.lsc.service.ISrcService;

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

    /** lsc prefix. */
    public static final String LSC_PROPS_PREFIX = "lsc";

    /** lsc.tasks property. */
    public static final String TASKS_PROPS_PREFIX = "tasks";

    /** lsc.tasks.TASKNAME.type property. */
    public static final String TYPE_PROPS_PREFIX = "type";

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

    /** The local LOG4J Logger. */
    private static final Logger LOGGER = 
        Logger.getLogger(SimpleSynchronize.class);

    /** The lsc properties. */
    private Properties lscProperties;

    /**
     * Main method Check properties, and for each task, launch the
     * synchronization and the cleaning phases.
     * 
     * @param syncTasks
     *                string list of the synchronization tasks to launch
     * @param cleanTasks
     *                string list of the cleaning tasks to launch
     * 
     * @return the launch status
     * @throws Exception 
     */
    public final boolean launch(final List<String> syncTasks, 
            final List<String> cleanTasks) throws Exception {
        Boolean foundATask = false;

        // Get the "lsc" prefixed properties
        lscProperties = Configuration.getAsProperties(LSC_PROPS_PREFIX);

        if (lscProperties == null) {
            LOGGER.fatal("Unable to get LSC properties ! Exiting ...");
            return false;
        }

        // Get the "lsc.tasks" property
        String tasks = lscProperties.getProperty(TASKS_PROPS_PREFIX);

        if (tasks == null) {
            LOGGER.fatal("Unable to get tasks in LSC properties ! Exiting ...");
            return false;
        }

        // Iterate on each task
        StringTokenizer tasksSt = new StringTokenizer(tasks, ",");

        while (tasksSt.hasMoreTokens()) {
            String taskName = tasksSt.nextToken();

            // Get the task properties
            String taskType = lscProperties.getProperty(TASKS_PROPS_PREFIX
                    + "." + taskName + "." + TYPE_PROPS_PREFIX);
            TaskType task = null;
            try {
                task = TaskType.valueOf(taskType.toLowerCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error("Missing '" + taskName + "' task parameter !");
                return false;
            }

            // Launch the task either if explicitly
            // specified or if "all" magic keyword used
            boolean isSyncTask = syncTasks.contains(taskName.toString());
            boolean isSyncTaskAll = syncTasks.contains(ALL_TASKS_KEYWORD);
            boolean isCleanTask = cleanTasks.contains(taskName.toString());
            boolean isCleanTaskAll = cleanTasks.contains(ALL_TASKS_KEYWORD);
            
            if (isSyncTask || isSyncTaskAll) {
                foundATask = true;
                if (!launchTask(task, taskName, TaskMode.sync)) {
                    return false;
                }
            }
            if(isCleanTask || isCleanTaskAll) {
                foundATask = true;
                if (!launchTask(task, taskName, TaskMode.clean)) {
                    return false;
                }
            }
        }

        if (!foundATask) {
            LOGGER.fatal("No specified tasks could be launched! Check spelling and that they exist in the configuration file.");
            return false;
        }

        return true;
    }
    
    /**
     * Enum for the type of mode
     *
     */
    private enum TaskMode {
        clean,
        sync;
    }
    

    /**
     * Launch the tasks.
     * 
     * @param taskType
     *                the task type (db2ldap, ldap2ldap, ...)
     * @param taskName
     *                the task name (generally the LDAP object class name,
     *                but every time)
     *  @param taskMode
     *                  the mode of the task (clean or sync)
     * 
     * @return the cleaning status
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    private boolean launchTask(final TaskType taskType, final String taskName, final TaskMode taskMode) throws Exception {
        try {
            String prefix = TASKS_PROPS_PREFIX + "." + taskName + ".";

            // Get all properties
            String objectClassName = lscProperties.getProperty(prefix + OBJECT_PROPS_PREFIX);
            String beanClassName = lscProperties.getProperty(prefix + BEAN_PROPS_PREFIX);
            Properties dstServiceProperties = Configuration.getAsProperties(LSC_PROPS_PREFIX + "." + prefix + DSTSERVICE_PROPS_PREFIX);
            String dstServiceClass = lscProperties.getProperty(prefix + DSTSERVICE_PROPS_PREFIX);

            Constructor<?> constr = Class.forName(dstServiceClass).getConstructor(new Class[] { Properties.class, String.class });
            
            String customLibraryName = lscProperties.getProperty(prefix + CUSTOMLIBRARY_PROPS_PREFIX);
            Object customLibrary = null;
            if (customLibraryName != null) {
                customLibrary = Class.forName(customLibraryName).newInstance();
            }
            
            top taskObject = null;
            Class<? extends AbstractBean> taskBean = null;
            if(taskMode == TaskMode.sync) {
                //Common objects
                taskObject = (top) Class.forName(objectClassName).newInstance();
                taskBean = (Class<? extends AbstractBean>) Class.forName(beanClassName);
            }
            
            IJndiDstService dstJndiService = (IJndiDstService) constr.newInstance(new Object[] { dstServiceProperties, beanClassName });
            
            switch(taskType) {
                case ldap2ldap :
                    Properties srcServiceProperties = Configuration.getAsProperties(LSC_PROPS_PREFIX + "."
                            + prefix + SRCSERVICE_PROPS_PREFIX);
                    
                    String srcJndiServiceClass = lscProperties.getProperty(prefix + SRCSERVICE_PROPS_PREFIX);
                    Constructor<?> constrSrcJndiService = Class.forName(srcJndiServiceClass).getConstructor(new Class[] { Properties.class, String.class });
                    ISrcService srcJndiService = (ISrcService) constrSrcJndiService.newInstance(new Object[] { srcServiceProperties, objectClassName });

                    switch(taskMode) {
                        case clean:
                            cleanLdap2Ldap(taskName, srcJndiService, dstJndiService);
                            break;
                        case sync:
                            synchronize2Ldap(taskName, srcJndiService, dstJndiService, taskObject, taskBean, customLibrary);
                            break;
                        default :
                            //Should not happen
                            LOGGER.error("Unknown task mode type " + taskMode.toString());
                            return false;
                    }
                    break;
                case db2ldap :
                    String srcServiceClass = lscProperties.getProperty(prefix   + SRCSERVICE_PROPS_PREFIX);
                    ISrcService jdbcService = (ISrcService) Class.forName(srcServiceClass).newInstance();
                    
                    switch(taskMode) {
                        case clean:
                            cleanDb2Ldap(taskName, jdbcService, dstJndiService);
                            break;
                        case sync:
                            synchronize2Ldap(taskName, jdbcService, dstJndiService, taskObject, taskBean, customLibrary);
                            break;
                        default :        
                            //Should not happen
                            LOGGER.error("Unknown task mode type " + taskMode.toString());
                            return false;
                    }
                    break;
                default :
                    LOGGER.warn("Unknown task type : " + taskType + " (must be one of ldap2ldap or db2ldap)");
                    return false;
            }
            
            // Manage exceptions
        } catch (Exception e) {
            Class<?>[] exceptionsCaught = {InstantiationException.class, IllegalAccessException.class,
                    ClassNotFoundException.class, SecurityException.class, NoSuchMethodException.class,
                    IllegalArgumentException.class, InvocationTargetException.class};

            if (ArrayUtils.contains(exceptionsCaught, e.getClass())) {
                LOGGER.fatal("Error while launching the following task: "
                        + taskName + ". Please check your code ! (" + e + ")", e);
                return false;
            }
            else {
                throw e;
            }
        }

        return true;
    }

}
