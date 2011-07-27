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
package org.lsc.configuration.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsc.configuration.objects.security.Security;
import org.lsc.exception.LscException;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

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
@XStreamAlias("lsc")
public class LscConfiguration {
	
	private ArrayList<Connection> connections;
	
	private ArrayList<Audit> audits;
	
	private ArrayList<Task> tasks;

	@XStreamOmitField
	private boolean underInitialization;

	@XStreamOmitField
	private boolean modified;
	
	private static LscConfiguration instance;
	private static LscConfiguration original;
	
	@XStreamOmitField
	private Map<String, String> otherSettings;
	
	@XStreamAsAttribute
	private int revision;
	
	private Security security;
	
	static {
		instance = new LscConfiguration();
		// Force static instantiation
		org.lsc.Configuration.isLoggingSetup();
	}

	public static void loadFromInstance(LscConfiguration original) {
		instance = original.clone();
		finalizeInitialization();
	}
	
	public static LscConfiguration getInstance() {
		return instance;
	}

	private LscConfiguration() {
		underInitialization = true;
		modified = false;
		connections = new ArrayList<Connection>();
		audits = new ArrayList<Audit>();
		tasks = new ArrayList<Task>();
		otherSettings = new HashMap<String, String>();
		revision = 0;
	}

	public static Collection<Audit> getAudits() {
		return Collections.unmodifiableCollection(instance.audits);
	}

	public static Audit getAudit(String name) {
		for(Audit audit: instance.audits) {
			if(audit.getName().equalsIgnoreCase(name)) {
				return audit;
			}
		}
		return null;
	}

	public static Collection<Connection> getConnections() {
		return Collections.unmodifiableCollection(instance.connections);
	}

	public static Connection getConnection(String name) {
		for(Connection connection: instance.connections) {
			if(connection.getName().equalsIgnoreCase(name)) {
				return connection;
			}
		}
		return null;
	}

	public static Task getTask(String name) {
		for(Task task: instance.tasks) {
			if(task.getName().equalsIgnoreCase(name)) {
				return task;
			}
		}
		return null;
	}
	
	public static Collection<Task> getTasks() {
		return Collections.unmodifiableCollection(instance.tasks);
	}
	
	/**
	 * @deprecated
	 */
	public static void setSrc(Connection sourceConnection) {
		logModification(sourceConnection);
		instance.connections.add(sourceConnection);
	}
 
	/**
	 * @deprecated
	 */
	public static Connection getSrc() {
		return getConnection("src-ldap");
	}

	
	/**
	 * @deprecated
	 */
	public static void setDst(Connection destinationConnection) {
		logModification(destinationConnection);
		instance.connections.add(destinationConnection);
	}

	/**
	 * @deprecated
	 */
	public static Connection getDst() {
		return getConnection("dst-ldap");
	}
	
	public static void setOtherSetting(String name, String value) {
		logModification(name + "/" + value);
		instance.otherSettings.put(name, value);
	}

	public static String getOtherSetting(String name) {
		return instance.otherSettings.get(name);
	}
	
	public static void addTask(Task task) {
		logModification(task);
		instance.tasks.add(task);
	}
	
	public static void removeTask(Task task) {
		logModification(task);
		instance.tasks.remove(task);
	}
	
	public static void addConnection(Connection connection) {
		logModification(connection);
		instance.connections.add(connection);
	}

	public static void removeConnection(Connection connection) {
		logModification(connection);
		instance.connections.remove(connection);
	}

	public static void addAudit(Audit audit) {
		logModification(audit);
		instance.audits.add(audit);
	}

	public static void removeAudit(Audit audit) {
		logModification(audit);
		instance.audits.remove(audit);
	}

	public static void reinitialize() {
		instance = new LscConfiguration();
	}
	
	public static void finalizeInitialization() {
		original = instance.clone();
		instance.underInitialization = false;
	}
	
	@SuppressWarnings("unchecked")
	public LscConfiguration clone() {
		LscConfiguration clone = new LscConfiguration();
		clone.revision 		= revision;
		if(audits != null) {
			clone.audits 		= (ArrayList<Audit>) audits.clone();
		}
		if(connections != null) {
			clone.connections 	= (ArrayList<Connection>) connections.clone();
		}
		if(tasks != null) {
			clone.tasks 			= (ArrayList<Task>) tasks.clone();
		}
		if(security != null) {
			clone.security = (Security) security.clone();
		}
		return clone;
	}
	
	public static void saving() {
		instance.revision++;
	}
	
	public static void saved() {
		finalizeInitialization();
	}
	
	public static void logModification(Object o) {
		if(!instance.underInitialization) {
			// Store updates
			instance.modified = true;
		}
	}
	
	public boolean isModified() {
		return modified;
	}
	
	public static void revertToInitialState() {
		instance.audits = original.audits;
		instance.connections = original.connections;
		instance.tasks = original.tasks;
		instance.modified = false;
	}

	public static boolean isInitialized() {
		return instance != null && !instance.underInitialization;
	}
	
	public void setConnections(List<Connection> conns) {
		for(Connection conn : conns) {
			logModification(conn);
			this.connections.add(conn);
		}
	}
	
	public int getRevision() {
		return revision; 
	}
	
	public static Security getSecurity() {
		return instance.security;
	}

	public void setSecurity(Security sec) {
		security = sec;
	}

	public void validate() throws LscException {
		// Tasks will check used audits and connections
		for(Task task: getTasks()) {
			task.validate();
		}
		if(security != null) {
			this.security.validate();
		}
	}
}