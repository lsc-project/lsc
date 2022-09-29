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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.CommunicationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.LscConfiguration;
import org.lsc.utils.directory.LDAP;

public class Ldap2LdapBinaryPivotSyncTest extends CommonLdapSyncTest {

	public static String SOURCE_DN = "ou=ldap2ldapBinaryTestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public static String DESTINATION_DN = "ou=ldap2ldapBinaryTestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public static String TASK_NAME = "ldap2ldapBinaryTestTask";

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}
	
	@Override
	public String getSourceDn() {
		return SOURCE_DN;
	}
	
	@Override
	public String getDestinationDn() {
		return DESTINATION_DN;
	}
	
	public List<String> getTaskList() {
		return Arrays.asList(getTaskName());
	}
	

	@Before
	public void setup() throws CommunicationException {
		LscConfiguration.reset();
		LscConfiguration.getInstance();
		Assert.assertNotNull(LscConfiguration.getConnection("src-ldap"));
		Assert.assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		reloadJndiConnections();
	}
	
	@Test
	public void testSync() throws Exception {
		// make sure the contents of the directory are as we expect to begin with

		// check MODRDN
		assertTrue(srcJndiServices.exists(DN_MODRDN_SRC));
		assertTrue(dstJndiServices.exists(DN_MODRDN_DST_BEFORE));
		assertFalse(dstJndiServices.exists(DN_MODRDN_DST_AFTER));

		// check ADD
		assertTrue(srcJndiServices.exists(DN_ADD_SRC));
		assertFalse(dstJndiServices.exists(DN_ADD_DST));

		// check MODIFY
		assertTrue(srcJndiServices.exists(DN_MODIFY_SRC));
		assertTrue(dstJndiServices.exists(DN_MODIFY_DST));
		// the original password is present and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secret0001"));
		// the new password can not be used yet
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secretCN0001"));
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_DST, "secretCN0001"));

		// perform the sync
		new SimpleSynchronize().launch(new ArrayList<String>(), getTaskList(), new ArrayList<String>());

		// check the results of the synchronization
		reloadJndiConnections();
		
		// check MODRDN
		assertTrue(srcJndiServices.exists(DN_MODRDN_SRC));
		assertFalse(dstJndiServices.exists(DN_MODRDN_DST_BEFORE));
		assertTrue(dstJndiServices.exists(DN_MODRDN_DST_AFTER));

		// check ADD
		assertTrue(srcJndiServices.exists(DN_ADD_SRC));
		assertTrue(dstJndiServices.exists(DN_ADD_DST));

		// check MODIFY
		assertTrue(srcJndiServices.exists(DN_MODIFY_SRC));
		assertTrue(dstJndiServices.exists(DN_MODIFY_DST));
		// the original password is present and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secret0001"));
		// the password was set and can be used
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secretCN0001"));
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_DST, "secretCN0001"));
		
		// check convergence after MODRDN
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODRDN_DST_AFTER, "0002"));
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODRDN_DST_AFTER, "secretCN0002"));
		// perform the sync
		new SimpleSynchronize().launch(new ArrayList<String>(), getTaskList(), new ArrayList<String>());
		// check the results of the synchronization
		reloadJndiConnections();
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODRDN_DST_AFTER, "0002"));
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODRDN_DST_AFTER, "secretCN0002"));
	}
	
	@Test
	public void testClean() throws Exception {
		// make sure the contents of the directory are as we expect to begin with
		assertTrue(dstJndiServices.exists(DN_DELETE_DST));
		assertFalse(srcJndiServices.exists(DN_DELETE_SRC));

		// perform the clean
		new SimpleSynchronize().launch(new ArrayList<String>(), new ArrayList<String>(), getTaskList());

		// check the results of the clean
		reloadJndiConnections();
		assertFalse(dstJndiServices.exists(DN_DELETE_DST));
		assertTrue(dstJndiServices.exists(DN_MODIFY_DST));
	}
}
