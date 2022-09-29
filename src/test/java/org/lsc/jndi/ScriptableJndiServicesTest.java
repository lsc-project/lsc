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
package org.lsc.jndi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.NamingException;

import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;

public class ScriptableJndiServicesTest {

	private JndiServices dstJndiServices;
	
	@Before
	public void setup() {
		assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		dstJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap"));
	}
	
	@Test
	public void testValuesOutOfRange() throws NamingException {
		ScriptableJndiServices sjs = new ScriptableJndiServices();
		sjs.setJndiServices(dstJndiServices);
//		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "-1"), null);
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "5").size(), 0);
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "4").size(), 0);
	}

	@Test
	public void testValidNonNullValues() throws NamingException {
		ScriptableJndiServices sjs = new ScriptableJndiServices();
		sjs.setJndiServices(dstJndiServices);
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "3").get(0), "dc=org");
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "2").get(0), "dc=lsc-project,dc=org");
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "1").get(0), "ou=People,dc=lsc-project,dc=org");
	}

	@Test
	public void testSup0() throws NamingException {
		ScriptableJndiServices sjs = new ScriptableJndiServices();
		sjs.setJndiServices(dstJndiServices);
		assertEquals(sjs.sup("uid=seb,ou=People,dc=lsc-project,dc=org", "0").size(), 3);
	}
}
