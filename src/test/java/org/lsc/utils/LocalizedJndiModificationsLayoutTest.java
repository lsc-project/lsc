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
package org.lsc.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import junit.framework.TestCase;

import org.slf4j.LoggerFactory;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

/**
 * Provide a complete test.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LocalizedJndiModificationsLayoutTest extends TestCase
{

	/**
	 * Launch a add entry layout test.
	 * 
	 * @throws IOException
	 */
	public final void testAdd() throws IOException
	{
		List<ModificationItem> mi = new ArrayList<ModificationItem>();
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("cn", "name")));
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("sn", "<non safe string>")));
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("givenName", "Sébastien")));
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("description", "")));

		JndiModifications jm = new JndiModifications(JndiModificationType.ADD_ENTRY);
		jm.setDistinguishName("givenName=Sébastien");
		jm.setModificationItems(mi);

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", LoggerFactory.getLogger(""), Level.INFO, jm, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("dn:: Z2l2ZW5OYW1lPVPDqWJhc3RpZW4sZGM9bHNjLXByb2plY3QsZGM9b3Jn\nchangetype: add\ncn: name\nsn:: PG5vbiBzYWZlIHN0cmluZz4=\ngivenName:: U8OpYmFzdGllbg==\ndescription: \n\n", layout.format(loggingEvent));
		
		jm.setDistinguishName(null);
		assertEquals("dn: dc=lsc-project,dc=org\nchangetype: add\ncn: name\nsn:: PG5vbiBzYWZlIHN0cmluZz4=\ngivenName:: U8OpYmFzdGllbg==\ndescription: \n\n", layout.format(loggingEvent));
	}

	/**
	 * Launch a modify entry layout test.
	 * 
	 * @throws IOException
	 */
	public final void testModify() throws IOException
	{
		List<ModificationItem> mi = new ArrayList<ModificationItem>();
		mi.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("cn", "new_name")));
		mi.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("uid", "old_id")));
		mi.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("sn", "À là bas")));
		mi.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("description", "Multi-line\ndescription")));


		JndiModifications jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
		jm.setDistinguishName("");
		jm.setModificationItems(mi);

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", LoggerFactory.getLogger(""), Level.INFO, jm, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("dn: dc=lsc-project,dc=org\n" +
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
				"\n",
				layout.format(loggingEvent));
	}

	/**
	 * Launch a remove entry layout test.
	 * 
	 * @throws IOException
	 */
	public final void testRemove() throws IOException
	{
		JndiModifications jm = new JndiModifications(JndiModificationType.DELETE_ENTRY);
		jm.setDistinguishName("uid=a");

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", LoggerFactory.getLogger(""), Level.INFO, jm, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("dn: uid=a,dc=lsc-project,dc=org\nchangetype: delete\n\n", layout.format(loggingEvent));
	}

	/**
	 * Launch a neutral layout test.
	 * 
	 * @throws IOException
	 */
	public final void testNeutral() throws IOException
	{
		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", LoggerFactory.getLogger(""), Level.INFO, "a simple string", null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m");
		I18n.setLocale(Locale.US);
		assertEquals("a simple string", layout.format(loggingEvent));
	}
	
	/**
	 * Launch a null layout test.
	 * 
	 * @throws IOException
	 */
	public final void testNull() throws IOException
	{
		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", LoggerFactory.getLogger(""), Level.INFO, null, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m");
		I18n.setLocale(Locale.US);
		assertEquals("", layout.format(loggingEvent));
	}

}
