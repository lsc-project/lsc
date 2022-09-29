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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.lsc.LscDatasets;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launch the jndi tests.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiServicesTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(JndiServicesTest.class);

	private JndiServices dstJndiServices;
	private JndiServices dstRelaxRulesJndiServices;
	private JndiServices dstRecursiveDeleteJndiServices;

	@Before
	public void setup() {
		assertNotNull(LscConfiguration.getConnection("dst-ldap"));
		dstJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap"));
		dstRelaxRulesJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap-relaxrules"));
		dstRecursiveDeleteJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap-recursivedelete"));
	}
	
	/**
	 * Just check that the connection is ready.
	 */
	@Test
	public final void testConnection() {
		assertNotNull(LscConfiguration.getConnection("src-ldap"));
		assertEquals(true, JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("src-ldap")).exists(""));
	}

	@Test
	public final void testGetAttrList() throws NamingException {
		Map<String, LscDatasets> values = null;
		List<String> attrsName = new ArrayList<String>();
		attrsName.add("objectClass");
		values = dstJndiServices.getAttrsList("",
						JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE, attrsName);
		assertEquals(1, values.size());
		assertNotNull(values.get(values.keySet().iterator().next()));
		assertNotNull(dstJndiServices.getSchema(
						new String[]{"objectclasses"}));
	}

	@Test
	public final void testSup() throws NamingException {
		assertEquals(null, dstJndiServices.sup("", -1));
		assertEquals(new ArrayList<String>(), dstJndiServices.sup(
						"ou=People", 1));
		List<String> test2list = new ArrayList<String>();
		test2list.add("ou=test2,ou=test3");
		assertEquals(test2list, dstJndiServices.sup(
						"ou=test1,ou=test2,ou=test3", 1));
		test2list.add(0, "ou=test1,ou=test2,ou=test3");
		assertEquals(test2list, dstJndiServices.sup(
						"ou=test1,ou=test2,ou=test3", 0));
	}

	@Test
	public final void testGetDnList() throws NamingException {
		List<String> test2list = new ArrayList<String>();
		test2list.add("");
		assertEquals(test2list, dstJndiServices.getDnList("",
						JndiServices.DEFAULT_FILTER, SearchControls.OBJECT_SCOPE));

		test2list = new ArrayList<String>();
		test2list.add("uid=00000001,ou=People");
		assertEquals(test2list, dstJndiServices.getDnList("ou=People",
						"objectclass=person", SearchControls.SUBTREE_SCOPE));
	}

	@Test
	public final void testReadEntry() throws NamingException {
		assertNotNull(dstJndiServices.readEntry("", false));
	}

	@Test
	public final void testApplyModifications() throws NamingException {
		String attrName = "description";
		List<String> attrsName = new ArrayList<String>();
		attrsName.add(attrName);
		Map<String, LscDatasets> values = dstJndiServices.getAttrsList("ou=People",
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
		assertTrue(dstJndiServices.apply(jm));

		// this should fail
		Attribute illegalAttr = new BasicAttribute("creatorsName");
		illegalAttr.add("Myself");
		jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
		jm.setDistinguishName("ou=People");
		mi = new ModificationItem(DirContext.ADD_ATTRIBUTE, illegalAttr);
		mis = new ArrayList<ModificationItem>();
		mis.add(mi);
		jm.setModificationItems(mis);
		assertFalse(dstJndiServices.apply(jm));
	}

	private final void createEntryWithChildren(String parent, String name, int currLevel, int maxLevel) throws NamingException {
		JndiModifications jm = new JndiModifications(JndiModificationType.ADD_ENTRY);
		jm.setDistinguishName("cn="+name+"," + parent);
		Attribute objectClass = new BasicAttribute("objectClass", "person");
		Attribute sn = new BasicAttribute("sn", name);
		Attribute cn = new BasicAttribute("cn", name);
		List<ModificationItem> mis = new ArrayList<ModificationItem>();
		mis.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, objectClass));
		mis.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, sn));
		mis.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, cn));
		jm.setModificationItems(mis);
		assertTrue(dstJndiServices.apply(jm));
		if (currLevel < maxLevel) {
			currLevel++;
			int nbChildren = new Random().nextInt(3) + 1;
			for (int child=0; child < nbChildren; child++) {
				createEntryWithChildren("cn="+name+"," + parent, name+child, currLevel, maxLevel);
			}
		}
	}

	@Test
	public final void testDeleteRecursively() throws NamingException {
		// First, add parent entry with children on many levels
		createEntryWithChildren("ou=People", "testDelete", 0, 3);
		JndiModifications jm = new JndiModifications(JndiModificationType.DELETE_ENTRY);
		jm.setDistinguishName("cn=testDelete,ou=People");
		assertFalse("delete entry with children should fail if not using recursiveDelete option", dstJndiServices.apply(jm));
		assertTrue("delete entry with children should succeed using recursiveDelete option", dstRecursiveDeleteJndiServices.apply(jm));
		assertNull("entry should not exist after delete",dstJndiServices.readEntry("cn=testDelete,ou=People", true));
	}

	@Test
	public final void testApplyModificationsRelaxRules() throws NamingException {
		{
			LdapContext ctx = dstRelaxRulesJndiServices.getContext(true);
			boolean hasRelaxRulesCtl = false;
			for (int i=0; i < ctx.getRequestControls().length; i++) {
				if (ctx.getRequestControls()[i].getID().equals(JndiServices.RELAX_RULES_CONTROL_OID)) {
					hasRelaxRulesCtl = true;
					break;
				}
			}
			assertTrue("ctx for updates does not contains relax-rules request control", hasRelaxRulesCtl);
		}
		{
			LdapContext ctx = dstRelaxRulesJndiServices.getContext(false);
			boolean hasRelaxRulesCtl = false;
			for (int i=0; i < ctx.getRequestControls().length; i++) {
				if (ctx.getRequestControls()[i].getID().equals(JndiServices.RELAX_RULES_CONTROL_OID)) {
					hasRelaxRulesCtl = true;
					break;
				}
			}
			assertFalse("ctx not for updates contains relax-rules request control", hasRelaxRulesCtl);
		}
		{
			// This fails as OpenDJ does not support relax-rules control
			Attribute entryUUID = new BasicAttribute("entryUUID");
			entryUUID.add(java.util.UUID.randomUUID().toString());
			JndiModifications jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
			jm.setDistinguishName("ou=People");
			ModificationItem mi = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, entryUUID);
			List<ModificationItem> mis = new ArrayList<ModificationItem>();
			mis.add(mi);
			jm.setModificationItems(mis);
			assertFalse(dstRelaxRulesJndiServices.apply(jm));
		}
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
		Map<String, LscDatasets> results = dstJndiServices.
						getAttrsList("", attrName + "=*", SearchControls.ONELEVEL_SCOPE, attrsName);
		Iterator<String> iter = results.keySet().iterator();
		int i = 0;
		for (; iter.hasNext(); i++) {
			String key = (String) iter.next();
			LscDatasets value = results.get(key);
			LOGGER.debug("key={}, value={}", key, value.getStringValueAttribute(attrName));
		}
		LOGGER.debug(" Final count : {}", i);
	}
	
	public void testAuthenticationThroughJAAS() {
		LoginContext lc = null;
		String user = "";
		String pass = "";

		URL url = getClass().getResource("jaas.conf");
		System.setProperty("java.security.auth.login.config", url.toExternalForm());
		
		try {
	 
		    lc = new LoginContext(JndiServices.class.getName(), JndiServices.getCallbackHandler(user, pass));
		    lc.login();
		} catch (LoginException le) {
		    System.err.println("Authentication attempt failed" + le);
		    System.exit(-1);
		}
		System.out.println("Authenticated via GSS-API");
		//Subject.doAs(lc.getSubject(), new JndiServices());	
	}
}
