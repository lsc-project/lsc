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
package org.lsc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lsc.beans.IBean;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.JndiServices;
import org.lsc.jndi.SimpleJndiSrcService;
import org.lsc.service.IService;
import org.lsc.utils.directory.LDAP;

/**
 * This test case attempts to reproduce a typical ldap2ldap setup via
 * SimpleSynchronize. This relies on settings in lsc.properties from the test
 * resources, and entries in the local OpenDS directory. testSync() performs a
 * synchronization between two branches of the local LDAP server, and should
 * perform 3 operations : 1 ADD, 1 MODRDN, 1 MODIFY. The
 * 
 * @author Jonathan Clarke &ltjonathan@phillipoux.net&gt;
 */
public class Ldap2LdapSyncTest {

	public final static String TASK_NAME = "ldap2ldapTestTask";
	public final static String DN_ADD_SRC = "cn=CN0003,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_ADD_DST = "cn=CN0003,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_MODIFY_SRC = "cn=CN0001,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_MODIFY_DST = "cn=CN0001,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_DELETE_SRC = "cn=CN0004,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_DELETE_DST = "cn=CN0004,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_MODRDN_SRC = "cn=CN0002,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_MODRDN_DST_BEFORE = "cn=CommonName0002,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DN_MODRDN_DST_AFTER = "cn=CN0002,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";

	private JndiServices srcJndiServices;
	
	private JndiServices dstJndiServices;
	
