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
package org.lsc.jmx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.lsc.beans.SimpleBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX Agent - Provide the ability to start/stop/get status of asynchronous
 * tasks
 * 
 * By default this agent is only used on local call
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LscAgent implements NotificationListener {

	/** The local logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(LscAgent.class);

	/** When asking for an operation, this field contains the task */
	private String taskName;

	/** The different asynchronous task operation types */
	public enum OperationType {
		START, STOP, STATUS, TASKS_LIST, UNKNOWN,
	}

	/** The operation type */
	private OperationType operation;

	/** Specify the URL */
	private JMXServiceURL url;
	/** The RMI connector */
	private RMIConnector jmxC;
	/** The MBean server connection */
	private MBeanServerConnection jmxc;
	/** The LSC Server MBean */
	private LscServer lscServer;
	/** The IP hostname */
	private String hostname;
	/** The TCP port */
	private String port;
	/** Identifier to synchronize */
	private String idToSync;
	/** Identifier to synchronize */
	private Map<String, String> attrsToSync;

	/**
	 * Default constructor
	 */
	public LscAgent() {
		operation = OperationType.UNKNOWN;
		attrsToSync = new HashMap<String, String>(); 
	}

	public static void main(String[] args) {
		LscAgent jmxAgent = new LscAgent();
		int retCode = jmxAgent.parseOptions(args);
		if (retCode > 0) {
			System.exit(retCode);
		}
		System.exit(jmxAgent.run(jmxAgent.getOperation()));
	}

	public int run(OperationType operation) {
		if(!jmxBind()) {System.exit(1);
			return 1;
		}
		switch(operation) {
			case START:
				if(idToSync != null) {
					if(lscServer.launchSyncTaskById(taskName, idToSync, attrsToSync)) {
						LOGGER.info("Synchronization per id successfully achieved.");
					} else {
						LOGGER.error("Synchronization per id failed !");
						return 2;
					}
				} else {
					lscServer.startAsyncTask(taskName);
				}
				break;
			case STOP:
				lscServer.shutdownAsyncTask(taskName);
				break;
			case STATUS:
				boolean status = lscServer.isAsyncTaskRunning(taskName);
				if(ArrayUtils.contains(lscServer.getAsyncTasksName(), taskName)) {
					LOGGER.info("Asynchronous task " + taskName + " is " + ( status ? "running" : "stopped"));
				} else 	if(ArrayUtils.contains(lscServer.getSyncTasksName(), taskName)) {
					LOGGER.info("Synchronous task " + taskName + " is " + ( status ? "running" : "stopped"));	
				} else {
					LOGGER.error("Unknown or synchronous task name: " + taskName);
					return 3;
				}
				
				// Display full taks status
				if (status) {
					String fullStatus = lscServer.getTaskStatus(taskName);
					LOGGER.info(fullStatus);
					return 0;
				}
				
				return 1;
			case TASKS_LIST:
				LOGGER.info("Available asynchronous tasks are: ");
				for(String taskName: lscServer.getAsyncTasksName()) {
					LOGGER.info(" - " + taskName);
				}
				LOGGER.info("Available synchronous tasks are: ");
				for(String taskName: lscServer.getSyncTasksName()) {
					LOGGER.info(" - " + taskName);
				}
				break;
			default:
				
		}
		jmxUnbind();
		return 0;
	}
	
	public boolean syncByObject(String taskName, String id, Map<String, List<String>> map) {
		if(!jmxBind()) {
			return false;
		}
		SimpleBean bean = new SimpleBean();
		bean.setMainIdentifier(id);
		for(Entry<String, List<String>> entry: map.entrySet()) {
			bean.setDataset(entry.getKey(), new HashSet<Object>(Arrays.asList(entry.getValue().toArray())));
		}
		return lscServer.launchSyncTask(taskName, bean);
	}

	/**
	 * Manage command line options.
	 * 
	 * @param args command line
	 * @return the status code (0: OK, >=1 : failed)
	 */
	protected int parseOptions(final String[] args) {
		Options options = new Options();
		options.addOption("a", "start", true, "Start an asynchronous task");
		options.addOption("h", "hostname", true, "Specify the hostname to connect to");
		options.addOption("l", "list", false, "List the available asynchronous tasks");
		options.addOption("o", "stop", true, "Stop an asynchronous task");
		options.addOption("p", "port", true, "Specify the port to connect to");
		options.addOption("i", "identifier", true, "Specify the identifier to synchronize");
		options.addOption("t", "attributes", true, "Specify the attributes pivot to synchronize (comma separated, identifier parameter required)");
		options.addOption("s", "status", true, "Get a task status");

		CommandLineParser parser = new GnuParser();

		try {
			CommandLine cmdLine = parser.parse(options, args);
			if ( cmdLine.hasOption("a") ) {
				operation = OperationType.START;
				taskName = cmdLine.getOptionValue("a");
			}
			if ( cmdLine.hasOption("l") ) {
				operation = OperationType.TASKS_LIST;
			}
			if ( cmdLine.hasOption("o") ) {
				operation = OperationType.STOP;
				taskName = cmdLine.getOptionValue("o");
			}
			if ( cmdLine.hasOption("s") ) {
				operation = OperationType.STATUS;
				taskName = cmdLine.getOptionValue("s");
			}
			if ( cmdLine.hasOption("i") ) {
				idToSync = cmdLine.getOptionValue("i");
				if(cmdLine.hasOption("t")) {
					StringTokenizer attrsStr = new StringTokenizer(cmdLine.getOptionValue("t"),",");
					while(attrsStr.hasMoreTokens()) {
						String token = attrsStr.nextToken();
						if(token.contains("=")) {
							attrsToSync.put(token.substring(0, token.indexOf("=")), token.substring(token.indexOf("=")+1));
						} else {
							LOGGER.error("Unknown attribute name=value couple in \"{}\". Please check your parameters !", token);
							printHelp(options);
							return 1;
						}
					}
				}
			} else if (cmdLine.hasOption("t") ) {
				LOGGER.error("Attributes specified, but missing identifier !");
				printHelp(options);
				return 1;
			}
			if ( cmdLine.hasOption("h") ) {
				hostname = cmdLine.getOptionValue("h");
			} else {
				hostname = "localhost";
				LOGGER.info("Hostname parameter not specified, using {} as default value.", hostname);
			}
			if ( cmdLine.hasOption("p") ) {
				port = cmdLine.getOptionValue("p");
			} else {
				port = "1099";
				LOGGER.info("TCP Port parameter not specified, using {} as default value.", port);
			}
			if (operation == OperationType.UNKNOWN ) {
				printHelp(options);
				return 1;
			}
		} catch (ParseException e) {
			LOGGER.error("Unable to parse the options ({})", e.toString());
			LOGGER.debug(e.toString(), e);
			return 1;
		}
		return 0;
	}

	/**
	 * Print the command line help.
	 * 
	 * @param options
	 *            specified options to manage
	 */
	private static void printHelp(final Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("lsc", options);
	}

	/**
	 * Bind to the JMX Server 
	 */
	public boolean jmxBind() {
		try {
			String sUrl = "service:jmx:rmi:///jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
			LOGGER.info("Connecting to remote engine on : " + sUrl);
			url = new JMXServiceURL(sUrl);
			jmxC = new RMIConnector(url, null);
			jmxC.connect();
			jmxc = jmxC.getMBeanServerConnection();
			ObjectName lscServerName = new ObjectName("org.lsc.jmx:type=LscServer");
			lscServer = JMX.newMXBeanProxy(jmxc, lscServerName, LscServer.class, true);
			return true;
		} catch (MalformedObjectNameException e) {
			LOGGER.error(e.toString(), e);
		} catch (NullPointerException e) {
			LOGGER.error(e.toString(), e);
		} catch (MalformedURLException e) {
			LOGGER.error(e.toString(), e);
		} catch (IOException e) {
			LOGGER.error(e.toString(), e);
		}
		return false;
	}
	
	/**
	 * Unbind from the JMX Server 
	 */
	protected boolean jmxUnbind() {
		try {
			jmxC.close();
			return true;
		} catch (IOException e) {
			LOGGER.error(e.toString(), e);
		}
		return false;
	}

	public void handleNotification(Notification notification, Object handback) {
		LOGGER.info("\nReceived notification: " + notification);
	}

	public OperationType getOperation() {
		return operation;
	}
}
