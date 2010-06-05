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
 *               (c) 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jmx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.CommunicationException;

import org.junit.Assert;
import org.junit.Test;
import org.lsc.Ldap2LdapSyncTest;
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
	
	@Test
	public final void testList() throws Exception {
		clean();
		assertTrue(JndiServices.getSrcInstance().exists(Ldap2LdapSyncTest.DN_ADD_SRC));
		assertFalse(JndiServices.getDstInstance().exists(Ldap2LdapSyncTest.DN_ADD_DST));
		
		Thread syncThread = new Thread(this);
		this.start(); 
		Thread.sleep(2000);

		// Check that the regular sync goes well
		assertTrue(JndiServices.getDstInstance().exists(Ldap2LdapSyncTest.DN_ADD_DST));
		// Then clean
		clean();
		// And launch it through JMX
		LscAgent lscAgent = new LscAgent();
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-l"} );
		Assert.assertEquals(lscAgent.run(), 0);
		lscAgent.parseOptions(new String[] {"-h", "localhost", "-p", "1099", "-a", Ldap2LdapSyncTest.TASK_NAME, 
				"-i", Ldap2LdapSyncTest.DN_ADD_SRC, "-t", "sn=SN0003"} );
		Assert.assertEquals(lscAgent.run(), 0);
		
		assertTrue(JndiServices.getDstInstance().exists(Ldap2LdapSyncTest.DN_ADD_DST));
		
		syncThread.interrupt();
	}

	public void run() {
		try {
			Ldap2LdapSyncTest.launchSyncCleanTask(Ldap2LdapSyncTest.TASK_NAME, true, false, false);
		} catch (Exception e) {
			LOGGER.debug(e.toString(), e);
		}
	}
	
	private void clean() throws CommunicationException {
		if(JndiServices.getDstInstance().exists(Ldap2LdapSyncTest.DN_ADD_DST)) {
			JndiModifications jm = new JndiModifications(JndiModificationType.DELETE_ENTRY, Ldap2LdapSyncTest.TASK_NAME);
			jm.setDistinguishName(Ldap2LdapSyncTest.DN_ADD_DST);
			JndiServices.getDstInstance().apply(jm);
		}
	}
	
}