	@Before
	public void setup() {
		Assert.assertNotNull(LscConfiguration.getConnection("src-ldap"));
		Assert.assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		srcJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("src-ldap"));
		dstJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap"));
	}
	
	/**
	 * Test reading the userPassword attribute from our source directory through Object
	 * and Bean. This attribute has a binary syntax, so we must confirm we can parse it as a String.
	 * @throws NamingException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws LscServiceException 
	 */
	@Test
	public final void testReadUserPasswordFromLdap() throws NamingException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, LscServiceException {
		Map<String, LscDatasets> ids = new HashMap<String, LscDatasets>(1);
		Map<String, String> attributeValues = new HashMap<String, String>(1);
		attributeValues.put("sn", "SN0001");
		ids.put(DN_MODIFY_SRC, new LscDatasets(attributeValues));

		IService srcService = new SimpleJndiSrcService(LscConfiguration.getTask(TASK_NAME));
		Entry<String, LscDatasets> obj = ids.entrySet().iterator().next();
		IBean srcBean = srcService.getBean(obj.getKey(), obj.getValue(), true);
		String userPassword = srcBean.getDatasetFirstValueById("userPassword");

		// OpenDS automatically hashes the password using seeded SHA,
		// so we can't test the full value, just the beginning.
		// This is sufficient to confirm we can read the attribute as a String.
		assertTrue(userPassword.startsWith("{SSHA}"));
	}

	@Test
	public final void testSyncLdap2Ldap() throws Exception {

		// make sure the contents of the directory are as we expect to begin with

		// check MODRDN
		assertTrue(srcJndiServices.exists(DN_MODRDN_SRC));
		assertTrue(dstJndiServices.exists(DN_MODRDN_DST_BEFORE));
		assertFalse(dstJndiServices.exists(DN_MODRDN_DST_AFTER));

		// check ADD
		assertTrue(srcJndiServices.exists(DN_ADD_SRC));
		assertFalse(dstJndiServices.exists(DN_ADD_DST));
		checkAttributeIsEmpty(DN_ADD_SRC, "userPassword");
		checkAttributeIsEmpty(DN_ADD_SRC, "telephoneNumber");
		checkAttributeValue(DN_ADD_SRC, "description", "Number three's descriptive text");
		checkAttributeValue(DN_ADD_SRC, "sn", "SN0003");

		// check MODIFY
		assertTrue(srcJndiServices.exists(DN_MODIFY_SRC));
		assertTrue(dstJndiServices.exists(DN_MODIFY_DST));
		checkAttributeIsEmpty(DN_MODIFY_SRC, "telephoneNumber");
		checkAttributeValue(DN_MODIFY_SRC, "description", "Number one's descriptive text");
		checkAttributeValue(DN_MODIFY_SRC, "sn", "SN0001");
		// the original password is present and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secret0001"));
		// the new password can not be used yet
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_SRC, "secretCN0001"));
		assertFalse(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_DST, "secretCN0001"));

		// perform the sync
		launchSyncCleanTask(TASK_NAME, false, true, false);

		// check the results of the synchronization
		checkSyncResultsFirstPass();

		// sync again to confirm convergence
		launchSyncCleanTask(TASK_NAME, false, true, false);

		// check the results of the synchronization
		checkSyncResultsSecondPass();

		// sync a third time to make sure nothing changed
		launchSyncCleanTask(TASK_NAME, false, true, false);

		// check the results of the synchronization
		checkSyncResultsSecondPass();
	}

	private final void checkSyncResultsFirstPass() throws Exception {
		List<String> attributeValues = null;

		checkSyncResultsCommon();

		// check ADD

		// the telephoneNumber was created
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("000000");
		attributeValues.add("11111");
		checkAttributeValues(DN_ADD_DST, "telephoneNumber", attributeValues);

		// initials wasn't created, since it's not in the write attributes list
		attributeValues = new ArrayList<String>();
		checkAttributeValues(DN_ADD_DST, "initials", attributeValues);

		// mail was created, although it's not in the source object
		attributeValues = new ArrayList<String>();
		attributeValues.add("ok@domain.net");
		checkAttributeValues(DN_ADD_DST, "mail", attributeValues);

	}

	private final void checkSyncResultsSecondPass() throws Exception {
		List<String> attributeValues = null;

		checkSyncResultsCommon();

		// check MODRDN

		// the password was set and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODRDN_DST_AFTER, "secretCN0002"));

		// the description was copied over since this is an existing object and it description is set to MERGE
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("Number two's descriptive text");
		attributeValues.add(new String((byte[]) srcJndiServices.getEntry(DN_MODRDN_SRC, "objectclass=*").getAttributes().get("userPassword").get()));
		checkAttributeValues(DN_MODRDN_DST_AFTER, "description", attributeValues);

		// the telephoneNumber was added
		attributeValues = new ArrayList<String>(3);
		attributeValues.add("987987");
		attributeValues.add("123456");
		attributeValues.add("789987");
		checkAttributeValues(DN_MODRDN_DST_AFTER, "telephoneNumber", attributeValues);


		// check ADD

		// the telephoneNumber was merged
		attributeValues = new ArrayList<String>(4);
		attributeValues.add("000000");
		attributeValues.add("11111");
		attributeValues.add("123456");
		attributeValues.add("789987");
		checkAttributeValues(DN_ADD_DST, "telephoneNumber", attributeValues);

	}

	private final void checkSyncResultsCommon() throws Exception {
		List<String> attributeValues = null;

		// check MODRDN
		assertTrue(dstJndiServices.exists(DN_MODRDN_DST_AFTER));
		assertFalse(dstJndiServices.exists(DN_MODRDN_DST_BEFORE));

		// check ADD
		// the object has been created
		assertTrue(dstJndiServices.exists(DN_ADD_DST));
		// the description was copied over
		checkAttributeValue(DN_ADD_DST, "description", "Number three's descriptive text");
		// the password was set and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_ADD_DST, "secretCN0003"));

		// objectClass has inetOrgPerson and all above classes, since it was created with a create_value and MERGE status
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("top");
		attributeValues.add("person");
		attributeValues.add("organizationalPerson");
		attributeValues.add("inetOrgPerson");
		checkAttributeValues(DN_ADD_DST, "objectClass", attributeValues);

		// check MODIFY
		// sn shouldn't have changed
		checkAttributeValue(DN_MODIFY_DST, "sn", "SN0001");
		// the password was set and can be used
		assertTrue(LDAP.canBind(LscConfiguration.getConnection("dst-ldap").getUrl(), DN_MODIFY_DST, "secretCN0001"));
		// the description was copied over since this is an existing object and it description is set to MERGE
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("Number one's descriptive text");
		attributeValues.add(new String((byte[]) srcJndiServices.getEntry(DN_MODIFY_SRC, "objectclass=*").getAttributes().get("userPassword").get()));
		checkAttributeValues(DN_MODIFY_DST, "description", attributeValues);
		// the telephoneNumber was merged with existing values
		attributeValues = new ArrayList<String>(3);
		attributeValues.add("123456");
		attributeValues.add("456789");
		attributeValues.add("789987");
		checkAttributeValues(DN_MODIFY_DST, "telephoneNumber", attributeValues);
		// the objectClass wasn't changed
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("top");
		attributeValues.add("person");
		checkAttributeValues(DN_MODIFY_DST, "objectClass", attributeValues);
		// the givenName was deleted
		attributeValues = new ArrayList<String>();
		checkAttributeValues(DN_MODIFY_DST, "seeAlso", attributeValues);
	}

	@Test
	public final void testCleanLdap2Ldap() throws Exception {
		// make sure the contents of the directory are as we expect to begin with
		assertTrue(dstJndiServices.exists(DN_DELETE_DST));
		assertFalse(srcJndiServices.exists(DN_DELETE_SRC));

		// perform the clean
		launchSyncCleanTask(TASK_NAME, false, false, true);

		// check the results of the clean
		assertFalse(dstJndiServices.exists(DN_DELETE_DST));
	}

	public static void launchSyncCleanTask(String taskName, boolean doAsync, boolean doSync,
					boolean doClean) throws Exception {
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> asyncType = new ArrayList<String>();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();


		if (doAsync) {
			asyncType.add(taskName);
		}
		
		if (doSync) {
			syncType.add(taskName);
		}

		if (doClean) {
			cleanType.add(taskName);
		}

		boolean ret = sync.launch(asyncType, syncType, cleanType);
		assertTrue(ret);
	}

	private void checkAttributeIsEmpty(String dn, String attributeName)
					throws NamingException {
		SearchResult sr = dstJndiServices.readEntry(dn, false);
		assertNull(sr.getAttributes().get(attributeName));
	}

	/**
	 * Get an object from the destination directory, and check that a given attribute
	 * has one value exactly that matches the value provided.
	 * 
	 * In these tests we use this function to read from the source too, since
	 * it is in reality the same directory.
	 * 
	 * @param dn The object to read.
	 * @param attributeName The attribute to check.
	 * @param value The value expected in the attribute.
	 * @throws NamingException
	 */
	private void checkAttributeValue(String dn, String attributeName, String value) throws NamingException {
		SearchResult sr = dstJndiServices.readEntry(dn, false);
		Attribute at = sr.getAttributes().get(attributeName);
		assertNotNull(at);
		assertEquals(1, at.size());

		String realValue = (String) at.get();
		assertTrue(realValue.equals(value));
	}

	/**
	 * Get an object from the destination directory, and check that a given attribute
	 * has n values exactly that matches the values provided.
	 * 
	 * In these tests we use this function to read from the source too, since
	 * it is in reality the same directory.
	 * 
	 * @param dn The object to read.
	 * @param attributeName The attribute to check.
	 * @param expectedValues List of values expected in the attribute.
	 * @throws NamingException
	 */
	private void checkAttributeValues(String dn, String attributeName, List<String> expectedValues) throws NamingException {
		SearchResult sr = dstJndiServices.readEntry(dn, false);
		Attribute at = sr.getAttributes().get(attributeName);
		if (expectedValues.size() > 0) {
			assertNotNull(at);
		} else {
			if (at == null) {
				assertEquals(0, expectedValues.size());
				return;
			}
		}
		assertEquals(expectedValues.size(), at.size());

		// check that each value matches one on one
		for (String expectedValue : expectedValues) {
			assertTrue(at.contains(expectedValue));
		}
		for (int i = 0; i < at.size(); i++) {
			assertTrue(expectedValues.contains((String) at.get(i)));
		}
	}
}
