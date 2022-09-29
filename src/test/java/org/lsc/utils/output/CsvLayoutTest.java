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
package org.lsc.utils.output;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * Test CSV layout for sfl4j.
 * 
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public class CsvLayoutTest {

	private LoggerContext lc = new LoggerContext();
	private Logger LOGGER = lc.getLogger(CsvLayout.class);

	private ILoggingEvent makeLoggingEvent(String message, Object object) {
		return new LoggingEvent("org.lsc",
						LOGGER, Level.INFO, message,
						new Exception(), new Object[]{object});
	}

	private CsvLayout getDefaultOptionsLayout() {
		CsvLayout layout = new CsvLayout();
		layout.setSeparator(";");
		layout.setLogOperations("create");
		layout.setTaskNames("testTask,otherTestTask");
		layout.setAttrs("givenName;sn;dn;;cn");
		layout.start();
		return layout;
	}

	private LscModifications makeLscModifications(LscModificationType type, String task) {
		LscModifications lm = new LscModifications(type, task);
		lm.setMainIdentifer("cn=test,o=testing");
		List<LscDatasetModification> mi = new ArrayList<LscDatasetModification>();
		mi.add(new LscDatasetModification(LscDatasetModificationType.ADD_VALUES, "givenName", Arrays.asList(new Object[] { "Jon" } )));
		mi.add(new LscDatasetModification(LscDatasetModificationType.ADD_VALUES, "cn", Arrays.asList(new Object[] { "Tester CN" } )));
		lm.setLscAttributeModifications(mi);
		return lm;
	}

	@Test
	public void testParameterHandling() {
		// set some initial options and activate them
		CsvLayout layout = getDefaultOptionsLayout();

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
		assertEquals(1, layout.getLogOperations().size());
		assertEquals(true, layout.getLogOperations().contains(LscModificationType.CREATE_OBJECT));
	}

	@Test
	public void testEmptyLogging() {
		CsvLayout layout = getDefaultOptionsLayout();
		ILoggingEvent event = makeLoggingEvent("random string", null);		
		assertEquals("", layout.doLayout(event));
	}

	@Test
	public void testBasicLogging() {
		CsvLayout layout = getDefaultOptionsLayout();

		// create a JndiModifications object to test logging
		LscModifications jm = makeLscModifications(LscModificationType.CREATE_OBJECT, "testTask");

		// test simple logging of the modifications for a valid task
		ILoggingEvent event = makeLoggingEvent(jm.toString(), jm);
		assertEquals("Jon;;cn=test,o=testing;;Tester CN\n", layout.doLayout(event));
	}

	@Test
	public void testExcludedTaskLogging() {
		CsvLayout layout = getDefaultOptionsLayout();

		// change the task name to test logging a modification for an excluded task
		LscModifications jm = makeLscModifications(LscModificationType.CREATE_OBJECT, "notInList");

		ILoggingEvent event = makeLoggingEvent(jm.toString(), jm);
		assertEquals("", layout.doLayout(event));
	}

	@Test
	public void testExcludedOperationLogging() {
		CsvLayout layout = getDefaultOptionsLayout();

		// go back to a valid task, but try an excluded operation
		LscModifications jm = makeLscModifications(LscModificationType.UPDATE_OBJECT, "testTask");
		
		ILoggingEvent event = makeLoggingEvent(jm.toString(), jm);
		assertEquals("", layout.doLayout(event));
	}

	@Test
	public void testHeader() {
		CsvLayout layout = getDefaultOptionsLayout();

		// change options and reactivate them
		layout.setLogOperations("create,update");
		layout.setSeparator("%");
		layout.setAttrs("givenName%sn%dn%%cn");
		layout.setOutputHeader(true);
		layout.start();

		LscModifications jm = makeLscModifications(LscModificationType.CREATE_OBJECT, "testTask");

		// log one line to check that the outputHeader is prepended
		ILoggingEvent event = makeLoggingEvent(jm.toString(), jm);
		assertEquals("givenName%sn%dn%%cn\n", layout.getHeader());
		assertEquals("Jon%%cn=test,o=testing%%Tester CN\n", layout.doLayout(event));

		// log the same line again to check that the outputHeader is not logged again
		event = makeLoggingEvent(jm.toString(), jm);
		assertEquals("Jon%%cn=test,o=testing%%Tester CN\n", layout.doLayout(event));
	}
}
