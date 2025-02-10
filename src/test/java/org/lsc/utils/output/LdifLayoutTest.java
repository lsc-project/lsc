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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasetModification.LscDatasetModificationType;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * Provide a complete test.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdifLayoutTest {

	private LoggerContext lc = new LoggerContext();
	private Logger LOGGER = lc.getLogger(LdifLayout.class);

	private ILoggingEvent makeLoggingEvent(String message, Object object) {
		return new LoggingEvent("org.lsc",
						LOGGER, Level.INFO, message,
						new Exception(), new Object[]{object});
	}

	/**
	 * Launch a add entry layout test.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testAdd() throws IOException {
		try (MockedConstruction<Date> dateCtr = Mockito.mockConstruction(Date.class,(mock,context)-> {
			when(mock.toString()).thenReturn("Wed Dec 12 16:25:01 CET 2012");})) {
			List<LscDatasetModification> mi = new ArrayList<LscDatasetModification>();
			mi.add(new LscDatasetModification(
					LscDatasetModificationType.ADD_VALUES, "cn", Arrays.asList(new Object[] {"name"})));
			mi.add(new LscDatasetModification(
					LscDatasetModificationType.ADD_VALUES, "sn", Arrays.asList(new Object[] {"<non safe string>"})));
			mi.add(new LscDatasetModification(
					LscDatasetModificationType.ADD_VALUES, "givenName", Arrays.asList(new Object[] {"Sébastien"})));
			mi.add(new LscDatasetModification(
					LscDatasetModificationType.ADD_VALUES, "description", Arrays.asList(new Object[] {""})));
	
			LscModifications jm = new LscModifications(LscModificationType.CREATE_OBJECT);
			jm.setMainIdentifer("givenName=Sébastien,dc=lsc-project,dc=org");
			jm.setLscAttributeModifications(mi);
	
			ILoggingEvent loggingEvent = makeLoggingEvent(jm.toString(), jm);
	
			LdifLayout layout = new LdifLayout();
			layout.setContext(lc);
			layout.setPattern("%m%n");
			layout.start();
	
			assertEquals(
				"# Wed Dec 12 16:25:01 CET 2012\n"
				+ "dn:: Z2l2ZW5OYW1lPVPDqWJhc3RpZW4sZGM9bHNjLXByb2plY3QsZGM9b3Jn\n"
				+ "changetype: add\n"
				+ "cn: name\n"
				+ "sn:: PG5vbiBzYWZlIHN0cmluZz4=\n"
				+ "givenName:: U8OpYmFzdGllbg==\n"
				+ "description: \n\n",
				layout.doLayout(loggingEvent));
	
			jm.setMainIdentifer("dc=lsc-project,dc=org");
			assertEquals(
				"# Wed Dec 12 16:25:01 CET 2012\n"
				+ "dn: dc=lsc-project,dc=org\n"
				+ "changetype: add\ncn: name\n"
				+ "sn:: PG5vbiBzYWZlIHN0cmluZz4=\n"
				+ "givenName:: U8OpYmFzdGllbg==\n"
				+ "description: \n\n",
				layout.doLayout(loggingEvent));
		}
	}

	/**
	 * Launch a modify entry layout test.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testModify() throws IOException {
		try (MockedConstruction<Date> dateCtr = Mockito.mockConstruction(Date.class,(mock,context)-> {
			when(mock.toString()).thenReturn("Wed Dec 12 16:25:01 CET 2012");})) {		List<LscDatasetModification> mi = new ArrayList<LscDatasetModification>();
			mi.add(new LscDatasetModification(LscDatasetModificationType.REPLACE_VALUES, "cn", Arrays.asList(new Object[] {"new_name"})));
			mi.add(new LscDatasetModification(LscDatasetModificationType.DELETE_VALUES, "uid", Arrays.asList(new Object[] {"old_id"})));
			mi.add(new LscDatasetModification(LscDatasetModificationType.REPLACE_VALUES, "sn", Arrays.asList(new Object[] {"À là bas"})));
			mi.add(new LscDatasetModification(LscDatasetModificationType.ADD_VALUES, "description", Arrays.asList(new Object[] {"Multi-line\ndescription"})));
	
	
			LscModifications lm = new LscModifications(LscModificationType.UPDATE_OBJECT);
			lm.setMainIdentifer("dc=lsc-project,dc=org");
			lm.setLscAttributeModifications(mi);
	
			ILoggingEvent loggingEvent = makeLoggingEvent(lm.toString(), lm);
	
			LdifLayout layout = new LdifLayout();
			layout.setContext(lc);
			layout.setPattern("%m%n");
			layout.start();
	
			
			assertEquals("# Wed Dec 12 16:25:01 CET 2012\n" + 
							"dn: dc=lsc-project,dc=org\n" +
							"changetype: modify\n" +
							"replace: cn\n" +
							"cn: new_name\n" +
							"-\n" +
							"delete: uid\n" +
							"uid: old_id\n" +
							"-\n" +
							"replace: sn\n" +
							"sn:: w4AgbMOgIGJhcw==\n" +
							"-\n" +
							"add: description\n" +
							"description:: TXVsdGktbGluZQpkZXNjcmlwdGlvbg==\n" +
							"-\n" +
							"\n",
							layout.doLayout(loggingEvent));
		}
	}

	/**
	 * Launch a remove entry layout test.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void testRemove() throws IOException {
		try (MockedConstruction<Date> dateCtr = Mockito.mockConstruction(Date.class,(mock,context)-> {
			when(mock.toString()).thenReturn("Wed Dec 12 16:25:01 CET 2012");})) {
			LscModifications lm = new LscModifications(LscModificationType.DELETE_OBJECT);
			lm.setMainIdentifer("uid=a,dc=lsc-project,dc=org");
	
			ILoggingEvent loggingEvent = makeLoggingEvent(lm.toString(), lm);
	
			LdifLayout layout = new LdifLayout();
			layout.setContext(lc);
			layout.setPattern("%m%n");
			layout.start();
	
			assertEquals(
				"# Wed Dec 12 16:25:01 CET 2012\n"
				+ "dn: uid=a,dc=lsc-project,dc=org\n"
				+ "changetype: delete\n\n",
							layout.doLayout(loggingEvent));
		}
	}
}
