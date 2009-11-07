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
package org.lsc.utils.log4j.csv;

import org.lsc.utils.output.csv.CsvLayout;
import java.util.ArrayList;
import java.util.List;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

/**
 * Test CSV layout for log4j.
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class CsvLayoutTest extends TestCase {

	public final void testParameterHandling() {
		CsvLayout layout = new CsvLayout();
		
		// set some initial options and activate them
		layout.setSeparator(";");
		layout.setLogOperation("create");
		layout.setTaskNames("testTask,otherTestTask");
		layout.setAttrs("givenName;sn;dn;;cn");
		
		layout.activateOptions();
		
		// test that all attributes passed as a string have been interpreted
		assertEquals(5, layout.attributes.size());
		assertEquals(true, layout.attributes.contains("givenname"));
		assertEquals(true, layout.attributes.contains("sn"));
		assertEquals(true, layout.attributes.contains("dn"));
		assertEquals(true, layout.attributes.contains("cn"));
		
		// test that all task names passed as a string have been interpreted
		assertEquals(2, layout.taskNamesList.size());
		assertEquals(true, layout.taskNamesList.contains("testTask".toLowerCase()));
		assertEquals(true, layout.taskNamesList.contains("otherTestTask".toLowerCase()));
		
		// test that all task names passed as a string have been interpreted		
		assertEquals(1, layout.operations.size());
		assertEquals(true, layout.operations.contains(JndiModificationType.ADD_ENTRY));
	
		// test logging
		assertEquals("", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, "random string", new UnknownError())));

		// create a JndiModifications object to test logging
		JndiModifications jm = new JndiModifications(JndiModificationType.ADD_ENTRY, "testTask");
		jm.setDistinguishName("cn=test,o=testing");
		List<ModificationItem> mi = new ArrayList<ModificationItem>();
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("givenName", "Jon")));
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("cn", "Tester CN")));
		jm.setModificationItems(mi);
		
		// test simple logging of the modifications for a valid task
		assertEquals("Jon;;cn=test,o=testing;;Tester CN\n", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, jm, new UnknownError())));
		
		// change the task name to test logging a modification for an excluded task
		jm.setTaskName("notInList");
		assertEquals("", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, jm, new UnknownError())));

		// go back to a valid task, but try an excluded operation
		jm.setTaskName("otherTestTask");
		jm.setOperation(JndiModificationType.MODIFY_ENTRY);
		assertEquals("", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, jm, new UnknownError())));
		
		// change options and reactivate them
		layout.setLogOperation("create,update");
		layout.setSeparator("%");
		layout.setAttrs("givenName%sn%dn%%cn");
		layout.setOutputHeader("true");
		layout.activateOptions();
		
		// log one line to check that the outputHeader is prepended
		assertEquals("givenName%sn%dn%%cn\nJon%%cn=test,o=testing%%Tester CN\n", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, jm, new UnknownError())));
		
		// log the same line again to check that the outputHeader is not logged again
		assertEquals("Jon%%cn=test,o=testing%%Tester CN\n", layout.format(new LoggingEvent("dunno", LoggerFactory.getLogger(CsvLayoutTest.class), Level.INFO, jm, new UnknownError())));
		
	}
	
}
