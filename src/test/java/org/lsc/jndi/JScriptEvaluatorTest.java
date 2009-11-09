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
package org.lsc.jndi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import junit.framework.TestCase;

import org.junit.Test;
import org.lsc.beans.AbstractBean;
import org.lsc.beans.personBean;
import org.lsc.utils.JScriptEvaluator;
import org.mozilla.javascript.EcmaError;

/**
 * Test different use cases of this JScript evaluator
 * 
 * @author Sebastien Bahloul <seb@lsc-project.org>
 * @author Jonathan Clarke <jonathan@phillipoux.net>
 */
public class JScriptEvaluatorTest extends TestCase {

	public void testOk() {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));
		assertEquals("b", JScriptEvaluator.evalToString("srcAttr.get()", table));
	}

	@Test(expected=EcmaError.class)
	public void testNk() throws EcmaError {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));

		assertNull(JScriptEvaluator.evalToString("src.get()", table));
		assertNull(JScriptEvaluator.evalToStringList("src.get()", table));
	}

	public void testOk2() {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("sn", new BasicAttribute("sn", "Doe"));
		table.put("givenName", new BasicAttribute("givenName", "John"));
		assertEquals(JScriptEvaluator.evalToString("givenName.get() + ' ' + sn.get()", table), "John Doe");
	}

	public void testList() {
		Map<String, Object> table = new HashMap<String, Object>();
		Attribute sn = new BasicAttribute("sn", "Doe");
		Attribute givenName = new BasicAttribute("givenName", "John");
		Attribute cn = new BasicAttribute("cn");
		cn.add("John Doe");
		cn.add("DOE John");
		AbstractBean bean = (AbstractBean) new personBean();
		bean.setAttribute(sn);
		bean.setAttribute(givenName);
		bean.setAttribute(cn);

		table.put("srcBean", bean);

		assertEquals("John Doe", JScriptEvaluator.evalToString("srcBean.getAttributeById('givenName').get() + ' ' + srcBean.getAttributeById('sn').get()", table));
		assertEquals("John Doe", JScriptEvaluator.evalToString("srcBean.getAttributeFirstValueById('givenName') + ' ' + srcBean.getAttributeValueById('sn')", table));

		List<String> res = JScriptEvaluator.evalToStringList("srcBean.getAttributeById('givenName').get() + ' ' + srcBean.getAttributeById('sn').get()", table);
		assertNotNull(res);
		assertEquals("John Doe", res.get(0));

		res = JScriptEvaluator.evalToStringList("srcBean.getAttributeValuesById('cn')", table);
		assertNotNull(res);
		assertEquals(2, res.size());
		assertTrue(res.contains("John Doe"));
		assertTrue(res.contains("DOE John"));

		res = JScriptEvaluator.evalToStringList("srcBean.getAttributeValuesById('nonexistent')", table);
		assertNotNull(res);
		assertEquals(0, res.size());
		
		res = JScriptEvaluator.evalToStringList("srcBean.getAttributeValueById('nonexistent')", table);
		assertNotNull(res);
		assertEquals(0, res.size());

	}

	public void testOkLdap() {
		Map<String, Object> table = new HashMap<String, Object>();

		List<String> res = JScriptEvaluator.evalToStringList("ldap.or(ldap.attribute('ou=People,dc=lsc-project,dc=org','ou'), ldap.fsup('ou=People,dc=lsc-project,dc=org','dc=*'))", table);
		assertEquals("[People, dc=lsc-project,dc=org]", res.toString());
	}
}
