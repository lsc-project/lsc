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
package org.lsc.utils.directory;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test AD specific function library.
 * 
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public class ADTest {

	// various representations of the reference date 01/10/08 8:12:34 UTC
	private static Calendar refTimeCalendar;
	private static final String refTimeADString = "128673223540000000";
	private static final long refTimeADLong = 128673223540000000L;
	private static final String refTimeUnixString = "1222848754";
	private static final int refTimeUnixInt= 1222848754;

	@Before
	public void setUp() {
		refTimeCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		refTimeCalendar.clear();
		refTimeCalendar.set(2008, 10-1, 01, 8, 12, 34);
	}
	
	/**
	 * Test functions to manipulate AD's userAccountControl attribute in bit-field format.
	 */
	@Test
	public final void testUserAccountControl() {
		// initialize userAccountControl to a normal account
		int uACValue = AD.userAccountControlSet(0, new String[]{AD.UAC_NORMAL_ACCOUNT.toString()});
		assertEquals(512, uACValue);

		// check it's enabled two ways
		assertFalse(AD.userAccountControlCheck(uACValue, AD.UAC_ACCOUNTDISABLE.toString()));

		// disable the account and set password expired
		uACValue = AD.userAccountControlSet(uACValue, new String[]{AD.UAC_ACCOUNTDISABLE.toString(), AD.UAC_PASSWORD_EXPIRED.toString()});
		assertEquals(8389122, uACValue);

		// unset password expired
		uACValue = AD.userAccountControlSet(uACValue, new String[]{AD.UAC_UNSET_PASSWORD_EXPIRED.toString()});
		assertEquals(514, uACValue);

		// check it's disabled
		assertTrue(AD.userAccountControlCheck(uACValue, AD.UAC_ACCOUNTDISABLE.toString()));

		// toggle it back to enabled
		uACValue = AD.userAccountControlToggle(uACValue, AD.UAC_ACCOUNTDISABLE.toString());
		assertEquals(512, uACValue);

		// and toggle back to disabled
		uACValue = AD.userAccountControlToggle(uACValue, AD.UAC_ACCOUNTDISABLE.toString());
		assertEquals(514, uACValue);
	}

	/**
	 * Test number of weeks calculation for lastLogon or lastLogonTimestamp attribute in AD.
	 *
	 * With this value:
	 * lastLogonTimestamp: 128673223549843750
	 * The last logon was recorded at 01/10/08 8:12:34 UTC.
	 */
	@Test
	public final void testNumberWeeksLastLogon() {
		// get result from tested class
		int numWeeksFromLastLogon = AD.getNumberOfWeeksSinceLastLogon(refTimeADString);

		// calculate result from known date to now
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long secondsSinceReference = (now.getTimeInMillis() - refTimeCalendar.getTimeInMillis()) / 1000;
		int numWeeksFromReference = (int) (secondsSinceReference / (60 * 60 * 24 * 7));

		assertTrue(numWeeksFromReference <= numWeeksFromLastLogon + 1 && numWeeksFromReference >= numWeeksFromLastLogon - 1);
	}
	
	/**
	 * Test for the {@link AD#aDTimeToUnixTimestamp(String)} method.
	 */
	@Test
	public final void testADTimeToUnixTimestampWithString() {
		Calendar testedTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		testedTime.clear();
		testedTime.setTimeInMillis(AD.aDTimeToUnixTimestamp(refTimeADString) * (long) 1000);
		
		assertEquals(refTimeCalendar, testedTime);
	}
	
	/**
	 * Test for the {@link AD#aDTimeToUnixTimestamp(long)} method.
	 */
	@Test
	public final void testADTimeToUnixTimestampWithLong() {
		Calendar testedTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		testedTime.clear();
		testedTime.setTimeInMillis(AD.aDTimeToUnixTimestamp(refTimeADLong) * (long) 1000);
		
		assertEquals(refTimeCalendar, testedTime);
	}

	/**
	 * Test for the {@link AD#unixTimestampToADTime(String)} method.
	 */
	@Test
	public final void testUnixTimestampToADTimeWithString() {
		assertEquals(refTimeADLong, AD.unixTimestampToADTime(refTimeUnixString));
	}
	
	/**
	 * Test for the {@link AD#unixTimestampToADTime(int)} method.
	 */
	@Test
	public final void testUnixTimestampToADTimeWithInt() {
		assertEquals(refTimeADLong, AD.unixTimestampToADTime(refTimeUnixInt));
	}
	
}
