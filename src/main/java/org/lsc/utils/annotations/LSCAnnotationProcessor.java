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
package org.lsc.utils.annotations;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.directory.api.util.Strings;
import org.lsc.configuration.ConditionsType;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.ConnectionsType;
import org.lsc.configuration.DatabaseConnectionType;
import org.lsc.configuration.DatasetType;
import org.lsc.configuration.EncryptionType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdapDestinationServiceType;
import org.lsc.configuration.LdapSourceServiceType;
import org.lsc.configuration.Lsc;
import org.lsc.configuration.PluginConnectionType;
import org.lsc.configuration.PropertiesBasedSyncOptionsType;
import org.lsc.configuration.SecurityType;
import org.lsc.configuration.TaskType;
import org.lsc.configuration.TasksType;
import org.lsc.configuration.ValuesType;
import org.lsc.configuration.ServiceType.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process the LSC annotations to create an LSC instance.
 */
public class LSCAnnotationProcessor {
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LSCAnnotationProcessor.class );

    
    /**
     * Create a Plugin connection
     * 
     * @param pluginConnectionType The Plugin connection instance
     * @param createConnection The configuration for a Plugin connection
     */
    private static void createPluginConnection(PluginConnectionType pluginConnectionType, 
            CreateConnection createConnection) {
        pluginConnectionType.setConfigurationClass(createConnection.configurationClass());
        pluginConnectionType.setImplementationClass(createConnection.implementationClass());

        LOG.debug("Created the {} Plugin connection", createConnection.name() );
    }
    
    /**
     * Create a Database connection
     * 
     * @param databaseConnectionType The Database connection instance
     * @param createConnection The configuration for a Database connection
     */
    private static void createDatabaseConnection(DatabaseConnectionType databaseConnectionType, 
            CreateConnection createConnection) {
        databaseConnectionType.setDriver(createConnection.driver());

        LOG.debug("Created the {} Ldap connection", createConnection.name() );
    }

    
    /**
     * Create a Ldap connection
     * 
     * @param ldapConnectionType The LDAP connection instance
     * @param createConnection The configuration for a LDAP connection
     */
    private static void createLdapConnection(LdapConnectionType ldapConnectionType, 
            CreateConnection createConnection) {

        // From the ConnectionType
        ldapConnectionType.setId( createConnection.id() );
        ldapConnectionType.setName( createConnection.name() );
        ldapConnectionType.setUrl( createConnection.url() );
        ldapConnectionType.setUsername( createConnection.username() );
        ldapConnectionType.setPassword( createConnection.password() );

        // From the LdapConnectionType
        ldapConnectionType.setAuthentication(createConnection.authenticationType());
        ldapConnectionType.setReferral(createConnection.referral());
        ldapConnectionType.setDerefAliases(createConnection.derefAliases());
        ldapConnectionType.setVersion(createConnection.version());
        ldapConnectionType.setPageSize(createConnection.pageSize());
        ldapConnectionType.setFactory(createConnection.factory());
        ldapConnectionType.setTlsActivated(createConnection.tlsActivated());
        ldapConnectionType.setSaslMutualAuthentication(createConnection.saslMutualAuthentication());

        if ( Strings.isNotEmpty(createConnection.sortedBy())) {
            ldapConnectionType.setSortedBy(createConnection.sortedBy());
        }
        
        ldapConnectionType.setBinaryAttributes(null);
        ldapConnectionType.setRecursiveDelete(createConnection.recursiveDelete());
        ldapConnectionType.setRelaxRules( createConnection.relaxRules() );
        ldapConnectionType.setSaslQop(createConnection.saslQop());
        
        LOG.debug("Created the {} Ldap connection", createConnection.name() );
    }
    
    /**
     * Create the connections. We may have multiple flavors of connections:
     * <ul>
     *   <li>databaseConnectionType: A Database connection</li>
     *   <li>ldapConnectionType: a LDAP connection</li>
     *   <li>pluginConnectionType: A plugin connection</li>
     * </ul>
     * 
     * @param createConnections The connection annotations
     * @param connectionsType The place where to store the created connectionType instances
     */
    private static void createConnections(Lsc lsc, CreateConnection[] createConnections) {
        // The list of connections
        ConnectionsType connectionsType = new ConnectionsType();
        List<ConnectionType> lscConnections = 
                connectionsType.getLdapConnectionOrDatabaseConnectionOrPluginConnection();

        for ( CreateConnection createConnection: createConnections ) {
            Class<? extends ConnectionType> type = createConnection.type();
            
            try {
                ConnectionType connectionType = type.getDeclaredConstructor().newInstance();
                
                if (connectionType instanceof LdapConnectionType) {
                    // LDAP connection
                    createLdapConnection((LdapConnectionType)connectionType, createConnection);

                    lscConnections.add(connectionType);
                    LOG.debug("Created the {} Ldap connection", createConnection.name() );
                } else if (connectionType instanceof DatabaseConnectionType) {
                    // Database connection
                    createDatabaseConnection((DatabaseConnectionType)connectionType, createConnection);

                    lscConnections.add(connectionType);
                    LOG.debug("Created the {} Database connection", createConnection.name() );
                } else if (connectionType instanceof PluginConnectionType) {
                    // Plugin connection
                    createPluginConnection((PluginConnectionType)connectionType, createConnection);

                    lscConnections.add(connectionType);
                    LOG.debug("Created the {} Plugin connection", createConnection.name() );
                }
            } catch ( InstantiationException |  IllegalAccessException | NoSuchMethodException |
                    IllegalArgumentException | InvocationTargetException e) {
            
                // Todo...
                LOG.error("The Ldap connection {} cannot be created: {}", createConnection.name(), e.getMessage() );
            }
        }
        
        // Inject them in the LSC instance
        lsc.setConnections(connectionsType);
    }
    
    
    /**
     * Create a ValuesType instance from an array of values
     */
    private static ValuesType setValues( String... vals ) {
        ValuesType valuesType = new ValuesType();
        
        List<String> values = valuesType.getString();
        
        for ( String val:vals ) {
            values.add(val);
        }
        
        return valuesType;
    }

    /**
     * Create the LdapSourceService instance, connect it with the connection
     */
    private static void createLdapSourceService(Lsc lsc, TaskType taskType, 
            CreateLdapSourceService createLdapSourceService) {
        // Ok, we have a LdapSourceService, create an instance
        LdapSourceServiceType ldapSourceService = new LdapSourceServiceType();

        ldapSourceService.setId(createLdapSourceService.id());
        ldapSourceService.setName(createLdapSourceService.name());
        ldapSourceService.setBaseDn(createLdapSourceService.baseDn());
        ldapSourceService.setPivotAttributes(
                setValues(createLdapSourceService.pivotAttributes()));
        if ( Strings.isNotEmpty(createLdapSourceService.allFilter() ) ) {
        
            ldapSourceService.setAllFilter(createLdapSourceService.allFilter());
        }

        if ( Strings.isNotEmpty(createLdapSourceService.oneFilter() ) ) {
        }
            ldapSourceService.setOneFilter(createLdapSourceService.oneFilter());

        if ( Strings.isNotEmpty(createLdapSourceService.cleanFilter() ) ) {
            ldapSourceService.setCleanFilter(createLdapSourceService.cleanFilter()); 
        }
        ldapSourceService.setDateFormat(createLdapSourceService.dateFormat());
        ldapSourceService.setFetchedAttributes(
                setValues(createLdapSourceService.fetchedAttributes()));
        ldapSourceService.setFilterAsync(createLdapSourceService.filterAsync());
        ldapSourceService.setInterval(createLdapSourceService.interval());

        // Attach the connections
        String connectionName = createLdapSourceService.connectionRef();
        
        for ( ConnectionType connectionType:
            lsc.getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection()) {
            if ( connectionType.getName().equals(connectionName)) {
                Connection connection = new Connection();
                connection.setReference(connectionType);

                ldapSourceService.setConnection(connection);
                taskType.setLdapSourceService(ldapSourceService);
            }
        }
    }
    
    /**
     * Create the LdapDestinationService instance, connect it with the connection
     */
    private static void createLdapDestinationService(Lsc lsc, TaskType taskType, CreateTask task) {
        LdapDestinationServiceType ldapDestinationService = new LdapDestinationServiceType();
        
        CreateLdapDestinationService createLdapDestinationService = task.ldapDestinationService()[0];
        
        ldapDestinationService.setId(createLdapDestinationService.id());
        ldapDestinationService.setName(createLdapDestinationService.name());
        ldapDestinationService.setBaseDn(createLdapDestinationService.baseDn());
        ldapDestinationService.setPivotAttributes(
                setValues(createLdapDestinationService.pivotAttributes()));
        ldapDestinationService.setAllFilter(createLdapDestinationService.allFilter());
        ldapDestinationService.setOneFilter(createLdapDestinationService.oneFilter());
        ldapDestinationService.setFetchedAttributes(
                setValues(createLdapDestinationService.fetchedAttributes()));

        // Attach the connection
        String connectionName = createLdapDestinationService.connectionRef();
        
        for ( ConnectionType connectionType:
            lsc.getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection()) {
            if ( connectionType.getName().equals(connectionName)) {
                Connection connection = new Connection();
                connection.setReference(connectionType);

                ldapDestinationService.setConnection(connection);
                taskType.setLdapDestinationService(ldapDestinationService);
            }
        }
    }
    
    /**
     * Create the task's services. We will have a source and a destination service.
     * 
     * We have four source service flavors:
     * 
     * <ul>
     *   <li>asyncLdapSourceService</li>
     *   <li>databaseSourceService</li>
     *   <li>ldapSourceService</li>
     *   <li>pluginSourceService</li>
     * </ul>
     * 
     * We have five destination service flavors:
     * 
     * <ul>
     *   <li>databaseDestinationService</li>
     *   <li>ldapDestinationService</li>
     *   <li>multiDestinationService</li>
     *   <li>pluginDestinationService</li>
     *   <li>XaFileDestinationService</li>
     * </ul>
     * 
     * @param lsc The LSC instance
     * @param taskType The TaskType instance to fill
     * @param task The task instance
     */
    private static void createServices(Lsc lsc, TaskType taskType, CreateTask task) {
        // Check which kind of source service we have:
        // * asyncLdapSourceService
        // * databaseSourceService
        // * ldapSourceService
        // * pluginSourceService
        if (task.ldapSourceService().length > 0) {
            // Ok, we have a LdapSourceService, create an instance
            createLdapSourceService(lsc, taskType, task.ldapSourceService()[0]);
        } /* else if (task.databaseSourceService().length > 0) {
        } else if (task.pluginSourceService().length > 0) {
        } */
        
        // Todo: deal with databaseSourceService/pluginSourceService
        
        // Same thing for the destination services:
        // * databaseDestinationService
        // * ldapDestinationService
        // * multiDestinationService
        // * pluginDestinationService
        // * XaFileDestinationService
        if (task.ldapDestinationService().length != 0) {
            // Ok, we have a LdapSourceService, create an instance
            createLdapDestinationService(lsc, taskType, task);
        } /* else if (task.databaseDatabaseService().length > 0) {
        } else if (task.pluginDestinationService().length > 0) {
        } else if (task.multiDestinationService().length > 0) {
        } else if (task.xaFileDestinationService().length > 0) {
        } */
        
        // Now, deal with the syncOptions
        
    }
    
    /**
     * Create the ValuesType instance
     * 
     * @param createValuesType The read configuration
     * @return The created ValuesType instance
     */
    private static ValuesType setValues(CreateValuesType createValuesType) {
        ValuesType valuesType = new ValuesType();
        
        if (Strings.isNotEmpty(createValuesType.id())) {
            valuesType.setId( createValuesType.id() );
        }
        
        List<String> values = valuesType.getString();
        
        for (String value:createValuesType.string()) {
            values.add(value);
        }

        return valuesType;
    }

    /**
     * Create the task's property based syncOptions.
     * 
     * @param lsc The LSC instance 
     * @param task The task annotation to process
     */
    private static void createPropertyBasedSyncOptions(Lsc lsc, 
            TaskType taskType, CreatePropertiesBasedSyncOptions properties) {
        PropertiesBasedSyncOptionsType syncOptions = new PropertiesBasedSyncOptionsType();
        
        // The main identifier
        syncOptions.setMainIdentifier(properties.mainIdentifier());
        
        // The default policy
        syncOptions.setDefaultPolicy(properties.defaultPolicy());
        
        // The pivotTransformation, if any
        if (Strings.isNotEmpty(properties.pivotTransformation())) {
            syncOptions.setPivotTransformation(null);
        }
        
        // The default delimiter, if any
        if (Strings.isNotEmpty(properties.defaultDelimiter())) {
            syncOptions.setDefaultDelimiter(properties.defaultDelimiter());
        }

        CreateConditions[] conditions = properties.conditions();

        if ( conditions != null ) {
            for ( CreateConditions condition : conditions) {
                ConditionsType conditionsType = new ConditionsType();
                syncOptions.setConditions(conditionsType);

                // The create condition
                String create = condition.create();

                if ( Strings.isNotEmpty(create)) {
                    conditionsType.setCreate(create);
                }

                // The update condition
                String update = condition.update();

                if ( Strings.isNotEmpty(update)) {
                    conditionsType.setUpdate(update);
                }

                // The delete condition
                String delete = condition.delete();

                if ( Strings.isNotEmpty(delete)) {
                    conditionsType.setDelete(delete);
                }

                // The changeId condition
                String changeId = condition.changeId();
                if ( Strings.isNotEmpty(changeId)) {
                    conditionsType.setChangeId(changeId);
                }
            }
        }
 
        // The datasets
        CreateDataset[] datasets = properties.dataset();
        List<DatasetType> datasetsType = syncOptions.getDataset();
        
        // Iterate on them
        for (CreateDataset dataset:datasets) {
            DatasetType datasetType = new DatasetType();
            datasetType.setName(dataset.name());
            
            // The policy, either explicit or inherited
            if (dataset.policy().length == 0) {
                datasetType.setPolicy(syncOptions.getDefaultPolicy());
            } else {
                datasetType.setPolicy(dataset.policy()[0]);
            }

            // The default values, if any
            if (dataset.defaultValues().length > 0) {
                datasetType.setDefaultValues( setValues(dataset.defaultValues()[0]));
            }
            
            // The create values
            if (dataset.createValues().length > 0) {
                datasetType.setCreateValues( setValues(dataset.createValues()[0]));
            }
            
            // The force values
            if (dataset.forceValues().length > 0) {
                datasetType.setForceValues( setValues(dataset.forceValues()[0]));
            }

            // Finally add it to the list of datasets
            datasetsType.add(datasetType);
        }

        taskType.setPropertiesBasedSyncOptions(syncOptions);
    }

    /**
     * Create the task's syncOptions.
     * 
     * @param lsc The LSC instance 
     * @param task The task annotation to process
     */
    private static void createSyncOptions(Lsc lsc, TaskType taskType, CreateTask task) {
        // We may have one of PropertiesBasedSyncOptions, ForceSyncOptions or PluginSyncOptions
        CreatePropertiesBasedSyncOptions propertiesBasedSyncOptions[] = task.propertiesBasedSyncOptions();
        
        if (propertiesBasedSyncOptions.length > 0) {
            createPropertyBasedSyncOptions(lsc, taskType, propertiesBasedSyncOptions[0]);
        }
    }

    /**
     * Create the Tasks from the CreateTask annotation.
     * We won't handle the tasksType's ID.
     * 
     * @param lsc The LSC instance
     * @param tasks The tasks annotation
     */
    private static void createTasks(Lsc lsc, CreateTask[] tasks) {
        TasksType tasksType = new TasksType();
        lsc.setTasks(tasksType);

        List<TaskType> taskList = tasksType.getTask();
        
        // Iterate on the read tasks
        for ( CreateTask createTask:tasks) {
            TaskType taskType = new TaskType();

            // The ID, name and bean parts
            taskType.setId(createTask.id());
            taskType.setName(createTask.name());
            taskType.setBean(createTask.bean());
            
            // The hooks
            if ( Strings.isNotEmpty(createTask.cleanHook())) {
                taskType.setCleanHook(createTask.cleanHook());
            }

            if ( Strings.isNotEmpty(createTask.syncHook())) {
                taskType.setSyncHook(createTask.syncHook());
            }
            
            // Process the source and destination services
            createServices(lsc, taskType, createTask);
            
            // Process the errors
            taskType.setErrorIfEmptySource(createTask.errorIfEmptySource());
            taskType.setErrorIfEmptyDestination(createTask.errorIfEmptyDestination());
            
            // Process the sync options (either propertyBased, force or plugin
            createSyncOptions(lsc, taskType, createTask);
            
            // The custom library, if any
            if (createTask.customLibrary().length > 0) {
                taskType.setCustomLibrary(setValues(createTask.customLibrary()[0]));
            }

            // The script include, if any
            if (createTask.scriptInclude().length > 0) {
                taskType.setScriptInclude(setValues(createTask.scriptInclude()[0]));
            }
            
            taskList.add(taskType);
        }
    }
    
    /**
     * Create the Security part from the CreateSecurity annotation
     * 
     * @param lsc The LSC instance
     * @param securities The Security annotation
     */
    private static void createSecurity(Lsc lsc, CreateSecurity[] securities) {
        if (securities.length != 0) {
            CreateSecurity security = securities[0];
            
            SecurityType securityType = new SecurityType();
            
            securityType.setId(security.id());
            
            // Create the Encryption type instance
            EncryptionType encryptionType = new EncryptionType();
            encryptionType.setKeyfile(security.keyfile());
            encryptionType.setAlgorithm(security.algorithm());
            encryptionType.setStrength(security.strength());
            
            // And inject it into the securityType instance
            securityType.setEncryption(encryptionType);
        }
    }

    /**
     * Create a LSC instance from an annotation. The @CreateLSC annotation
     * must be associated with either the method or the encapsulating class. We
     * will first try to get the annotation from the method, and if there is
     * none, then we try at the class level.
     * 
     * @return A valid LCS instance
     * @throws Exception If the LSC instance can't be returned
     */
    public static Lsc createLSC( CreateLSC lscBuilder ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Starting DS {}...", lscBuilder.id() );
        }
        
        // Create the LSC instance
        Lsc lsc = new Lsc();
        
        // The ID and revision
        lsc.setId(lscBuilder.id());
        lsc.setRevision(lscBuilder.revision());

        // The source and destination connections
        createConnections(lsc, lscBuilder.connections());
        
        // The tasks
        createTasks( lsc, lscBuilder.tasks() );

        // The security part
        createSecurity(lsc, lscBuilder.security());
        
        return lsc;
    }
}
