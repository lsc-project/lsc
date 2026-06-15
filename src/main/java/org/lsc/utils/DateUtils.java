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

 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of the LSC Project nor the names of its
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Manage LDAP date format.
 * 
 * <p>For example, use :</p>
 * <ul>
 * <li>DateUtils.parse("20070622192826Z");</li>
 * <li>DateUtils.format(new Date());</li>
 * </ul>
 */
public final class DateUtils {

	private DateUtils() {}

	public static final String LDAP_DATE_INTERNAL_STORAGE_FORMAT =
					"yyyyMMddHHmmss.S'Z'";

	public static final String LDAP_DATE_SIMPLIFIED_STORAGE_FORMAT =
					"yyyyMMddHHmmss'Z'";

	private static final DateTimeFormatter FORMATTER =
					DateTimeFormatter.ofPattern(LDAP_DATE_INTERNAL_STORAGE_FORMAT)
									.withZone(ZoneOffset.UTC);

	private static final DateTimeFormatter SIMPLIFIED_FORMATTER =
					DateTimeFormatter.ofPattern(LDAP_DATE_SIMPLIFIED_STORAGE_FORMAT)
									.withZone(ZoneOffset.UTC);

	/**
	 * Return a date object corresponding to the LDAP date string.
	 *
	 * @param date the date to parse
	 * @return the corresponding Java Date object
	 * @throws ParseException thrown if an error occurs in date parsing
	 */
	public static Date parse(final String date) throws ParseException {
		try {
			return Date.from(FORMATTER.parse(date, Instant::from));
		} catch (DateTimeParseException pe) {
			try {
				return Date.from(SIMPLIFIED_FORMATTER.parse(date, Instant::from));
			} catch (DateTimeParseException pe2) {
				throw new ParseException(pe.getMessage(), pe.getErrorIndex());
			}
		}
	}

	/**
	 * Generate a date string in the standard LDAP format: yyyyMMddHHmmss.S'Z'.
	 *
	 * @param date date to extract
	 * @return generated date
	 */
	public static String format(final Date date) {
		return FORMATTER.format(date.toInstant());
	}

	/**
	 * Generate a date string in the simplified LDAP format: yyyyMMddHHmmss'Z'.
	 *
	 * @param date date to extract
	 * @return generated date
	 */
	public static String simpleFormat(final Date date) {
		return SIMPLIFIED_FORMATTER.format(date.toInstant());
	}
}
