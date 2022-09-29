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
package org.lsc.beans.syncoptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.lsc.configuration.DatasetType;
import org.lsc.configuration.PolicyType;
import org.lsc.configuration.PropertiesBasedSyncOptionsType;
import org.lsc.configuration.TaskType;
import org.lsc.configuration.ValuesType;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.ScriptingEvaluator;

import mockit.Injectable;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class PropertiesBasedSyncOptionsTest {

	PropertiesBasedSyncOptionsType conf ;
	DatasetType pbsoNonExistingAttr;
	DatasetType pbsoExistingAttr;
	@Injectable @NonStrict TaskType task;
	@Injectable @NonStrict org.lsc.Task taskExec;

	@Before
	public void setup() {
		pbsoNonExistingAttr = new DatasetType();
		pbsoExistingAttr = new DatasetType();
		conf = new PropertiesBasedSyncOptionsType();
	}
	
	@Test
	public final void testDelimiters() {
		DatasetType delimitedAttr = new DatasetType();
		ValuesType vt = new ValuesType();
		vt.getString().addAll(Arrays.asList(new String[] {"\"a\"", "\"b\""}));
		delimitedAttr.setForceValues(vt);
		delimitedAttr.setName("DelimitedAttribute");

		conf.getDataset().add(delimitedAttr);
		conf.setDefaultPolicy(PolicyType.FORCE);

		new NonStrictExpectations() {
			{
				task.getPropertiesBasedSyncOptions(); result = conf;
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

	@Test
	public final void test1() {
		pbsoNonExistingAttr.setName("nonExistantAttrName");
		pbsoNonExistingAttr.setPolicy(PolicyType.KEEP);
		conf.getDataset().add(pbsoNonExistingAttr); 

		pbsoExistingAttr.setPolicy(PolicyType.KEEP);
		pbsoExistingAttr.setName("sampleAttribute");
		conf.getDataset().add(pbsoExistingAttr); 

		new NonStrictExpectations() {
			{
				task.getPropertiesBasedSyncOptions(); result = conf;
			}
		};

		ISyncOptions iso = new PropertiesBasedSyncOptions();
		assertNotNull(iso);
		iso.initialize(task);
		assertEquals(iso.getStatus("objectId", "sampleAttribute"), PolicyType.KEEP);
		assertEquals(PolicyType.KEEP, iso.getStatus(null, "nonExistantAttrName"));
	}

	@Test
	public final void testJS() throws LscServiceException {
		DatasetType jsAttr = new DatasetType();
		ValuesType jsAttrValues = new ValuesType();
		jsAttrValues.getString().addAll(Arrays.asList(new String[] {"\"uid=00000001\" + \",ou=People,dc=lsc-project,dc=org\""}));
		jsAttr.setDefaultValues(jsAttrValues);
		jsAttr.setName("JsAttribute");
		conf.getDataset().add(jsAttr);

		new NonStrictExpectations() {
			{
				task.getPropertiesBasedSyncOptions(); result = conf;
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
		List<Object> defaultValuesObj = ScriptingEvaluator.evalToObjectList(taskExec, defaultValue, null);
		assertEquals(1, defaultValuesObj.size());
		assertEquals("uid=00000001,ou=People,dc=lsc-project,dc=org", defaultValuesObj.get(0));
	}
}
