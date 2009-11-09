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
package org.lsc.utils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage all common string manipulation for french
 *
 * @author Sebastien Bahloul <seb@lsc-project.org>;
 */
public final class FrenchFilters {

	private static Logger LOGGER = LoggerFactory.getLogger(FrenchFilters.class);

	/** The regexep for authorized characters */
	private static final String REGEXP_CHARACTERS =
					"[\\p{Alpha}\\s'\"áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;" + "&egrave;êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç-]+";

	/** Array of accents and cedillas */
	private static final String[] REGEXP_ACCENTS_CEDILLAS = {
		"À", "á", "Á",
		"&agrave;",
		"&agrave;", "â",
		"Â", "ä", "Ä",
		"&eacute;",
		"&eacute;",
		"&egrave;",
		"&egrave;", "ê",
		"Ê", "ë", "Ë",
		"È", "É", "é",
		"è", "Ì", "ì",
		"Í", "í", "î",
		"Î", "ï", "Ï",
		"Ò", "ò", "Ó",
		"ó", "ô", "Ô",
		"ö", "Ö", "ù",
		"Ù", "Ú", "ú",
		"û", "Û", "ü",
		"Ü", "Ý", "ý",
		"ç"
	};
	
	/**
	 * Replacement chars for the array REGEXP_ACCENTS_CEDILLES
	 */
	private static final String[] REGEXP_STRING_ACCENTS_CEDILLAS = {
		"A", "a", "A",
		"a", "A",
		"a", "A",
		"a", "A",
		"e", "E",
		"e", "E",
		"e", "E",
		"e", "E",
		"E", "E",
		"e", "e",
		"I", "i",
		"I", "i",
		"i", "I",
		"i", "I",
		"O", "o",
		"O", "o",
		"o", "O",
		"o", "O",
		"u", "U",
		"U", "u",
		"u", "U",
		"u", "U",
		"Y", "y",
		"c"
	};
	
	/** Allowed chars for words separator */
	private static final String[] SEPARATORS_FOR_UPPER_BEGINNING_NAME = {
		" ",
		"'",
		"\"",
		"-",
		"_"
	};
	
	/**
	 * Bad word separators chars for emails
	 */
	public static final String[] BAD_SEPARATOR_FOR_EMAIL = {" ", "'", "\""};

	/**
	 * Good words separators for emails
	 */
	public static final String[] GOOD_SEPARATOR_FOR_EMAIL = {"_", "_", "_"};

	/**
	 * Chars to replace in telephone numbers
	 */
	public static final String[] BAD_SEPARATOR_FOR_PHONE = {
		"-", " ", "\\.",
		"/", "\\+",
		"\\(", "\\)",
		";", ":", "_",
		","
	};
	
	/**
	 * Chars of remplactement for telephone numbers
	 */
	public static final String[] GOOD_SEPARATOR_FOR_PHONE = {
		"", "", "", "",
		"", "", "", "",
		"", "", ""
	};
	
	/** Regexp for formatting first names */
	private static final String REGEXP_FOR_FISRTNAME =
					"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;&egrave;" +
					"êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç' -]+";
					
	/** Regexp for formatting last names */
	private static final String REGEXP_FOR_LASTNAME =
					"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;&egrave;" +
					"êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç'\"\\s -_]+";
					
	/**
	 * Bad char separators for IDs
	 */
	private static final String[] BAD_SEPARATOR_FOR_ID = {" ", "'", "\"", "-"};
	
	/**
	 * Chars authorized for passwords
	 * (No O,0 and I,1,l etc.)
	 */
	private static final String GOOD_PASSWORD =
					"abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789/";
	
	/** Authorized characters for numerical identifier. */
	private static final String REGEXP_FOR_NUMERICAL_ID = "-?[0123456789]+";
	
	/** Authorized characters for numerical identifier. */
	private static final String REGEXP_FOR_ALPHA_NUMERICAL_ID =
					"[\\p{Alpha}0123456789]+";

	/**
	 * Find if a string is in an array
	 * @Deprecated because the method is in Java
	 *
	 * @param array the array
	 * @param name the string
	 *
	 * @return boolean
	 */
	@Deprecated
	public static boolean containsInTab(final String[] array, final String name) {
		return Arrays.asList(array).contains(name);
	}

