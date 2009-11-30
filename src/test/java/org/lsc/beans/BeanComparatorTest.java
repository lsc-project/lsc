/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2009, LSC Project 
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
package org.lsc.beans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.BasicAttribute;

import junit.framework.TestCase;

import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

public class BeanComparatorTest extends TestCase
{

	/**
	 * Test method for {@link org.lsc.beans.BeanComparator#calculateModificationType(ISyncOptions, IBean, IBean, Object)}.
	 */
	public void testCalculateModificationType() {
		dummySyncOptions syncOptions = new dummySyncOptions();
		IBean srcBean = new personBean();
		IBean dstBean = new personBean();

		try {
			// test null and null --> null
			assertNull(BeanComparator.calculateModificationType(syncOptions, null, null, null));
			
			// test not null and null --> add
			assertEquals(JndiModificationType.ADD_ENTRY, BeanComparator.calculateModificationType(syncOptions, srcBean, null, null));
			
			// test null and not null --> delete
			assertEquals(JndiModificationType.DELETE_ENTRY, BeanComparator.calculateModificationType(syncOptions, null, dstBean, null));
			
			// test both not null, and syncoptions to make DNs identical --> modify
			syncOptions.setDn("\"destination DN\"");
			dstBean.setDistinguishName("destination DN");
			assertEquals(JndiModificationType.MODIFY_ENTRY, BeanComparator.calculateModificationType(syncOptions, srcBean, dstBean, null));

			// test both not null, with different DNs and no DN in syncoptions --> modrdn
			syncOptions.setDn(null);
			srcBean.setDistinguishName("source DN");
			dstBean.setDistinguishName("destination DN");
			assertEquals(JndiModificationType.MODRDN_ENTRY, BeanComparator.calculateModificationType(syncOptions, srcBean, dstBean, null));
		}
		catch (CloneNotSupportedException e) {
			assertTrue(e.toString(), false);
		}
	}

	
	/**
	 * This test ensures that a source bean containing fields with only
	 * empty string values is not output as a modification to be applied 
	 * to the destination.
	 * 
	 * If this is not the case, LDAP operations follow for LDIF like this:
	 * modificationType: add
	 * add: sn
	 * sn: 
	 * 
	 * With an invalid syntax error.
	 */
	public void testCalculateModificationsWithEmptyFields()
	{
		ISyncOptions syncOptions = new ForceSyncOptions();
		IBean srcBean, destBean;
		Object customLibrary = null;
		boolean condition = true;
		
		// test add
		srcBean = new personBean();
		srcBean.setDistinguishName("something");
		srcBean.setAttribute(new BasicAttribute("sn", ""));
		srcBean.setAttribute(new BasicAttribute("cn", "real cn"));
		destBean = null;
		
		try {
			JndiModifications jm = BeanComparator.calculateModifications(syncOptions, srcBean, destBean, customLibrary, condition);
			
			assertEquals("something", jm.getDistinguishName());
			assertEquals(1, jm.getModificationItems().size());
			assertEquals("cn", jm.getModificationItems().get(0).getAttribute().getID());
			assertEquals("real cn", jm.getModificationItems().get(0).getAttribute().get());
			
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
		
		// test mod
		destBean = new personBean();
		destBean.setDistinguishName("something");
		destBean.setAttribute(new BasicAttribute("cn", "old cn"));
		
		try {
			JndiModifications jm = BeanComparator.calculateModifications(syncOptions, srcBean, destBean, customLibrary, condition);
			
			assertEquals("something", jm.getDistinguishName());
			assertEquals(1, jm.getModificationItems().size());
			assertEquals("cn", jm.getModificationItems().get(0).getAttribute().getID());
			assertEquals("real cn", jm.getModificationItems().get(0).getAttribute().get());
			
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	public class dummySyncOptions implements ISyncOptions {

		Map<String, List<String>> createValuesMap;
		Map<String, List<String>> defaultValuesMap; 
		Map<String, List<String>> forceValuesMap;
		Map<String, STATUS_TYPE> statusMap;
		String dn;
		
		public dummySyncOptions(Map<String, List<String>> createValuesMap,
				Map<String, List<String>> defaultValuesMap, 
				Map<String, List<String>> forceValuesMap,
				Map<String, STATUS_TYPE> statusMap)
		{
			if (createValuesMap != null) this.createValuesMap = createValuesMap;
			else this.createValuesMap = new HashMap<String, List<String>>();
			
			if (defaultValuesMap != null) this.defaultValuesMap = defaultValuesMap;
			else this.defaultValuesMap = new HashMap<String, List<String>>();

			if (forceValuesMap != null) this.forceValuesMap = forceValuesMap;
			else this.forceValuesMap = new HashMap<String, List<String>>();
			
			if (statusMap != null) this.statusMap = statusMap;
			else this.statusMap = new HashMap<String, STATUS_TYPE>();

		}
		
		public dummySyncOptions() {
			// TODO Auto-generated constructor stub
		}

		public String getCondition(JndiModificationType operation)
		{
			// TODO Auto-generated method stub
			return null;
		}

		public Set<String> getCreateAttributeNames()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public String getCreateCondition()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public List<String> getCreateValues(String id, String attributeName)
		{
			return createValuesMap.get(attributeName);
		}

		public Set<String> getDefaultValuedAttributeNames()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public List<String> getDefaultValues(String id, String attributeName)
		{
			return defaultValuesMap.get(attributeName);
		}

		public String getDeleteCondition()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public void setDn(String dn)
		{
			this.dn = dn;
		}

		public String getDn()
		{
			return dn;
		}

		public Set<String> getForceValuedAttributeNames()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public List<String> getForceValues(String id, String attributeName)
		{
			return forceValuesMap.get(attributeName);
		}

		public String getModrdnCondition()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public STATUS_TYPE getStatus(String id, String attributeName)
		{
			return statusMap.get(attributeName);
		}

		public String getTaskName()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public String getUpdateCondition()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public List<String> getWriteAttributes()
		{
			// TODO Auto-generated method stub
			return null;
		}

		public void initialize(String taskname)
		{
			// TODO Auto-generated method stub
			
		}
		
	}

}
