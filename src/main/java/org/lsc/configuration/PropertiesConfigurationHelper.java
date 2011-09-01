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
package org.lsc.configuration;

import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.configuration.ConfigurationException;
import org.lsc.Configuration;
import org.lsc.configuration.objects.LscConfiguration;
import org.lsc.configuration.objects.Task;
import org.lsc.configuration.objects.connection.Database;
import org.lsc.configuration.objects.connection.directory.AuthenticationType;
import org.lsc.configuration.objects.connection.directory.Ldap;
import org.lsc.configuration.objects.security.Encryption;
import org.lsc.configuration.objects.security.Security;
import org.lsc.exception.LscConfigurationException;

/**
 * Properties configuration loader
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class PropertiesConfigurationHelper {

	/** lsc prefix. */
	public static final String LSC_PROPS_PREFIX = "lsc";

	/** lsc.tasks property. */
	public static final String TASKS_PROPS_PREFIX = "tasks";

	/** lsc.syncoptions property. */
	public static final String SYNCOPTIONS_PREFIX = "syncoptions";

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

	/** lsc.tasks.TASKNAME.bean property. */
	public static final String ASYNCHRONOUS_PROPS = "async";

	/** lsc.tasks.TASKNAME.postSyncHook property. */
	public static final String POST_SYNC_HOOK_PROPS_PREFIX = "postSyncHook";
	
	/** lsc.tasks.TASKNAME.postCleanHook property. */
	public static final String POST_CLEAN_HOOK_PROPS_PREFIX = "postCleanHook";

	/**
	 * Call by Configuration.setUp() if the new format configuration file is not available
	 * @param filename
	 * @throws ConfigurationException
	 */
	@SuppressWarnings("deprecation")
	public static void loadConfigurationFrom(String filename) throws LscConfigurationException {
		LscConfiguration.reinitialize();
		Properties conf = org.lsc.Configuration.getAsProperties(filename, LSC_PROPS_PREFIX);
		
        // Get the "lsc.tasks" property
        String tasks = conf.getProperty(TASKS_PROPS_PREFIX);
        
        if(org.lsc.Configuration.getString("src.java.naming.provider.url") != null) {
            Ldap ldapConn = new Ldap();
            ldapConn.setName("src-ldap");
            ldapConn.setUsername(org.lsc.Configuration.getString("src.java.naming.security.principal"));
            ldapConn.setPassword(org.lsc.Configuration.getString("src.java.naming.security.credentials"));
            if(ldapConn.getUsername() != null) {
            	ldapConn.setAuthenticationType(AuthenticationType.SIMPLE);
            } else {
            	ldapConn.setAuthenticationType(AuthenticationType.ANONYMOUS);
            }
            ldapConn.setUrl(org.lsc.Configuration.getString("src.java.naming.provider.url"));
            LscConfiguration.addConnection(ldapConn);
        }

        if(org.lsc.Configuration.getString("src.database.url") != null)  {
            Database jdbcConn = new Database();
            jdbcConn.setName("src-jdbc");
            jdbcConn.setUsername(org.lsc.Configuration.getString("src.database.username"));
            jdbcConn.setPassword(org.lsc.Configuration.getString("src.database.password"));
            jdbcConn.setUrl(org.lsc.Configuration.getString("src.database.url"));
            jdbcConn.setDriver(org.lsc.Configuration.getString("src.database.driver"));
            LscConfiguration.addConnection(jdbcConn);
        }

        if(org.lsc.Configuration.getString("dst.java.naming.provider.url") != null) {
	        Ldap dstConn = new Ldap();
	        dstConn.setName("dst-ldap");
	        dstConn.setUsername(org.lsc.Configuration.getString("dst.java.naming.security.principal"));
	        dstConn.setPassword(org.lsc.Configuration.getString("dst.java.naming.security.credentials"));
	        if(dstConn.getUsername() != null) {
	        	dstConn.setAuthenticationType(AuthenticationType.SIMPLE);
	        } else {
	        	dstConn.setAuthenticationType(AuthenticationType.ANONYMOUS);
	        }
	        dstConn.setUrl(org.lsc.Configuration.getString("dst.java.naming.provider.url"));
	        LscConfiguration.addConnection(dstConn);
        }
        
        if (tasks != null) {
            // Iterate on each task
            StringTokenizer tasksSt = new StringTokenizer(tasks, ",");
    		while (tasksSt.hasMoreTokens()) {
    			String taskName = tasksSt.nextToken();
    			Task ti = new Task(taskName, conf);
    			LscConfiguration.addTask(ti);
    		}
        }
        Security sec = new Security();
        sec.setEncryption(new Encryption());
        if(new File(Configuration.getConfigurationDirectory(), "lsc.key").exists()) {
        	sec.getEncryption().setKeyfile(new File(Configuration.getConfigurationDirectory(), "lsc.key").getAbsolutePath());
        }
        LscConfiguration.getInstance().setSecurity(sec);
        
        LscConfiguration.finalizeInitialization();
	}
}
