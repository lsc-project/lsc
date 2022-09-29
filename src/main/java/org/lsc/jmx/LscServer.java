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

import java.util.Map;

import javax.management.MXBean;

import org.lsc.beans.SimpleBean;


/**
 * This object is used by JMX as public interface for Lsc Server
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
@MXBean
public interface LscServer {

	/**
	 * List all asynchronous manageable tasks (running or not)
	 * @return asynchronous tasks list
	 */
	public String[] getAsyncTasksName();
	
	/**
	 * List all synchronous manageable tasks (running or not)
	 * @return synchronous tasks list
	 */
	public String[] getSyncTasksName();
	
	/**
	 * Check if the named task is running or not
	 * @param taskName
	 * @return task status
	 */
	public boolean isAsyncTaskRunning(String taskName);

	/**
	 * Start a named task
	 * @param taskName
	 */
	public void startAsyncTask(String taskName);
	
	/**
	 * Start a named task
	 * @param taskName
	 */
	public boolean launchSyncTaskById(String taskName, String id, Map<String, String> attributes);
	
	/**
	 * Start a task with the corresponding bean object (bypass the source lookup)
	 * @param taskName the corresponding task name
	 * @param bean the object bean
	 */
	public boolean launchSyncTask(String taskName, SimpleBean bean);
	
	/**
	 * Schedule for shutdown a named task
	 * @param taskName
	 */
	public void shutdownAsyncTask(String taskName);

	/**
	 * Just ping the LSC instance
	 * @return the server status
	 */
	public boolean ping();

	/**
	 * Return LSC process identifier
	 * @return the process identifier
	 */
	public String getPid();

	/**
	 * Return the status of the current LSC instance
	 * @return the instance status
	 */
	public String status();

	/**
	 * Stop the whole LSC instance
	 */
	public void stop();
	
	/**
	 * Accessor
	 * @return the configuration directory
	 */
	public String getConfigurationDirectory();

	/**
	 * Launch a clean task
	 * @param taskName
	 * @throws Exception
	 */
	public void launchCleanTask(String taskName) throws Exception;
	
	/**
	 * Get status line of a task
	 */
	public String getTaskStatus(String taskName);
	
}
