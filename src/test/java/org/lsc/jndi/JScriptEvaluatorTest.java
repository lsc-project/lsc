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
package org.lsc.jndi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.BasicAttribute;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Before;
import org.junit.Test;
import org.lsc.Task;
import org.lsc.beans.IBean;
import org.lsc.beans.SimpleBean;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.ScriptingEvaluator;
import org.mozilla.javascript.EcmaError;

import com.google.common.collect.Sets;

/**
 * Test different use cases of this JScript evaluator
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke <jonathan@phillipoux.net>
 */
public class JScriptEvaluatorTest {

	@Mocked Task task;
	
	@Before
	public void before() {
		LscConfiguration.reset();
	}
	
	@Test
	public void testOk() throws LscServiceException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));
		assertEquals("b", ScriptingEvaluator.evalToString(task, "srcAttr.get()", table));
	}

	@Test(expected=LscServiceException.class)
	public void testNk() throws EcmaError, LscServiceException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));

		ScriptingEvaluator.evalToString(task, "src.get()", table);
	}

	@Test
	public void testOk2() throws LscServiceException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("sn", new BasicAttribute("sn", "Doe"));
		table.put("givenName", new BasicAttribute("givenName", "John"));
		assertEquals(ScriptingEvaluator.evalToString(task, "givenName.get() + ' ' + sn.get()", table), "John Doe");
	}

	@Test
	public void testList() throws LscServiceException {
		Map<String, Object> table = new HashMap<String, Object>();
		IBean bean = (IBean) new SimpleBean();
		bean.setDataset("sn", Sets.newHashSet((Object)"Doe"));
        bean.setDataset("givenName", Sets.newHashSet((Object)"John"));
        bean.setDataset("cn", Sets.newHashSet((Object)"John Doe", (Object)"DOE John"));

		table.put("srcBean", bean);

		assertEquals("John Doe", ScriptingEvaluator.evalToString(task, "srcBean.getDatasetFirstValueById('givenName') + ' ' + srcBean.getDatasetFirstValueById('sn')", table));

		List<Object> res = ScriptingEvaluator.evalToObjectList(task, "srcBean.getDatasetById('givenName') + ' ' + srcBean.getDatasetById('sn')", table);
		assertNotNull(res);
		assertEquals("[John] [Doe]", res.get(0));

		assertEquals("John Doe", ScriptingEvaluator.evalToString(task, "srcBean.getDatasetById('givenName').toArray()[0] + ' ' + srcBean.getDatasetById('sn').toArray()[0]", table));

		res = ScriptingEvaluator.evalToObjectList(task, "srcBean.getDatasetValuesById('cn')", table);
		assertNotNull(res);
		assertEquals(2, res.size());
		assertTrue(res.contains("John Doe"));
		assertTrue(res.contains("DOE John"));

		res = ScriptingEvaluator.evalToObjectList(task, "srcBean.getDatasetValuesById('nonexistent')", table);
		assertNotNull(res);
		assertEquals(0, res.size());
		
		res = ScriptingEvaluator.evalToObjectList(task, "srcBean.getDatasetFirstValueById('nonexistent')", table);
		assertNotNull(res);
		assertEquals(0, res.size());

        res = ScriptingEvaluator.evalToObjectList(task, "var arr = new Array(); \n arr[0]='a'; \n  arr[1]='b'; arr", table);
        assertNotNull(res);
        assertEquals(2, res.size());

	}

	@Test
	public void testOkLdap() throws LscServiceException {
		Map<String, Object> table = new HashMap<String, Object>();

		final TaskType taskConf = LscConfiguration.getTask("ldap2ldapTestTask");
		assertNotNull(taskConf);
		
		new NonStrictExpectations() {
			{
				task.getDestinationService(); result = new SimpleJndiDstService(taskConf);
			}
		};
		
		List<Object> res = ScriptingEvaluator.evalToObjectList(task, "ldap.or(ldap.attribute('ou=People,dc=lsc-project,dc=org','ou'), ldap.fsup('ou=People,dc=lsc-project,dc=org','dc=*'))", table);
		assertEquals("[People, dc=lsc-project,dc=org]", res.toString());
	}
}
