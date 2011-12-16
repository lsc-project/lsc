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
package org.lsc.jmx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.CommunicationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.Ldap2LdapSyncTest;
import org.lsc.SimpleSynchronize;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.jndi.JndiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test covers all the JMX capabilities
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LscJmxTest extends Thread {

	/** The local logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(LscJmxTest.class);
	
	public final static String TASK_NAME = "ldap2ldapJmxTestTask";
	public final static String DN_ADD_SRC = "cn=CN0003,ou=ldap2ldapJmxTestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_ADD_DST = "cn=CN0003,ou=ldap2ldapJmxTestTaskDst,ou=Test Data,dc=lsc-project,dc=org";

	private JndiServices jndiServices;
	
	@Before
	public void setup() {
		Assert.assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		jndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap"));
	}
	
	@Test
	public final void testList() throws Exception {
		clean();
		assertTrue(jndiServices.exists(DN_ADD_SRC));
		assertFalse(jndiServices.exists(DN_ADD_DST));
		
		Thread syncThread = new Thread(this);
		this.start(); 
		Thread.sleep(3500);

		// Check that the regular sync goes well
		assertTrue(jndiServices.exists(DN_ADD_DST));
		// Then clean
		clean();
		// And launch it through JMX
		LscAgent lscAgent = new LscAgent();
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-l"} );
		Assert.assertEquals(lscAgent.run(lscAgent.getOperation()), 0);
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-a", TASK_NAME, 
				"-i", DN_ADD_SRC, "-t", "sn=SN0003"} );
		Assert.assertEquals(lscAgent.run(lscAgent.getOperation()), 0);
		
		assertTrue(jndiServices.exists(DN_ADD_DST));
		
		syncThread.interrupt();
	}

	@Test
	public final void testSyncByObject() throws Exception {

//		Thread syncThread = new Thread(this);
		this.start(); 
		Thread.sleep(3500);

		LscAgent lscAgent = new LscAgent();
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-l"} );
		Assert.assertEquals(lscAgent.run(lscAgent.getOperation()), 0);
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-a", TASK_NAME, 
				"-i", DN_ADD_SRC, "-t", "sn=SN0003"} );
		Map<String, List<String>> values = new HashMap<String, List<String>>();
		values.put("sn", Arrays.asList(new String[] {"SN0003"}));
		values.put("cn", Arrays.asList(new String[] {"CN0003"}));
		values.put("objectClass", Arrays.asList(new String[] {"person", "top"}));
		Assert.assertTrue(lscAgent.syncByObject(TASK_NAME, DN_ADD_DST, values));
	}
	
	public void run() {
		try {
			SimpleSynchronize sync = new SimpleSynchronize();
			sync.init();
			sync.setThreads(1);
			LscServerImpl.startJmx(sync);
			Ldap2LdapSyncTest.launchSyncCleanTask(TASK_NAME, true, false, false);
		} catch (RuntimeException e) {
			LOGGER.debug(e.toString(), e);
		} catch (Throwable e) {
			LOGGER.debug(e.toString(), e);
		}
	}
	
	private void clean() throws CommunicationException {
		if(jndiServices.exists(DN_ADD_DST)) {
			JndiModifications jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, TASK_NAME);
			jm.setDistinguishName(DN_ADD_DST);
			jndiServices.apply(jm);
		}
	}
}
