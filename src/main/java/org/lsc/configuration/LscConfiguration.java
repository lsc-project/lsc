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
package org.lsc.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.PropertiesBasedSyncOptions;
import org.lsc.configuration.PivotTransformationType.Transformation;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscException;
import org.lsc.jndi.PullableJndiSrcService;
import org.lsc.jndi.SimpleJndiDstService;
import org.lsc.service.MultipleDstService;
import org.lsc.service.SimpleJdbcDstService;
import org.lsc.service.SimpleJdbcSrcService;
import org.lsc.service.SyncReplSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main configuration object. It contains all reference to the other objects, including plugins and third party
 * implementations.
 * 
 * It references mainly :
 * &lt;ul&gt;
 * &lt;li&gt;the datasource connections which includes directories, database, nis, ... source and destination connectors connections&lt;/li&gt;
 * &lt;li&gt;the datasource connections which includes directories, database, nis, ... source and destination connectors connections&lt;/li&gt;
 * &lt;/ul&gt;
 * 
 * Severeals methods can be used to set and get the instance from / to an modelized configuration (properties, xml, json, ...). Modifications 
 * are logged in order to identify if the object has to be saved.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author rschermesser
 */
public class LscConfiguration {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LscConfiguration.class);
	
	private ArrayList<ConnectionType> connections;
	
	private ArrayList<AuditType> audits;
	
	private ArrayList<TaskType> tasks;

	private boolean underInitialization;

	private boolean modified;
	
	private static LscConfiguration instance;
	private static LscConfiguration original;
	
	private Lsc lscObject;
	
//	private Map<String, String> otherSettings;
	
	private int revision;
	
	private SecurityType security;
	
	static {
		instance = null;
	}
	
	public static void loadFromInstance(Lsc lscInstance) {
		if(instance == null) {
			instance = new LscConfiguration();
		} else {
			original = instance;
		} 	
		instance.lscObject = new Lsc();
		instance.lscObject.setAudits(lscInstance.getAudits());
		instance.lscObject.setConnections(lscInstance.getConnections());
		instance.lscObject.setId(lscInstance.getId());
		instance.lscObject.setRevision(lscInstance.getRevision());
		instance.lscObject.setSecurity(lscInstance.getSecurity());
		instance.lscObject.setTasks(lscInstance.getTasks());
		finalizeInitialization();
	}
	
	public static LscConfiguration getInstance() {
		if(instance == null) {
			org.lsc.Configuration.setUp();
//			// Force static instantiation
//			instance = new LscConfiguration();
//			instance.lscObject = new Lsc();
		}
		return instance;
	}

	private LscConfiguration() {
		underInitialization = true;
		modified = false;
		connections = new ArrayList<ConnectionType>();
		audits = new ArrayList<AuditType>();
		tasks = new ArrayList<TaskType>();
//		otherSettings = new HashMap<String, String>();
		revision = 0;
	}

	public static Collection<AuditType> getAudits() {
		return Collections.unmodifiableCollection(getInstance().getLsc().getAudits().getCsvAuditOrLdifAuditOrPluginAudit());
	}

	public static AuditType getAudit(String name) {
        if(getInstance().getLsc().getAudits() != null) {
    		for(AuditType audit: getInstance().getLsc().getAudits().getCsvAuditOrLdifAuditOrPluginAudit()) {
    			if(audit.getName().equalsIgnoreCase(name)) {
    				return audit;
    			}
    		}
        }
		return null;
	}

	public static Collection<ConnectionType> getConnections() {
		List<ConnectionType> connectionsList = new ArrayList<ConnectionType>();
		if(getInstance().getLsc().getConnections() != null) {
	        connectionsList.addAll(getInstance().getLsc().getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection());
		}
		return Collections.unmodifiableCollection(connectionsList);
	}

	public static ConnectionType getConnection(String name) {
		for(ConnectionType connection: getConnections()) {
			if(connection.getName().equalsIgnoreCase(name)) {
				return connection;
			}
		}
		return null;
	}

	public static TaskType getTask(String name) {
		for(TaskType task: getInstance().getLsc().getTasks().getTask()) {
			if(task.getName().equalsIgnoreCase(name)) {
				return task;
			}
		}
		return null;
	}
	
	public static Collection<TaskType> getTasks() {
		return Collections.unmodifiableCollection(getInstance().getLsc().getTasks().getTask());
	}
	
	public static void addTask(TaskType task) {
		logModification(task);
		getInstance().getLsc().getTasks().getTask().add(task);
	}
	
	public static void removeTask(TaskType task) {
		logModification(task);
		getInstance().getLsc().getTasks().getTask().remove(task);
	}
	
	public static void addConnection(ConnectionType connection) {
		logModification(connection);
		getInstance().getLsc().getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection().add(connection);
	}

	public static void removeConnection(ConnectionType connection) {
		logModification(connection);
		getInstance().getLsc().getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection().remove(connection);
	}

	public static void addAudit(AuditType audit) {
		logModification(audit);
		getInstance().getLsc().getAudits().getCsvAuditOrLdifAuditOrPluginAudit().add(audit);
	}

	public static void removeAudit(AuditType audit) {
		logModification(audit);
		getInstance().getLsc().getAudits().getCsvAuditOrLdifAuditOrPluginAudit().remove(audit);
	}

