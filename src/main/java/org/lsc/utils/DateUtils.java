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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Manage LDAP date format.
 * 
 * <p/>For example, use :
 * <ul>
 * <li>DateUtils.parse("20070622192826Z");</li>
 * <li>DateUtils.format(new Date());</li>
 * </ul>
 */
public final class DateUtils {

	// Utility class
	private DateUtils() {}
	
	/**
	 * This is the standard LDAP date format : yyyyMMddHHmmss.S'Z'.
	 */
	public static final String LDAP_DATE_INTERNAL_STORAGE_FORMAT =
					"yyyyMMddHHmmss.S'Z'";

	/**
	 * This is the simplified LDAP date format : yyyyMMddHHmmss'Z'.
	 */
	public static final String LDAP_DATE_SIMPLIFIED_STORAGE_FORMAT =
					"yyyyMMddHHmmss'Z'";

	/**
	 * Internal transformation object.
	 * TODO fix this if there is a performance problem, I did a small
	 * cleanup and added synchronization as a DateFormat is not
	 * threadsafe
	 */
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
					LDAP_DATE_INTERNAL_STORAGE_FORMAT);

	/**
	 * Internal transformation object.
	 * TODO fix this if there is a performance problem, I did a small
	 * cleanup and added synchronization as a DateFormat is not
	 * threadsafe
	 */
	private static final SimpleDateFormat SIMPLIFIED_FORMATTER =
					new SimpleDateFormat(LDAP_DATE_SIMPLIFIED_STORAGE_FORMAT);

	/** The UTC time zone. */
	private static final TimeZone UTC_TIME_ZONE = TimeZone.getDefault();//getTimeZone("UTC");

	static {
		FORMATTER.setLenient(false);
		FORMATTER.setTimeZone(UTC_TIME_ZONE);
	}

	/**
	 * Return a date object corresponding to the LDAP date string.
	 *
	 * @param date the date to parse
	 * @return the corresponding Java Date object
	 * @throws ParseException
	 *                 thrown if an error occurs in date parsing
	 */
	public static Date parse(final String date) throws ParseException {
		synchronized (FORMATTER) {
			try {
				return FORMATTER.parse(date);
			} catch (ParseException pe) {
				try {
					return SIMPLIFIED_FORMATTER.parse(date);
				} catch (ParseException pe2) {
					throw pe;
				}
			}
		}
	}

	/**
	 * Generate a date string - synchronized call to internal formatter
	 * object to support multi-threaded calls.
	 *
	 * @param date date to extract
	 * @return generated date
	 */
	public static String format(final Date date) {
		synchronized (FORMATTER) {
			return FORMATTER.format(date);
		}
	}

	/**
	 * Generate a date string - synchronized call to internal formatter
	 * object to support multi-threaded calls.
	 *
	 * This uses the simplified format: yyyyMMddHHmmss'Z'
	 *
	 * @param date date to extract
	 * @return generated date
	 */
	public static String simpleFormat(final Date date) {
		synchronized (SIMPLIFIED_FORMATTER) {
			return SIMPLIFIED_FORMATTER.format(date);
		}
	}
}