	/**
	 * Normalize accents and cedillas
	 *
	 * @param src Source string
	 *
	 * @return Filtered string
	 */
	public static String removeBadChars(final String src) {
		return filterRegexp(src, REGEXP_ACCENTS_CEDILLAS,
						REGEXP_STRING_ACCENTS_CEDILLAS);
	}

	/**
	 * Filter the string src by removing the chars in srcRegexp
	 * by the ones in destRegexp
	 *
	 * @param src
	 * @param srcRegexp
	 * @param destRegexp
	 *
	 * @return the filered string
	 */
	public static String filterRegexp(final String src,
					final String[] srcRegexp,
					final String[] destRegexp) {
		String dest = src;

		for (int i = 0; i < srcRegexp.length; i++) {
			dest = dest.replaceAll(srcRegexp[i], destRegexp[i]);
		}

		return dest;
	}

	/**
	 * Remove all the occurences of a string in a string
	 *
	 * @param chars to remove
	 * @param string to filter
	 *
	 * @return
	 */
	private static String filterDelStringIntoString(final String charactere,
					final String string) {
		String returned = "";
		String tmp = string;
		int i = tmp.indexOf(charactere);

		while ((i != -1) && (i < string.length())) {
			returned += tmp.substring(0, i);
			tmp = tmp.substring(i + 1, tmp.length());
			i = tmp.indexOf(charactere);
		}

		if (tmp.length() > 0) {
			returned += tmp;
		}

		return returned;
	}

	/**
	 * Transform a telephon number in the international display
	 *
	 * @param phone2parse
	 *
	 * @return the filtered phone number
	 */
	public static String filterPhone(final String phone2parse) {
		// We remove spaces, dots and dashes
		String phoneResult = filterRegexp(phone2parse,
						BAD_SEPARATOR_FOR_PHONE,
						GOOD_SEPARATOR_FOR_PHONE);

		switch (phoneResult.length()) {
			case 8:
				return "331" + phoneResult;

			case 10:
				return "33" + phoneResult.substring(1, phoneResult.length());

			default:
				return phoneResult;
		}
	}

	/**
	 * Uppercased all the words of a string
	 *
	 * @param string
	 *
	 * @return String with caps for all characters after space, "-", etc ...
	 */
	public static String toUpperCaseAllBeginningNames(final String string) {
		if (string.length() == 0) {
			return "";
		}

		String returned = "";
		String tmp = string;

		// The string must start with a upper case
		tmp = tmp.substring(0, 1).toUpperCase() + tmp.substring(1, tmp.length()).toLowerCase();

		for (int j = 0; j < SEPARATORS_FOR_UPPER_BEGINNING_NAME.length; j++) {
			int i = tmp.indexOf(SEPARATORS_FOR_UPPER_BEGINNING_NAME[j]);

			while ((i != -1) && (i < (tmp.length() - 1))) {
				returned += tmp.substring(0, i + 1);

				try {
					tmp = tmp.substring(i + 1, i + 2).toUpperCase() + tmp.substring(i + 2, tmp.length());
				} catch (StringIndexOutOfBoundsException e) {
					LOGGER.error(e + " caused by '" + string + "'");
					throw e;
				}

				i = tmp.indexOf(SEPARATORS_FOR_UPPER_BEGINNING_NAME[j]);
			}
		}

		if (tmp.length() > 0) {
			returned += tmp;
		}

		return returned;
	}

	/**
	 * M&eacute;thode permettant de formatter le nouveau sn!
	 * @param sn
	 * @return the filtered surname
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterSn(final String sn)
					throws CharacterUnacceptedException {
		String tmp = toUpperCaseAllBeginningNames(filterName(sn));

		if (!tmp.matches(REGEXP_FOR_LASTNAME)) {
			throw new CharacterUnacceptedException();
		}

		return tmp;
	}

	/**
	 * Remove bad chars from a string
	 *
	 * @param startString
	 * @return
	 */
	private static String filterBadChars(final String startString) {
		String tmp = filterName(startString);

		for (int i = 0; i < BAD_SEPARATOR_FOR_ID.length; i++) {
			tmp = filterDelStringIntoString(BAD_SEPARATOR_FOR_ID[i], tmp);
		}

		// Remove all accents and cedillas
		tmp = removeBadChars(tmp);

		return tmp;
	}

	/**
	 * Returns the uid on 14 chars and well formatted
	 *
	 * @param sn the last name to filter
	 * @return the filtered uid
	 */
	public static String filterUid(final String sn) {
		String tmp = filterBadChars(sn);

		if (tmp.length() > 14) {
			return tmp.substring(0, 14);
		} else {
			return tmp;
		}
	}

