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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.jndi.JndiServices;

/**
 * This test case attempts to reproduce a typical ldap2ldap setup via
 * SimpleSynchronize. This relies on settings in lsc.properties from the test
 * resources, and entries in the local OpenDS directory. testSync() performs a
 * synchronization between two branches of the local LDAP server, and should
 * perform 3 operations : 1 ADD, 1 MODRDN, 1 MODIFY. The
 * 
 * @author Jonathan Clarke &ltjonathan@phillipoux.net&gt;
 */
public class CommonLdapSyncTest {

	public final static String TASK_NAME = "ldap2ldapTestTask";
	public final static String SOURCE_DN = "ou=ldap2ldap2TestTaskSrc,ou=Test Data,dc=lsc-project,dc=org";
	public final static String DESTINATION_DN = "ou=ldap2ldap2TestTaskDst,ou=Test Data,dc=lsc-project,dc=org";
	
	public String getTaskName() {
		return TASK_NAME;
	}
	
	public String getSourceDn() {
		return SOURCE_DN;
	}
	
	public String getDestinationDn() {
		return DESTINATION_DN;
	}
	
	public String DN_ADD_SRC = "cn=CN0003," + getSourceDn(); 
	public String DN_ADD_DST = "cn=CN0003," + getDestinationDn();
	public String DN_MODIFY_SRC = "cn=CN0001," + getSourceDn();
	public String DN_MODIFY_DST = "cn=CN0001," + getDestinationDn();
	public String DN_DELETE_SRC = "cn=CN0004," + getSourceDn();
	public String DN_DELETE_DST = "cn=CN0004," + getDestinationDn();
	public String DN_MODRDN_SRC = "cn=CN0002," + getSourceDn();
	public String DN_MODRDN_DST_BEFORE = "cn=CommonName0002," + getDestinationDn();
	public String DN_MODRDN_DST_AFTER = "cn=CN0002," + getDestinationDn();

	protected JndiServices srcJndiServices;
	
	protected JndiServices dstJndiServices;
	
	protected void reloadJndiConnections() {
		srcJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("src-ldap"));
		dstJndiServices = JndiServices.getInstance((LdapConnectionType)LscConfiguration.getConnection("dst-ldap"));
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
	public void checkAttributeValues(String dn, String attributeName, List<String> expectedValues) throws NamingException {
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

	public void checkAttributeIsEmpty(String dn, String attributeName)
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
	public void checkAttributeValue(String dn, String attributeName, String value) throws NamingException {
		SearchResult sr = dstJndiServices.readEntry(dn, false);
		Attribute at = sr.getAttributes().get(attributeName);
		assertNotNull(at);
		assertEquals(1, at.size());

		String realValue = (String) at.get();
		assertEquals(value, realValue);
	}
}
