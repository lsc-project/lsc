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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jndi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.LscAttributes;
import org.lsc.jndi.JndiServices;

import org.junit.Test;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launch the jndi tests.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiServicesTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(JndiServicesTest.class);

	/**
	 * Just check that the connection is ready.
	 */
	@Test
	public final void testConnection() {
		assertEquals(true, JndiServices.getDstInstance().exists(""));
	}

	@Test
	public final void testConnectionCache() {
		assertEquals(JndiServices.getDstInstance(), JndiServices.getDstInstance());
	}

	@Test
	public final void testGetAttrList() throws NamingException {
		Map<String, LscAttributes> values = null;
		List<String> attrsName = new ArrayList<String>();
		attrsName.add("objectClass");
		values = JndiServices.getDstInstance().getAttrsList("",
						JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE, attrsName);
		assertEquals(1, values.size());
		assertNotNull(values.get(values.keySet().iterator().next()));
		assertNotNull(JndiServices.getDstInstance().getSchema(
						new String[]{"objectclasses"}));
	}

	@Test
	public final void testSup() throws NamingException {
		assertEquals(null, JndiServices.getDstInstance().sup("", -1));
		assertEquals(new ArrayList<String>(), JndiServices.getDstInstance().sup(
						"ou=People", 1));
		List<String> test2list = new ArrayList<String>();
		test2list.add("ou=test2,ou=test3");
		assertEquals(test2list, JndiServices.getDstInstance().sup(
						"ou=test1,ou=test2,ou=test3", 1));
		test2list.add(0, "ou=test1,ou=test2,ou=test3");
		assertEquals(test2list, JndiServices.getDstInstance().sup(
						"ou=test1,ou=test2,ou=test3", 0));
	}

	@Test
	public final void testGetDnList() throws NamingException {
		List<String> test2list = new ArrayList<String>();
		test2list.add("");
		assertEquals(test2list, JndiServices.getDstInstance().getDnList("",
						JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE));

		test2list = new ArrayList<String>();
		test2list.add("uid=00000001,ou=People");
		assertEquals(test2list, JndiServices.getDstInstance().getDnList("ou=People",
						"objectclass=person", SearchControls.SUBTREE_SCOPE));
	}

	@Test
	public final void testReadEntry() throws NamingException {
		assertNotNull(JndiServices.getDstInstance().readEntry("", false));
	}

	@Test
	public final void testApplyModifications() throws NamingException {
		String attrName = "description";
		List<String> attrsName = new ArrayList<String>();
		attrsName.add(attrName);
		Map<String, LscAttributes> values = JndiServices.getDstInstance().getAttrsList("ou=People",
						JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE, attrsName);
		Attribute descAttr = new BasicAttribute(attrName);
		String descValue = (String) values.get(values.keySet().iterator().next()).getStringValueAttribute(attrName);
		try {
			int n = Integer.parseInt(descValue.substring(descValue.length() - 1));
			n += 1;
			descValue = descValue.substring(0, descValue.length() - 1) + n;
		} catch (NumberFormatException e) {
			descValue = descValue + "-1";
		}
		descAttr.add(descValue);
		JndiModifications jm = new JndiModifications(
						JndiModificationType.MODIFY_ENTRY);
		jm.setDistinguishName("ou=People");
		ModificationItem mi = new ModificationItem(
						DirContext.REPLACE_ATTRIBUTE, descAttr);
		List<ModificationItem> mis = new ArrayList<ModificationItem>();
		mis.add(mi);
		jm.setModificationItems(mis);
		assertTrue(JndiServices.getDstInstance().apply(jm));

		// this should fail
		Attribute illegalAttr = new BasicAttribute("creatorsName");
		illegalAttr.add("Myself");
		jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
		jm.setDistinguishName("ou=People");
		mi = new ModificationItem(DirContext.ADD_ATTRIBUTE, illegalAttr);
		mis = new ArrayList<ModificationItem>();
		mis.add(mi);
		jm.setModificationItems(mis);
		assertFalse(JndiServices.getDstInstance().apply(jm));
	}

	/**
	 * Test the retrieve of the complete directory.
	 */
	@Test
	public final void testAttrPagedResultsList() throws NamingException {
		String attrName = "objectClass";
		LOGGER.debug("Counting all the directory entries ...");
		List<String> attrsName = new ArrayList<String>();
		attrsName.add(attrName);
		Map<String, LscAttributes> results = JndiServices.getDstInstance().
						getAttrsList("", attrName + "=*", SearchControls.ONELEVEL_SCOPE, attrsName);
		Iterator<String> iter = results.keySet().iterator();
		int i = 0;
		for (; iter.hasNext(); i++) {
			String key = (String) iter.next();
			LscAttributes value = results.get(key);
			LOGGER.debug("key={}, value={}", key, value.getStringValueAttribute(attrName));
		}
		LOGGER.debug(" Final count : {}", i);
	}
	
	/**
	 * Tests {@link JndiServices#getDnList(String, String, int)}
	 * @throws NamingException
	 */
	@Test
	public final void testSlashesInDnInGetDnListForResults() throws NamingException {
		List<String> list = JndiServices.getDstInstance().getDnList("ou=Test Data", "(objectClass=person)", SearchControls.ONELEVEL_SCOPE);
		assertNotNull(list);
		assertTrue(list.size() >= 1);
		assertTrue(list.contains("cn=One / One,ou=Test Data"));
	}
	
	/**
	 * Tests {@link JndiServices#getDnList(String, String, int)}
	 * @throws NamingException
	 */
	@Test
	public final void testSlashesInDnInGetDnListForSearchBase() throws NamingException {
		List<String> list = JndiServices.getDstInstance().getDnList("cn=One / One,ou=Test Data", "(objectClass=person)", SearchControls.OBJECT_SCOPE);
		assertNotNull(list);
		assertTrue(1 == list.size());
		assertTrue(list.contains("cn=One / One,ou=Test Data"));
	}
	
	/**
	 * Tests {@link JndiServices#getDnList(String, String, int)}
	 * @throws NamingException
	 */
	@Test
	public final void testSlashesInDnInGetDnListForSearchBaseAndResults() throws NamingException {
		List<String> list = JndiServices.getDstInstance().getDnList("cn=One / One,ou=Test Data", "(objectClass=person)", SearchControls.ONELEVEL_SCOPE);
		assertNotNull(list);
		assertTrue(1 == list.size());
		assertTrue(list.contains("cn=OneFriend,cn=One / One,ou=Test Data"));
	}

	/**
	 * Tests {@link JndiServices#readEntry(String, boolean)}
	 * @throws NamingException
	 */
	@Test
	public final void testSlashesInDnInReadEntry() throws NamingException {
		SearchResult sr = JndiServices.getDstInstance().readEntry("cn=One / One,ou=Test Data", false);
		assertNotNull(sr);
		assertEquals("cn=One / One,ou=Test Data,dc=lsc-project,dc=org", sr.getNameInNamespace());
	}

	/**
	 * Test {@link JndiServices#getEntry(String, String)}
	 * @throws NamingException 
	 */
	@Test
	public final void testGetEntry() throws NamingException {
		SearchResult sr = JndiServices.getDstInstance().getEntry("ou=Test Data", "sn=One One");
		assertNotNull(sr);
		assertEquals("cn=One / One,ou=Test Data,dc=lsc-project,dc=org", sr.getNameInNamespace());
	}
	
	/**
	 * Test {@link JndiServices#getEntry(String, String)}
	 * @throws NamingException 
	 */
	@Test(expected=SizeLimitExceededException.class)
	public final void testGetEntryMultipleEntries() throws NamingException {
		@SuppressWarnings("unused")
		SearchResult sr = JndiServices.getDstInstance().getEntry("ou=Test Data", "objectClass=person");
	}

	/**
	 * Test {@link JndiServices#getEntry(String, String)}
	 * @throws NamingException 
	 */
	@Test
	public final void testGetEntryWithSlashesInDnInSearchBase() throws NamingException {
		SearchResult sr = JndiServices.getDstInstance().getEntry("cn=One / One,ou=Test Data", "sn=One One");
		assertNotNull(sr);
		assertEquals("cn=One / One,ou=Test Data,dc=lsc-project,dc=org", sr.getNameInNamespace());
	}

	/**
	 * Test {@link JndiServices#exists(String)}
	 * @throws NamingException 
	 */
	@Test
	public final void testExistsWithSlashesInDnInSearchBase() throws NamingException {
		boolean res = JndiServices.getDstInstance().exists("cn=One / One,ou=Test Data");
		assertTrue(res);
	}
	
	/**
	 * Test {@link JndiServices#sup(String, int)}
	 * @throws NamingException 
	 */
	@Test
	public final void testSupWithSlashesInDn() throws NamingException {
		List<String> res = JndiServices.getDstInstance().sup("cn=OneFriend,cn=One / One,ou=Test Data", 1);
		assertNotNull(res);
		assertEquals(1, res.size());
		assertEquals("cn=One / One,ou=Test Data", res.get(0));
	}

	
}