	/**
	 * Returns the uid on 8 chars and well formatted
	 *
	 * @param sn the last name to filter
	 * @return the filtered short uid
	 */
	public static String filterShortUid(final String sn) {
		String tmp = filterBadChars(sn);

		if (tmp.length() > 8) {
			return tmp.substring(0, 8);
		} else {
			return tmp;
		}
	}

	/**
	 * Filter a string to match a last name
	 * 
	 * @param name the last name to filter
	 * @return the filtered patronimic name
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterLastName(final String name)
					throws CharacterUnacceptedException {
		String tmp = toUpperCaseAllBeginningNames(filterName(name));

		if (!tmp.matches(REGEXP_CHARACTERS)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
	}

	/**
	 * Filter a string to match a first name
	 * 
	 * @param name the first name to filter
	 * @return the filtered public given name
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterFirstName(final String name)
					throws CharacterUnacceptedException {
		String tmp = toUpperCaseAllBeginningNames(filterName(name));

		if (!tmp.matches(REGEXP_FOR_FISRTNAME)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
	}

	/**
	 * Filter a string to match a givenName
	 * 
	 * @param oldValue the value to filter
	 * @return the filtered givenname
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterGivenName(final String oldValue)
					throws CharacterUnacceptedException {
		String tmp = toUpperCaseAllBeginningNames(filterName(oldValue));

		if (!tmp.matches(REGEXP_FOR_FISRTNAME)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
	}

	/**
	 * Generate a 8 chars long password
	 *
	 * @return Le mot de passe
	 */
	public static String generatePwd() {
		StringBuffer passwd = new StringBuffer("");
		Random r = new Random();

		for (int i = 0; i < 8; i++) {
			passwd.append(GOOD_PASSWORD.charAt(r.nextInt(64)));
		}

		return passwd.toString();
	}

	/**
	 * Remove trailing and starting spaces
	 * and replace remaining spaces and dots by dashes
	 *
	 * @param aString the string to filter
	 *
	 * @return the filtered string
	 */
	public static String filterName(final String aString) {
		String tmp = aString.trim().replace('.', '-').toLowerCase();

		while (tmp.lastIndexOf('-') == (tmp.length() - 1)) {
			if (tmp.length() > 1) {
				tmp = tmp.substring(0, tmp.length() - 1);
			} else {
				tmp = "UNKNOWN";
			}
		}

		return tmp;
	}

	/**
	 * Trim a string
	 * @deprecated because it is a simple wrapper for a Java method
	 *
	 * @param aString the string to filter
	 * @return the filtered string
	 */
	@Deprecated
	public static String filterString(final String aString) {
		return aString.trim();
	}

	/**
	 * Filters numerical identifier.
	 *
	 * @param value the string
	 * @return the normalized String
	 * @throws CharacterUnacceptedException launch if and only if the argument
	 *         is not a numerical identifier
	 */
	public static String filterNumber(final String value)
					throws CharacterUnacceptedException {
		int n = Integer.parseInt(value);
		String tmp = String.valueOf(n);

		if (!tmp.matches(REGEXP_FOR_NUMERICAL_ID)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
	}

	/**
	 * Filter all alphanumeric characters.
	 * @param value the original value
	 * @return the filtered string
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterAlpha(final String value)
					throws CharacterUnacceptedException {
		String tmp = value.trim();

		if (!tmp.matches(REGEXP_FOR_ALPHA_NUMERICAL_ID)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
	}

	/**
	 * Converts Date into timestamp string.
	 *
	 * @param value A string representation fo a date
	 * @param format The format of Date with representation used by
	 *        SimpleDateFormat
	 *
	 * @return String A string containing correspondant timestamp
	 *
	 * @throws CharacterUnacceptedException thrown if an rejected character
	 * is encountered during analysis
	 */
	public static String filterDate(final String value, final String format)
					throws CharacterUnacceptedException {
		String tmp = value.trim();

		if (tmp.length() > 0) {
			SimpleDateFormat myFormat = new SimpleDateFormat(format);
			Date myDate = myFormat.parse(value, new ParsePosition(0));

			if (myDate != null) {
				return DateUtils.format(myDate);
			}
		}

		throw new CharacterUnacceptedException(tmp);
	}
}
