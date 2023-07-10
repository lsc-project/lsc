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
package org.lsc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.beans.IBean;
import org.lsc.configuration.LscConfiguration;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiSrcService;
import org.lsc.service.IService;
import org.lsc.utils.directory.LDAP;

/**
 * This test case attempts to reproduce a ldap2ldap setup via SimpleSynchronize.
 * It attempts to launch all the 3 tasks defined in
 * src/test/resources/etc/lsc.xml
 * ldap2ldapOrderedTestTask3, ldap2ldapOrderedTestTask2, and ldap2ldapOrderedTestTask1
 */
public class Ldap2LdapOrderedSyncTest extends CommonLdapSyncTest {


	@Before
	public void setup() {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		Assert.assertNotNull(LscConfiguration.getConnection("src-ldap"));
		Assert.assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		reloadJndiConnections();
	}

	@Test
	public final void testLdap2LdapOrderedSyncTest() throws Exception {

		// Declare the tasks to launch in the correct order
		List<String> user_tasks = Arrays.asList("ldap2ldapOrderedTestTask3", "ldap2ldapOrderedTestTask2", "ldap2ldapOrderedTestTask1");

		// perform the sync
		launchSyncCleanTask(user_tasks, false, true, false);

		// check the results of the synchronization
		reloadJndiConnections();
		checkSyncResults();
	}

	private final void checkSyncResults() throws Exception {
		List<String> attributeValues = null;


		// check Operation order
                /*
                 * Here there is nothing to check really
                 * - either every task has succeeded
                 * - either one task has failed, which means the order is not enforced
                 * Indeed, the task can only succeed in this precise order:
                 *     - ldap2ldapOrderedTestTask3 (creates an entry cn=CN0001-A from cn=CN0001),
                 *     - ldap2ldapOrderedTestTask2 (creates an entry cn=CN0001-B from cn=CN0001-A),
                 *     - ldap2ldapOrderedTestTask1 (creates an entry cn=CN0001-C from cn=CN0001-B)
                 * See src/test/resources/etc/lsc.xml for more details
                 * if the order was taken from the position of the tasks in lsc.xml, it would fail
                 */

	}

	public static void launchSyncCleanTask(List<String> tasks, boolean doAsync, boolean doSync,
					boolean doClean) throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();


		if (doAsync) {
			for(String taskName: tasks) {
				asyncType.add(taskName);
			}
		}
		
		if (doSync) {
			for(String taskName: tasks) {
				syncType.add(taskName);
			}
		}

		if (doClean) {
			for(String taskName: tasks) {
				cleanType.add(taskName);
			}
		}

		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue(ret);
	}
}
