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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.directory.BasicAttribute;

import org.lsc.utils.JScriptEvaluator;

import junit.framework.TestCase;

/**
 * Test different use cases of this JScript evaluator
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
public class JScriptEvaluatorTest extends TestCase {

	public void testOk() throws IllegalAccessException, InstantiationException, InvocationTargetException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));
		assertEquals(JScriptEvaluator.evalToString("srcAttr.get()", table), "b");
	}

	public void testNk() throws IllegalAccessException, InstantiationException, InvocationTargetException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("srcAttr", new BasicAttribute("a", "b"));
		try {
			assertEquals(JScriptEvaluator.evalToString("src.get()", table), "b");
			assertTrue(false);
		} catch(org.mozilla.javascript.EcmaError e) {
			assertTrue(true);
		}
	}

	public void testOk2() throws IllegalAccessException, InstantiationException, InvocationTargetException {
		Map<String, Object> table = new HashMap<String, Object>();
		table.put("sn", new BasicAttribute("sn", "Doe"));
		table.put("givenName", new BasicAttribute("givenName", "John"));
		assertEquals(JScriptEvaluator.evalToString("givenName.get() + ' ' + sn.get()", table), "John Doe");
	}

//	public void testOk3() throws IllegalAccessException, InstantiationException, InvocationTargetException {
//		Map<String, Object> table = new HashMap<String, Object>();
//		Attribute sn = new BasicAttribute("sn", "Doe");
//		Attribute givenName = new BasicAttribute("givenName", "John");
//		inetOrgPersonBean srcIopb = new inetOrgPersonBean();
//		srcIopb.setAttribute(sn);
//		srcIopb.setAttribute(givenName);
//		table.put("srcBean", srcIopb);
//		assertEquals(JScriptEvaluator.eval("srcBean.getAttributeById('givenName').get() + ' ' + srcBean.getAttributeById('sn').get()", table), "John Doe");
//	}

//	public void testOkLdap() throws IllegalAccessException, InstantiationException, InvocationTargetException {
//		Map<String, Object> table = new HashMap<String, Object>();
//		assertEquals(JScriptEvaluator.eval("ldap.or(ldap.attribute('ou=People,dc=lsc-project,dc=org','ou'), ldap.fsup('ou=People,dc=lsc-project,dc=org','dc=*'))", table), "[People,lsc-project]");
//	}
}
