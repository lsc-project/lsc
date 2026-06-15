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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Test conversion date utils.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class DateUtilsTest {

	/**
	 * Launch the parse test.
	 * 
	 * @throws ParseException Thrown if the parsing operation failed
	 */
	@org.junit.jupiter.api.Test
	public final void testParse() throws ParseException {
		// "20070622192826.0Z" is a UTC date string
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		gc.set(2007, 5, 22, 19, 28, 26);
		gc.set(Calendar.MILLISECOND, 0);
		assertEquals(gc.getTime(), DateUtils.parse("20070622192826.0Z"));
		assertEquals(gc.getTime(), DateUtils.parse("20070622192826Z"));
	}

	/**
	 * Launch the format test.
	 */
	@Test
	public final void testFormat() {
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		gc.set(2007, 5, 22, 19, 28, 26);
		gc.set(Calendar.MILLISECOND, 0);
		assertEquals("20070622192826.0Z", DateUtils.format(gc.getTime()));
		assertEquals("20070622192826Z", DateUtils.simpleFormat(gc.getTime()));
	}

	/**
	 * Launch the format test.
	 */
	@Test
	public final void testError() throws ParseException {
		assertThrows(ParseException.class, () -> DateUtils.parse("0Z"));
	}

	@Test
	public final void testParseUsesUtc() throws ParseException {
		GregorianCalendar expected = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		expected.set(2007, 5, 22, 19, 28, 26);
		expected.set(Calendar.MILLISECOND, 0);
		assertEquals(expected.getTime(), DateUtils.parse("20070622192826Z"));
	}

	@Test
	public final void testFormatUsesUtc() {
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		gc.set(2007, 5, 22, 19, 28, 26);
		gc.set(Calendar.MILLISECOND, 0);
		assertEquals("20070622192826.0Z", DateUtils.format(gc.getTime()));
	}

	@Test
	public final void testThreadSafety() throws Exception {
		int threadCount = 10;
		int iterations = 1000;
		java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
		java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger(0);

		for (int t = 0; t < threadCount; t++) {
			final int threadId = t;
			new Thread(() -> {
				try {
					GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
					gc.set(2007, 5, 22, 19, 28, 26);
					gc.set(Calendar.MILLISECOND, 0);

					for (int i = 0; i < iterations; i++) {
						String formatted = DateUtils.format(gc.getTime());
						if (!"20070622192826.0Z".equals(formatted)) {
							errors.incrementAndGet();
						}
						Date parsed = DateUtils.parse("20070622192826.0Z");
						if (!gc.getTime().equals(parsed)) {
							errors.incrementAndGet();
						}
					}
				} catch (Exception e) {
					errors.incrementAndGet();
				} finally {
					latch.countDown();
				}
			}).start();
		}

		latch.await();
		assertEquals(0, errors.get(), "Thread safety violations detected");
	}
}
