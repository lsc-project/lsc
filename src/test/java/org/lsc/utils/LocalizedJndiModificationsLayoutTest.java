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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.lsc.utils.I18n;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.lsc.utils.LocalizedJndiModificationsLayout;

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

		JndiModifications jm = new JndiModifications(JndiModificationType.ADD_ENTRY);
		jm.setDistinguishName("");
		jm.setModificationItems(mi);

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", Logger.getLogger(""), Level.INFO, jm, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("dn: dc=lsc-project,dc=org\nchangetype: add\ncn: name\nsn:: PG5vbiBzYWZlIHN0cmluZz4=\ngivenName:: U8OpYmFzdGllbg==\n\n", layout.format(loggingEvent));
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
		mi.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("sn", "Nom accentué")));

		JndiModifications jm = new JndiModifications(JndiModificationType.MODIFY_ENTRY);
		jm.setDistinguishName("");
		jm.setModificationItems(mi);

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", Logger.getLogger(""), Level.INFO, jm, null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("dn: dc=lsc-project,dc=org\nchangetype: modify\nreplace: cn\ncn: new_name\n-\ndelete: uid\nuid: old_id\n-\nreplace: sn\nsn:: Tm9tIGFjY2VudHXDqQ==\n\n", layout.format(loggingEvent));
	}

	/**
	 * Launch a remove entry layout test.
	 * 
	 * @throws IOException
	 */
	public final void testRemove() throws IOException
	{
		// List<ModificationItem> mi = new ArrayList<ModificationItem>();
		JndiModifications jm = new JndiModifications(JndiModificationType.DELETE_ENTRY);
		jm.setDistinguishName("uid=a");

		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", Logger.getLogger(""), Level.INFO, jm, null);

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
		LoggingEvent loggingEvent = new LoggingEvent("org.lsc", Logger.getLogger(""), Level.INFO, "a simple string", null);

		LocalizedJndiModificationsLayout layout = new LocalizedJndiModificationsLayout();
		layout.setConversionPattern("%m%n");
		I18n.setLocale(Locale.US);
		assertEquals("a simple string\n", layout.format(loggingEvent));
	}
}
