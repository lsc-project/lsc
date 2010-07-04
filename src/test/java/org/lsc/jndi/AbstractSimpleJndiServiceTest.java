/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2010, LSC Project 
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
 *               (c) 2008 - 2010 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jndi;

import static org.junit.Assert.*;

import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lsc.Configuration;
import org.lsc.LscAttributes;

/**
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 *
 */
public class AbstractSimpleJndiServiceTest {

	private Properties props;
	private AbstractSimpleJndiService testService;
	private LscAttributes pivotAttrs;
	private SearchResult res = null;
	
	@Before
	public void setUp() { 
		// set up an instance of AbstractSimpleJndiService
		
		props = Configuration.getAsProperties(Configuration.LSC_TASKS_PREFIX + ".ldap2ldapTestTask.srcService");

		pivotAttrs = new LscAttributes();
		pivotAttrs.put("cn", "CN0001");
		pivotAttrs.put("sn", "SN0001");		
	}
	
	@After
	public void check() throws NamingException {
		assertNotNull(res);
		assertNotNull(res.getAttributes());
		assertNotNull(res.getAttributes().get("cn"));
		assertNotNull(res.getAttributes().get("sn"));

		assertEquals(1, res.getAttributes().get("sn").size());
		assertEquals(1, res.getAttributes().get("cn").size());

		assertEquals("CN0001", res.getAttributes().get("cn").get());
		assertEquals("SN0001", res.getAttributes().get("sn").get());
	}
	
	/**
	 * Test method for {@link org.lsc.jndi.AbstractSimpleJndiService#get(java.lang.String, org.lsc.LscAttributes)}.
	 * @throws NamingException Error
	 */
	@Test
	public void testGetWithMultiplePivotAttributes() throws NamingException {
		props.put("filterId", "(&(cn={cn})(sn={sn}))");

		testService = new SimpleJndiSrcService(props, "org.lsc.beans.SimpleBean");
		res = testService.get("Random string that shouldn't matter", pivotAttrs);
	}
	
}
