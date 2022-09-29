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

import java.beans.ConstructorProperties;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.lsc.AbstractSynchronize;
import org.lsc.Configuration;
import org.lsc.LscDatasets;
import org.lsc.SimpleSynchronize;
import org.lsc.Task;
import org.lsc.beans.SimpleBean;
import org.lsc.utils.PidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
/**
 * This class is implementing all the exposed server methods
 * 
 * For security purposes, please look at :
 * http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#remote
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LscServerImpl implements LscServer, Runnable {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractSynchronize.class);

	private SimpleSynchronize synchronize;

	@ConstructorProperties({"synchronize"})
	public LscServerImpl(SimpleSynchronize synchronize) {
		this.synchronize = synchronize;
	}
	
	public SimpleSynchronize getSynchronize() {
		return synchronize;
	}

	public String[] getAsyncTasksName() {
		List<String> asynchronousTasksName = new ArrayList<String>();
		for(Task task: synchronize.getTasks()) {
			if(synchronize.isAsynchronousTask(task.getName())) {
				asynchronousTasksName.add(task.getName());
			}
		}
		return asynchronousTasksName.toArray(new String[asynchronousTasksName.size()]);
	}

	public String[] getSyncTasksName() {
		List<String> synchronousTasksName = new ArrayList<String>();
		for(Task task: synchronize.getTasks()) {
			if(!synchronize.isAsynchronousTask(task.getName())) {
				synchronousTasksName.add(task.getName());
			}
		}
		return synchronousTasksName.toArray(new String[synchronousTasksName.size()]);
	}

	public boolean isAsyncTaskRunning(String taskName) {
		boolean status = synchronize.isAsynchronousTaskRunning(taskName);
		LOGGER.info("Task " + taskName + " confirmed " + (status ? "running" : "stopped") + " to JMX client !");
		return status;
	}

	private static boolean jmxStarted = false;

	/**
	 * Register LSC as JMX MBean
	 * @param sync the synchronization object to use as callback
	 */
	public static void startJmx(SimpleSynchronize sync) {
		try {
			if (!jmxStarted) {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				ObjectName name = new ObjectName("org.lsc.jmx:type=LscServer");
				LscServerImpl mbean = new LscServerImpl(sync);
				mbs.registerMBean(mbean, name);
				jmxStarted = true;
			}
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstanceAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MBeanRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotCompliantMBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean launchSyncTaskById(String taskName, String id, Map<String, String> attributes) {
		Map<String, LscDatasets> entries = new HashMap<String, LscDatasets>();
		entries.put(id, new LscDatasets(attributes));
		return synchronize.launchById(taskName, entries);
	}
	
	public void launchCleanTask(String taskName) throws Exception {
		List<String> cleanList = new ArrayList<String>();
		cleanList.add(taskName);
		synchronize.launch(SimpleSynchronize.EMPTY_LIST, SimpleSynchronize.EMPTY_LIST, cleanList);
	}

	public void shutdownAsyncTask(String taskName) {
		synchronize.shutdownAsynchronousSynchronize2Ldap(taskName, true);
	}

	public void startAsyncTask(String taskName) {
		synchronize.startAsynchronousSynchronize2Ldap(synchronize.getTask(taskName));
	}

	public boolean ping() {
		return true;
	}
	
	public String getPid() {
		return PidUtil.getPID();
	}
	
	public String status() {
		return "Running";
	}
	
	public void stop() {
		for(Task task: synchronize.getTasks()) {
			String taskName = task.getName();
			if(synchronize.isAsynchronousTask(taskName)
					&& synchronize.isAsynchronousTaskRunning(taskName)) {
				synchronize.shutdownAsynchronousSynchronize2Ldap(taskName, false);
			}
		}
		new Thread(this).start();
	}
	
	public void run() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		System.exit(0);
	}
	
	public String getConfigurationDirectory() {
		return Configuration.getConfigurationDirectory();
	}

	@Override
	public boolean launchSyncTask(String taskName, SimpleBean bean) {
		return synchronize.launch(taskName, bean);
	}

	public String getTaskStatus(String taskName) {
		return synchronize.getTaskFullStatus(taskName);
	}
}
