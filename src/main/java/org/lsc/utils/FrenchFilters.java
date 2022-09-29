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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Manage all common string manipulation for french
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;;
 */
public final class FrenchFilters {

	// Utility class
	private FrenchFilters() {}
	
	/** The regexep for authorized characters */
	private static final String REGEXP_CHARACTERS =
					"[\\p{Alpha}\\s'\"áÁ&agrave;&agrave;âÂäÄ&eacute;&eacute;&egrave;" + "&egrave;êÊëËÌìÍíîÎïÏÒòÓóôÔöÖùÙÚúûÛüÜÝýç-]+";

	/** Array of accents and cedillas */
	private static final String[] REGEXP_ACCENTS_CEDILLAS = {
		"À", "à", "á", "Á",
		"&agrave;", "&agrave;",
		"â", "Â",
		"ä", "Ä",
		"ã", "Ã",
		"&eacute;", "&eacute;",
		"&egrave;", "&egrave;",
		"ê", "Ê",
		"ë", "Ë",
		"È", "É",
		"é", "è",
		"ẽ", "Ẽ",
		"Ì", "ì",
		"Í", "í",
		"î", "Î",
		"ï", "Ï",
		"Ò", "ò", 
		"Ó", "ó",
		"ô", "Ô",
		"ö", "Ö",
		"õ", "Õ",
		"ù", "Ù",
		"Ú", "ú",
		"û", "Û",
		"ü", "Ü",
		"Ý", "ý",
		"Ÿ", "ÿ",
		"ç", "Ç",
		"ñ", "Ñ"
	};

	/**
	 * Replacement chars for the array REGEXP_ACCENTS_CEDILLES
	 */
	private static final String[] REGEXP_STRING_ACCENTS_CEDILLAS = {
		"A", "a", "a", "A",
		"a", "A",
		"a", "A",
		"a", "A",
		"a", "A",
		"e", "E",
		"e", "E",
		"e", "E",
		"e", "E",
		"E", "E",
		"e", "e",
		"e", "E",
		"I", "i",
		"I", "i",
		"i", "I",
		"i", "I",
		"O", "o",
		"O", "o",
		"o", "O",
		"o", "O",
		"o", "O",
		"u", "U",
		"U", "u",
		"u", "U",
		"u", "U",
		"Y", "y",
		"Y", "y",
		"c", "C",
		"n", "N"
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
					"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄãÃ&eacute;&eacute;&egrave;&egrave;" +
					"êÊëËẽẼÌìÍíîÎïÏÒòÓóôÔöÖõÕùÙÚúûÛüÜÝýŸÿçÇñÑ' -]+";

	/** Regexp for formatting last names */
	private static final String REGEXP_FOR_LASTNAME =
					"[\\p{Alpha}áÁ&agrave;&agrave;âÂäÄãÃ&eacute;&eacute;&egrave;&egrave;" +
					"êÊëËẽẼÌìÍíîÎïÏÒòÓóôÔöÖõÕùÙÚúûÛüÜÝýŸÿçÇñÑ'\"\\s -_]+";

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

		if (srcRegexp.length == destRegexp.length) {
			for (int i = 0; i < srcRegexp.length; i++) {
				dest = dest.replaceAll(srcRegexp[i], destRegexp[i]);
			}
		}
		return dest;
	}

	/**
	 * Transform a telephone number in the international display
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
		
		char[] tmp = string.toLowerCase().toCharArray();
		// The string must start with a upper case
		tmp[0] = Character.toUpperCase(tmp[0]);
		
		for (int i = 1; i < tmp.length - 1; i++) {
			char c = tmp[i];
			for (int j = 0; j < SEPARATORS_FOR_UPPER_BEGINNING_NAME.length; j++) {
				if (c == SEPARATORS_FOR_UPPER_BEGINNING_NAME[j].charAt(0)) {
					tmp[i + 1] = Character.toUpperCase(tmp[i + 1]);
					break;
				}
			}
		}
		return new String(tmp);
	}

	/**
	 * Format a sn
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
	 * @return String without the bad chars
	 */
	private static String filterBadChars(final String startString) {
		String tmp = filterName(startString);

		for (int i = 0; i < BAD_SEPARATOR_FOR_ID.length; i++) {
			tmp = tmp.replaceAll(BAD_SEPARATOR_FOR_ID[i], "");
		}

		// Remove all accents and cedillas
		tmp = removeBadChars(tmp);

		return tmp;
	}

	public static String filterLengthString(final String sn, int length) {
		String tmp = filterBadChars(sn);

		if (tmp.length() > length) {
			return tmp.substring(0, length);
		} else {
			return tmp;
		}
	}

	/**
	 * Returns the uid on 14 chars and well formatted
	 *
	 * @param sn the last name to filter
	 * @return the filtered uid
	 */
	public static String filterUid(final String sn) {
		return filterLengthString(sn, 14);
	}

	/**
	 * Returns the uid on 8 chars and well formatted
	 *
	 * @param sn the last name to filter
	 * @return the filtered short uid
	 */
	public static String filterShortUid(final String sn) {
		return filterLengthString(sn, 8);
	}

	public static String filterStringRegExp(final String value, final String regexp)
					throws CharacterUnacceptedException {
		String tmp = toUpperCaseAllBeginningNames(filterName(value));

		if (!tmp.matches(regexp)) {
			throw new CharacterUnacceptedException(tmp);
		}

		return tmp;
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
		return filterStringRegExp(name, REGEXP_CHARACTERS);
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
		return filterStringRegExp(oldValue, REGEXP_FOR_FISRTNAME);
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
	 * Generate a 8 chars long password
	 *
	 * @return Le mot de passe
	 */
	public static String generatePwd() {
		StringBuilder passwd = new StringBuilder("");
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
