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
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.beans.syncoptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import mockit.Injectable;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.junit.Test;
import org.lsc.configuration.objects.Task;
import org.lsc.configuration.objects.syncoptions.PBSOAttribute;
import org.lsc.utils.ScriptingEvaluator;

public class PropertiesBasedSyncOptionsTest {

	@Injectable @NonStrict org.lsc.configuration.objects.syncoptions.PropertiesBasedSyncOptions conf ;
	@Injectable @NonStrict PBSOAttribute pbsoNonExistingAttr;
	@Injectable @NonStrict PBSOAttribute pbsoExistingAttr;
	@Injectable @NonStrict Task task;
	@Injectable @NonStrict org.lsc.Task taskExec;
	
	@Test
	public final void test1() {
		new NonStrictExpectations() {
			{
				pbsoNonExistingAttr.getPolicy(); result = ISyncOptions.STATUS_TYPE.KEEP;
				conf.getAttribute("nonExistantAttrName"); result = pbsoNonExistingAttr; 
				pbsoNonExistingAttr.getPolicy(); result = ISyncOptions.STATUS_TYPE.KEEP;
				conf.getAttribute("sampleAttribute"); result = pbsoExistingAttr; 
				task.getSyncOptions(); result = conf;
			}
		};
		ISyncOptions iso = new PropertiesBasedSyncOptions();
		assertNotNull(iso);
		iso.initialize(task);
		assertNotSame(iso.getStatus("objectId", "sampleAttribute"), ISyncOptions.STATUS_TYPE.UNKNOWN);
		assertEquals(ISyncOptions.STATUS_TYPE.KEEP, iso.getStatus(null, "nonExistantAttrName"));
	}

	@Test
	public final void testJS() {
		new NonStrictExpectations() {
			@Injectable @NonStrict PBSOAttribute jsAttr;
			{
				jsAttr.getDefaultValues(); result = Arrays.asList(new String[] {"\"uid=00000001\" + \",ou=People,dc=lsc-project,dc=org\""});
				conf.getAttribute("JsAttribute"); result = jsAttr;
				task.getSyncOptions(); result = conf;
			}
		};
		ISyncOptions iso = new PropertiesBasedSyncOptions();
		assertNotNull(iso);
		iso.initialize(task);

		// get JavaScript enable default value
		List<String> defaultValues = iso.getDefaultValues(null, "JsAttribute");
		assertNotNull(defaultValues);
		Iterator<String> it = defaultValues.iterator();
		assertTrue(it.hasNext());
		String defaultValue = it.next();
		assertEquals("\"uid=00000001\" + \",ou=People,dc=lsc-project,dc=org\"", defaultValue);

		// evaluate JavaScript
		defaultValues = ScriptingEvaluator.evalToStringList(taskExec, defaultValue, null);
		assertEquals(1, defaultValues.size());
		assertEquals("uid=00000001,ou=People,dc=lsc-project,dc=org", defaultValues.get(0));
	}

	@Test
	public final void testDelimiters() {
		new NonStrictExpectations() {
			@Injectable @NonStrict PBSOAttribute delimitedAttr;
			{
				delimitedAttr.getForceValues(); result = Arrays.asList(new String[] {"\"a\"", "\"b\""});
				conf.getAttribute("DelimitedAttribute"); result = delimitedAttr;
				task.getSyncOptions(); result = conf;
			}
		};
		ISyncOptions iso = new PropertiesBasedSyncOptions();
		assertNotNull(iso);
		iso.initialize(task);

		List<String> forceValues = iso.getForceValues(null, "DelimitedAttribute");

		assertEquals(2, forceValues.size());
		assertEquals("\"a\"", forceValues.get(0));
		assertEquals("\"b\"", forceValues.get(1));
	}
}
