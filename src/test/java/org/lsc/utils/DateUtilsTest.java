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
package org.lsc.utils;

import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test conversion date utils.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class DateUtilsTest {

	/**
	 * Launch the parse test.
	 * @throws ParseException Thrown if the parsing operation failed
	 */
	@Test
	public final void testParse() throws ParseException {
		// Please take care : use 5 instead of 6 because month is a 0 starting value
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getDefault());
		gc.set(2007, 5, 22, 19, 28, 26);
		// Then remove milliseconds
		long time = gc.getTime().getTime();
		time = time - time % 1000;
		gc.setTimeInMillis(time);
		assertEquals(gc.getTime(), DateUtils.parse("20070622192826.0Z"));
		assertEquals(gc.getTime(), DateUtils.parse("20070622192826Z"));
	}

	/**
	 * Launch the format test.
	 */
	@Test
	public final void testFormat() {
		// Please take care : use 5 instead of 6 because month is a 0 starting value
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getDefault());
		gc.set(2007, 5, 22, 19, 28, 26);
		long time = gc.getTime().getTime();
		time = time - time % 1000;
		gc.setTimeInMillis(time);
		assertEquals("20070622192826.0Z", DateUtils.format(gc.getTime()));
		assertEquals("20070622192826Z", DateUtils.simpleFormat(gc.getTime()));
	}

	/**
	 * Launch the format test.
	 */
	@Test(expected=ParseException.class)
	public final void testError() throws ParseException {
		DateUtils.parse("0Z");
	}
}
