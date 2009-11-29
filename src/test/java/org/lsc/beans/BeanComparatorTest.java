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

import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.swing.text.SimpleAttributeSet;

import org.lsc.beans.syncoptions.ForceSyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.jndi.JndiModifications;

import junit.framework.TestCase;

public class BeanComparatorTest extends TestCase
{

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

}
