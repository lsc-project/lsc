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
package org.lsc.utils.directory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import mockit.Mocked;

import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.junit.Test;
import org.lsc.Task;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.ScriptingEvaluator;
;
;

/**
 * Test LDAP function library.
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class LDAPTest {

	@Mocked Task task;
	
	@Test
	public final void testCanBind() throws NamingException, LdapURLEncodingException, LscServiceException {
		assertTrue(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager", "secret"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager", "public"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=nobody", "secret"));

		assertTrue(ScriptingEvaluator.evalToBoolean(task, "LDAP.canBind(\"ldap://localhost:33389/\", "
				+ "\"cn=Directory Manager\", \"secret\")", null));
		assertFalse(ScriptingEvaluator.evalToBoolean(task, "LDAP.canBind(\"ldap://localhost:33389/\", "
				+ "\"cn=Directory Manager\", \"public\")", null));
		assertFalse(ScriptingEvaluator.evalToBoolean(task, "LDAP.canBind(\"ldap://localhost:33389/\", "
				+ "\"cn=nobody\", \"secret\")", null));

		assertTrue(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager",
						"secret", "uid=00000001,ou=People,dc=lsc-project,dc=org", "secret"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager",
						"secret", "uid=00000001,ou=People,dc=lsc-project,dc=org", "public"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager",
						"secret", "uid=nobody,ou=People,dc=lsc-project,dc=org", "secret"));

		assertTrue(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=00000001)",
						"cn=Directory Manager", "secret", "secret"));
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=00000001)",
						"cn=Directory Manager", "secret", "public"));
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=nonexistant)",
						"cn=Directory Manager", "secret", "secret"));

		// this should fail since there are two cn=CN0001 entries
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(cn=CN0001)",
						"cn=Directory Manager", "secret", "secret"));
	}

	// this should fail with a can't connect exception
	@Test(expected = NamingException.class)
	public void testCanBindCantConnect() throws NamingException {
		LDAP.canBind("ldap://no.such.host.really:33389/", "cn=Directory Manager", "public");
	}

	// this should fail with a NamingException (no such object)
	@Test(expected = NameNotFoundException.class)
	public void testCanBindSearchRebindNoSuchObject() throws NamingException, LdapURLEncodingException {
		LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=com??sub?(cn=CN0001)",
						"cn=Directory Manager", "secret", "secret");
	}
}