//	public static void reinitialize() {
//		getInstance().lscObject = new Lsc();
//	}
//	
	public static void finalizeInitialization() {
		original = instance.clone();
		getInstance().underInitialization = false;
	}
	
	@SuppressWarnings("unchecked")
	public LscConfiguration clone() {
		LscConfiguration clone = new LscConfiguration();
		clone.revision 		= revision;
		if(audits != null) {
			clone.audits 		= (ArrayList<AuditType>) audits.clone();
		}
		if(connections != null) {
			clone.connections 	= (ArrayList<ConnectionType>) connections.clone();
		}
		if(tasks != null) {
			clone.tasks 			= (ArrayList<TaskType>) tasks.clone();
		}
		if(security != null) {
			clone.security = (SecurityType) new SecurityType();
			clone.security.setId(security.getId());
			clone.security.setEncryption(new EncryptionType());
			clone.security.getEncryption().setAlgorithm(security.getEncryption().getAlgorithm());
			clone.security.getEncryption().setId(security.getEncryption().getId());
			clone.security.getEncryption().setKeyfile(security.getEncryption().getKeyfile());
			clone.security.getEncryption().setStrength(security.getEncryption().getStrength());
		}
		return clone;
	}
	
	public static void saving() {
		getInstance().getLsc().setRevision(getInstance().getLsc().getRevision()+1);
	}
	
	public static void saved() {
		finalizeInitialization();
		getInstance().modified = false;
	}
	
	public static void logModification(Object o) {
		if(!getInstance().underInitialization) {
			// Store updates
			getInstance().modified = true;
		}
	}
	
	public boolean isModified() {
		return modified;
	}
	
	public static void revertToInitialState() {
		instance.audits = original.audits;
		instance.connections = original.connections;
		instance.tasks = original.tasks;
		instance.security = original.security;
		instance.audits = original.audits;
		instance.revision = original.revision;
		instance.modified = false;
	}

	public static boolean isInitialized() {
		return instance != null && !instance.underInitialization && instance.lscObject != null;
	}
	
	public void setConnections(List<ConnectionType> conns) {
		getInstance().getLsc().getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection().clear();
		for(ConnectionType conn : conns) {
			logModification(conn);
			getInstance().getLsc().getConnections().getLdapConnectionOrDatabaseConnectionOrPluginConnection().add(conn);
		}
	}
	
	public int getRevision() {
		return (getLsc().getRevision() != null ? getLsc().getRevision().intValue() : -1); 
	}
	
	public static SecurityType getSecurity() {
		return getInstance().getLsc().getSecurity();
	}

	public void setSecurity(SecurityType sec) {
		getLsc().setSecurity(sec);
	}

	public void validate() throws LscException {
		// Tasks will check used audits and connections
		for(TaskType task: getTasks()) {
			validate(task);
		}
		if(security != null) {
			validate(security);
		}
	}

	private static void validate(SecurityType security2) {
		// TODO Auto-generated method stub
		
	}

	private static void validate(TaskType task) {
		// TODO Auto-generated method stub
		
	}

	public Lsc getLsc() {
		return lscObject;
	}

	public static SyncOptionsType getSyncOptions(TaskType task) {
		if(task.getPropertiesBasedSyncOptions() != null) {
			return task.getPropertiesBasedSyncOptions();
		} else if(task.getForceSyncOptions() != null) {
			return task.getForceSyncOptions();
		} else if(task.getPluginSyncOptions() != null){
			return task.getPluginSyncOptions();
		}
		return null;
	}

	public static DatasetType getDataset(PropertiesBasedSyncOptionsType conf,
			String attributeName) {
		if(conf != null && conf.getDataset() != null) {
			for(DatasetType dataset : conf.getDataset()) {
				if(dataset.getName().equalsIgnoreCase(attributeName)) {
					return dataset;
				}
			}
		}
		DatasetType attr = new DatasetType();
		attr.setName(attributeName);
		attr.setPolicy(conf.getDefaultPolicy());
		//conf.getDataset().add(attr);
		return attr;
	}	

	public static ServiceType getDestinationService(TaskType t) {
		if(t.getLdapDestinationService() != null) {
			return t.getLdapDestinationService();
		} else if (t.getDatabaseDestinationService() != null) {
			return t.getDatabaseDestinationService();
		} else if (t.getMultiDestinationService() != null) {
			return t.getMultiDestinationService();
		} else if (t.getPluginDestinationService() != null) {
			return t.getPluginDestinationService();
		}
		return null;
	}

	public static ServiceType getSourceService(TaskType t) {
		if(t.getAsyncLdapSourceService() != null) {
			return t.getAsyncLdapSourceService();
		} else if (t.getLdapSourceService() != null) {
			return t.getLdapSourceService();
		} else if (t.getDatabaseSourceService() != null) {
			return t.getDatabaseSourceService();
		} else if (t.getPluginSourceService() != null) {
			return t.getPluginSourceService();
		}
		return null;
	}

    public static void setSourceService(TaskType t, ServiceType s) throws LscConfigurationException {
        if(s instanceof DatabaseSourceServiceType) {
            t.setDatabaseSourceService((DatabaseSourceServiceType) s);
        } else if (s instanceof LdapSourceServiceType) {
            t.setLdapSourceService((LdapSourceServiceType) s);
        } else if (s instanceof PluginSourceServiceType) {
            t.setPluginSourceService((PluginSourceServiceType) s);
        } else  {
            throw new LscConfigurationException("Unable to map source service type to a known type: " + s.getClass().getName());
        }
    }

    public static void setDestinationService(TaskType t, ServiceType s) throws LscConfigurationException {
        if(s instanceof DatabaseDestinationServiceType) {
            t.setDatabaseDestinationService((DatabaseDestinationServiceType) s);
        } else if (s instanceof XaFileDestinationServiceType) {
            t.setXaFileDestinationService((XaFileDestinationServiceType) s);
        } else if (s instanceof MultiDestinationServiceType) {
            t.setMultiDestinationService((MultiDestinationServiceType) s);
        } else if (s instanceof LdapDestinationServiceType) {
            t.setLdapDestinationService((LdapDestinationServiceType) s);
        } else if (s instanceof PluginDestinationServiceType) {
            t.setPluginDestinationService((PluginDestinationServiceType) s);
        } else  {
            throw new LscConfigurationException("Unable to map destination service type to a known type: " + s.getClass().getName());
        }
    }

	public static Class<?> getServiceImplementation(ServiceType service) {
		if(service instanceof LdapDestinationServiceType) {
			return SimpleJndiDstService.class;
        } else if (service instanceof AsyncLdapSourceServiceType) {
            return SyncReplSourceService.class;
		} else if (service instanceof LdapSourceServiceType) {
			return PullableJndiSrcService.class;
		} else if (service instanceof DatabaseDestinationServiceType) {
			return SimpleJdbcDstService.class;
		} else if (service instanceof DatabaseSourceServiceType) {
			return SimpleJdbcSrcService.class;
		} else if (service instanceof MultiDestinationServiceType) {
			return MultipleDstService.class;
		} else if (service instanceof PluginDestinationServiceType) {
			try {
				return Class.forName(((PluginDestinationServiceType) service).getImplementationClass());
			} catch (ClassNotFoundException e) {
				throw new UnsupportedOperationException("Unknown plugin implementation: " + ((PluginDestinationServiceType) service).getImplementationClass());
			}
        } else if (service instanceof PluginSourceServiceType) {
            try {
                return Class.forName(((PluginSourceServiceType) service).getImplementationClass());
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("Unknown plugin implementation: " + ((PluginSourceServiceType) service).getImplementationClass());
            }
		} else {
			throw new UnsupportedOperationException("Unknown service type: " + service.getClass().getName());
		}
	}

	public static Class<?> getSyncOptionsImplementation(SyncOptionsType syncOptions) {
		if(syncOptions instanceof PropertiesBasedSyncOptionsType) {
			return PropertiesBasedSyncOptions.class;
		} else if (syncOptions instanceof ForceSyncOptionsType) {
			return ForceSyncOptions.class;
		} else if (syncOptions instanceof PluginSyncOptionsType) {
			try {
				return Class.forName(((PluginSyncOptionsType) syncOptions).getImplementationClass());
			} catch (ClassNotFoundException e) {
				throw new UnsupportedOperationException("Unknown plugin implementation: " + ((PluginSyncOptionsType) syncOptions).getImplementationClass());
			}
		} else {
			throw new UnsupportedOperationException("Unknown syncoptions type: " + syncOptions.getClass().getName());
		}
	}

	public static void loadSyncOptions(TaskType newTask, String taskName,
			Properties props) throws LscConfigurationException {
		
		SyncOptionsType sot = LscConfiguration.getSyncOptions(newTask);
		
		if(sot instanceof ForceSyncOptionsType) {
			return; 
		} else if(!(sot instanceof PropertiesBasedSyncOptionsType)) {
			throw new LscConfigurationException("Conversion from old properties configuration file format is not supported for this type of SyncOptions: " + sot.getClass().getName());
		}
		PropertiesBasedSyncOptionsType pbsot = (PropertiesBasedSyncOptionsType) sot;
		Enumeration<Object> en = props.keys();

		// temporary cache to store values read from properties
		Map<String, String> defaultValueStrings = new HashMap<String, String>();
		Map<String, String> createValueStrings = new HashMap<String, String>();
		Map<String, String> forceValueStrings = new HashMap<String, String>();
		
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			if (key.equals("")) {
				continue;
			}
			String value = props.getProperty(key);
			StringTokenizer stok = new StringTokenizer(key, ".");
			if (stok.countTokens() != 2) {
				LOGGER.error("Unable to use invalid name : lsc.{}.{} ! Bypassing ...", taskName, key);
				continue;
			}
			String datasetName = stok.nextToken();
			String typeName = stok.nextToken();
			DatasetType dataset = getDataset(pbsot, datasetName);
			if (typeName.equalsIgnoreCase("action")) {
				PolicyType policy = parseSyncType(value);
				LOGGER.debug("Adding '{}' sync type for dataset name {}.", value, datasetName);
				if (datasetName.equalsIgnoreCase("default")) {
					pbsot.setDefaultPolicy(policy);
				} else {
					dataset.setPolicy(policy);
				}
			} else if (typeName.equalsIgnoreCase("default_value")) {
				defaultValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("create_value")) {
				createValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("force_value")) {
				forceValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("delimiter")) {
				if (value.length() > 1) {
					LOGGER.error("Invalid delimiter for {} dataset. Delimiters must be 1 character maximum. Ignoring.", datasetName);
					continue;
				}
				if (datasetName.equalsIgnoreCase("default")) {
					pbsot.setDefaultDelimiter(value);
				} else {
					dataset.setDelimiter(value);
				}
			} else {
				LOGGER.error("Unable to identify dataset option \"{}\" in this name : lsc.{}.{} ! Bypassing.",
								new Object[]{typeName, taskName, key});
				continue;
			}
		}
	}

	private static PolicyType parseSyncType(String value) throws LscConfigurationException {
		if (value.equalsIgnoreCase("K")) {
			return PolicyType.KEEP;
		} else if (value.equalsIgnoreCase("F")) {
			return PolicyType.FORCE;
		} else if (value.equalsIgnoreCase("M")) {
			return PolicyType.MERGE;
		} else {
			throw new LscConfigurationException("Unknown synchronization policy in old properties format: " + value); 
		}
	}

	public static void reset() {
		instance = null;
		original = null;
	}

    public static void setSyncOptions(TaskType task, SyncOptionsType syncOptions) throws LscConfigurationException {
        if(syncOptions instanceof ForceSyncOptionsType) {
            task.setForceSyncOptions((ForceSyncOptionsType) syncOptions);
        } else if(syncOptions instanceof PropertiesBasedSyncOptionsType) {
            task.setPropertiesBasedSyncOptions((PropertiesBasedSyncOptionsType) syncOptions);
        } else if(syncOptions instanceof PluginSyncOptionsType) {
            task.setPluginSyncOptions((PluginSyncOptionsType) syncOptions);
        } else {
            throw new LscConfigurationException("Unknown or null syncoptions type: " + syncOptions.getClass().getName()); 
        }
    }
    
    public static boolean isLdapBinaryAttribute(String attributeName) {
    	for (ConnectionType connection: getConnections()) {
    		if (connection instanceof LdapConnectionType) {
    			ValuesType binaryAttributes = ((LdapConnectionType)connection).getBinaryAttributes();
				if (binaryAttributes != null && binaryAttributes.getString().contains(attributeName)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public static List<Transformation> getPivotTransformation(TaskType task) {
		SyncOptionsType syncOptions = LscConfiguration.getSyncOptions(task);
		if (! LscConfiguration.getSyncOptionsImplementation(syncOptions).equals(PropertiesBasedSyncOptions.class)) {
			return null;
		}
		PivotTransformationType pivotTransformationType = ((PropertiesBasedSyncOptionsType)syncOptions).getPivotTransformation();
		if (pivotTransformationType == null) {
			return null;
		}
		return pivotTransformationType.getTransformation();
    }
    
    public static boolean pivotOriginMatchesFromSource(PivotOriginType pivotOrigin, boolean fromSource) {
    	if (pivotOrigin.equals(PivotOriginType.BOTH)) {
    		return true;
    	}
    	if (fromSource) {
    		return pivotOrigin.equals(PivotOriginType.SOURCE);
    	} else {
    		return pivotOrigin.equals(PivotOriginType.DESTINATION);
    	}
    }
}
	