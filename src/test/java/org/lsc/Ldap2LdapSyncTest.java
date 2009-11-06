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
package org.lsc;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import junit.framework.TestCase;

import org.lsc.beans.AbstractBean;
import org.lsc.beans.personBean;
import org.lsc.jndi.JndiServices;
import org.lsc.jndi.SimpleJndiSrcService;
import org.lsc.service.ISrcService;
import org.lsc.utils.I18n;
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
public class Ldap2LdapSyncTest extends TestCase
{
	private final String TASK_NAME = "ldap2ldapTestTask";

	private final String DN_ADD_SRC = "cn=CN0003,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_ADD_DST = "cn=CN0003,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_MODIFY_SRC = "cn=CN0001,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_MODIFY_DST = "cn=CN0001,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_DELETE_SRC = "cn=CN0004,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_DELETE_DST = "cn=CN0004,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_MODRDN_SRC = "cn=CN0002,ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_MODRDN_DST_BEFORE = "cn=CommonName0002,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	private final String DN_MODRDN_DST_AFTER = "cn=CN0002,ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		// force the locale to en_US to avoid I18N errors with foreign LANG
		I18n.setLocale(Locale.US);
	}

	/**
	 * Test reading the userPassword attribute from our source directory through Object
	 * and Bean. This attribute has a binary syntax, so we must confirm we can parse it as a String.
	 * @throws NamingException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public final void testReadUserPasswordFromLdap() throws NamingException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Map<String, LscAttributes> ids = new HashMap<String, LscAttributes>(1);
		Map<String, String> attributeValues = new HashMap<String, String>(1);
		attributeValues.put("sn", "SN0001");
		ids.put(DN_MODIFY_SRC, new LscAttributes(attributeValues));

		ISrcService srcService = new SimpleJndiSrcService(Configuration.getAsProperties("lsc.tasks." +  TASK_NAME + ".srcService"));
		AbstractBean srcBean = srcService.getBean(new personBean(), ids.entrySet().iterator().next());
		String userPassword = srcBean.getAttributeFirstValueById("userPassword");
		
		// OpenDS automatically hashes the password using seeded SHA,
		// so we can't test the full value, just the beginning.
		// This is sufficient to confirm we can read the attribute as a String.
		assertTrue(userPassword.startsWith("{SSHA}"));
	}

	public final void testSyncLdap2Ldap() throws Exception
	{
		
		// make sure the contents of the directory are as we expect to begin with

		// check MODRDN
		assertTrue(JndiServices.getSrcInstance().exists(DN_MODRDN_SRC));
		assertTrue(JndiServices.getDstInstance().exists(DN_MODRDN_DST_BEFORE));
		assertFalse(JndiServices.getDstInstance().exists(DN_MODRDN_DST_AFTER));

		// check ADD
		assertTrue(JndiServices.getSrcInstance().exists(DN_ADD_SRC));
		assertFalse(JndiServices.getDstInstance().exists(DN_ADD_DST));
		checkAttributeIsEmpty(DN_ADD_SRC, "userPassword");
		checkAttributeIsEmpty(DN_ADD_SRC, "telephoneNumber");
		checkAttributeValue(DN_ADD_SRC, "description", "Number three's descriptive text");
		checkAttributeValue(DN_ADD_SRC, "sn", "SN0003");
		
		// check MODIFY
		assertTrue(JndiServices.getSrcInstance().exists(DN_MODIFY_SRC));
		assertTrue(JndiServices.getDstInstance().exists(DN_MODIFY_DST));
		checkAttributeIsEmpty(DN_MODIFY_SRC, "telephoneNumber");
		checkAttributeValue(DN_MODIFY_SRC, "description", "Number one's descriptive text");
		checkAttributeValue(DN_MODIFY_SRC, "sn", "SN0001");
		// the original password is present and can be used
		assertTrue(LDAP.canBind(Configuration.getSrcProperties().getProperty("java.naming.provider.url"), DN_MODIFY_SRC, "secret0001"));
		// the new password can not be used yet
		assertFalse(LDAP.canBind(Configuration.getSrcProperties().getProperty("java.naming.provider.url"), DN_MODIFY_SRC, "secretCN0001"));
		assertFalse(LDAP.canBind(Configuration.getSrcProperties().getProperty("java.naming.provider.url"), DN_MODIFY_DST, "secretCN0001"));

		// perform the sync
		launchSyncCleanTask(TASK_NAME, true, false);

		// check the results of the synchronization
		checkSyncResultsFirstPass();
		
		// sync again to confirm convergence
		launchSyncCleanTask(TASK_NAME, true, false);

		// check the results of the synchronization
		checkSyncResultsSecondPass();		

		// sync a third time to make sure nothing changed
		launchSyncCleanTask(TASK_NAME, true, false);

		// check the results of the synchronization
		checkSyncResultsSecondPass();		
	}

	private final void checkSyncResultsFirstPass() throws Exception
	{
		List<String> attributeValues = null;
		
		checkSyncResultsCommon();
		
		// check ADD

		// the telephoneNumber was created
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("000000");
		attributeValues.add("11111");
		checkAttributeValues(DN_ADD_DST, "telephoneNumber", attributeValues);

	}
	
	private final void checkSyncResultsSecondPass() throws Exception
	{
		List<String> attributeValues = null;
		
		checkSyncResultsCommon();
		
		// check MODRDN
		
		// the password was set and can be used
		assertTrue(LDAP.canBind(Configuration.getDstProperties().getProperty("java.naming.provider.url"), DN_MODRDN_DST_AFTER, "secretCN0002"));

		// the description was copied over since this is an existing object and it description is set to MERGE
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("Number two's descriptive text");
		attributeValues.add(new String((byte[]) JndiServices.getSrcInstance().getEntry(DN_MODRDN_SRC, "objectclass=*").getAttributes().get("userPassword").get()));
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

	
	private final void checkSyncResultsCommon() throws Exception
	{
		List<String> attributeValues = null;
		
		// check MODRDN
		assertTrue(JndiServices.getDstInstance().exists(DN_MODRDN_DST_AFTER));
		assertFalse(JndiServices.getDstInstance().exists(DN_MODRDN_DST_BEFORE));

		// check ADD
		// the object has been created
		assertTrue(JndiServices.getDstInstance().exists(DN_ADD_DST));
		// the description was copied over
		checkAttributeValue(DN_ADD_DST, "description", "Number three's descriptive text");
		// the password was set and can be used
		assertTrue(LDAP.canBind(Configuration.getDstProperties().getProperty("java.naming.provider.url"), DN_ADD_DST, "secretCN0003"));
		// the objectClass contains organizationalPerson, since this object wasn't created
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("top");
		attributeValues.add("person");
		checkAttributeValues(DN_ADD_DST, "objectClass", attributeValues);

		// check MODIFY
		// sn shouldn't have changed
		checkAttributeValue(DN_MODIFY_DST, "sn", "SN0001");
		// the password was set and can be used
		assertTrue(LDAP.canBind(Configuration.getDstProperties().getProperty("java.naming.provider.url"), DN_MODIFY_DST, "secretCN0001"));
		// the description was copied over since this is an existing object and it description is set to MERGE
		attributeValues = new ArrayList<String>(2);
		attributeValues.add("Number one's descriptive text");
		attributeValues.add(new String((byte[]) JndiServices.getSrcInstance().getEntry(DN_MODIFY_SRC, "objectclass=*").getAttributes().get("userPassword").get()));
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
	
	public final void testCleanLdap2Ldap() throws Exception
	{
		// make sure the contents of the directory are as we expect to begin with
		assertTrue(JndiServices.getDstInstance().exists(DN_DELETE_DST));
		assertFalse(JndiServices.getSrcInstance().exists(DN_DELETE_SRC));

		// perform the clean
		launchSyncCleanTask(TASK_NAME, false, true);

		// check the results of the clean
		assertFalse(JndiServices.getDstInstance().exists(DN_DELETE_DST));
	}

	private void launchSyncCleanTask(String taskName, boolean doSync,
			boolean doClean) throws Exception
	{
		// initialize required stuff
		SimpleSynchronize sync = new SimpleSynchronize();
		List<String> syncType = new ArrayList<String>();
		List<String> cleanType = new ArrayList<String>();

		if (doSync)
		{
			syncType.add(taskName);
		}

		if (doClean)
		{
			cleanType.add(taskName);
		}

		boolean ret = sync.launch(syncType, cleanType);
		assertTrue(ret);
	}

	private void checkAttributeIsEmpty(String dn, String attributeName)
			throws NamingException
	{
		SearchResult sr = JndiServices.getDstInstance().readEntry(dn, false);
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
	private void checkAttributeValue(String dn, String attributeName, String value) throws NamingException
	{
		SearchResult sr = JndiServices.getDstInstance().readEntry(dn, false);
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
	 * @param value The value expected in the attribute.
	 * @throws NamingException
	 */
	private void checkAttributeValues(String dn, String attributeName, List<String> expectedValues) throws NamingException
	{
		SearchResult sr = JndiServices.getDstInstance().readEntry(dn, false);
		Attribute at = sr.getAttributes().get(attributeName);
		if (expectedValues.size() > 0) {
			assertNotNull(at);
		} else {
			if (at==null) {
				assertEquals(0, expectedValues.size());
				return;
			}
		}
		assertEquals(expectedValues.size(), at.size());
		
		// check that each value matches one on one
		for (String expectedValue : expectedValues)
		{
			assertTrue(at.contains(expectedValue));
		}
		for (int i=0; i<at.size(); i++)
		{
			assertTrue(expectedValues.contains(at.get(i)));
		}
	}

}
