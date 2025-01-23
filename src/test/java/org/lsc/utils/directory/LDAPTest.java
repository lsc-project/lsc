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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lsc.Task;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.ScriptingEvaluator;

@ExtendWith({ ApacheDSTestExtension.class })
@CreateDS(name = "DSWithPartitionAndServer", loadedSchemas = {
		@LoadSchema(name = "other", enabled = true) }, partitions = {
				@CreatePartition(name = "lsc-project", suffix = "dc=lsc-project,dc=org", contextEntry = @ContextEntry(entryLdif = "dn: dc=lsc-project,dc=org\n"
						+ "dc: lsc-project\n" + "objectClass: top\n" + "objectClass: domain\n\n"), indexes = {
								@CreateIndex(attribute = "objectClass"), @CreateIndex(attribute = "dc"),
								@CreateIndex(attribute = "ou") }) })
@CreateLdapServer(allowAnonymousAccess = true, transports = { @CreateTransport(protocol = "LDAP", port = 33389),
		@CreateTransport(protocol = "LDAPS", port = 33636) })
@ApplyLdifs({
		// Entry # 0
		"dn: cn=Directory Manager,ou=system", "objectClass: person", "objectClass: top", "cn: Directory Manager",
		"description: Directory Manager", "sn: Directory Manager", "userpassword: secret" })
@ApplyLdifFiles({ "lsc-schema.ldif", "lsc-project.ldif" })
/**
 * Test LDAP function library.
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class LDAPTest extends AbstractLdapTestUnit {
	Task task = mock(Task.class);

	@Test
	public final void testCanBind() throws NamingException, LdapURLEncodingException, LscServiceException {
		assertTrue(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager,ou=system", "secret"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager,ou=system", "public"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=nobody,ou=system", "secret"));

		assertTrue(ScriptingEvaluator.evalToBoolean(task,
				"LDAP.canBind(\"ldap://localhost:33389/\", " + "\"cn=Directory Manager,ou=system\", \"secret\")",
				null));
		assertFalse(ScriptingEvaluator.evalToBoolean(task,
				"LDAP.canBind(\"ldap://localhost:33389/\", " + "\"cn=Directory Manager,ou=system\", \"public\")",
				null));
		assertFalse(ScriptingEvaluator.evalToBoolean(task,
				"LDAP.canBind(\"ldap://localhost:33389/\", " + "\"cn=nobody,ou=system\", \"secret\")", null));

		assertTrue(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager,ou=system", "secret",
				"uid=00000001,ou=People,dc=lsc-project,dc=org", "secret"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager,ou=system", "secret",
				"uid=00000001,ou=People,dc=lsc-project,dc=org", "public"));
		assertFalse(LDAP.canBind("ldap://localhost:33389/", "cn=Directory Manager,ou=system", "secret",
				"uid=nobody,ou=People,dc=lsc-project,dc=org", "secret"));

		assertTrue(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=00000001)",
				"cn=Directory Manager,ou=system", "secret", "secret"));
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=00000001)",
				"cn=Directory Manager,ou=system", "secret", "public"));
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(uid=nonexistant)",
				"cn=Directory Manager,ou=system", "secret", "secret"));

		// this should fail since there are two cn=CN0001 entries
		assertFalse(LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=org??sub?(cn=CN0001)",
				"cn=Directory Manager,ou=system", "secret", "secret"));
	}

	// this should fail with a can't connect exception
	@Test
	public void testCanBindCantConnect() throws NamingException {
		assertThrows(NamingException.class,
				() -> LDAP.canBind("ldap://no.such.host.really:33389/", "cn=Directory Manager", "public"));
	}

	// this should fail with a NamingException (no such object)
	@Test
	public void testCanBindSearchRebindNoSuchObject() throws NamingException, LdapURLEncodingException {
		assertThrows(NamingException.class,
				() -> LDAP.canBindSearchRebind("ldap://localhost:33389/dc=lsc-project,dc=com??sub?(cn=CN0001)",
						"cn=Directory Manager", "secret", "secret"));
	}
}
