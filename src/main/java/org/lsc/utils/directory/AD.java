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
 *         Sebastien Bahloul&lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau&lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke&lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser&lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.utils.directory;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

/**
 * Utility class to manage specific entries for a Microsoft ActiveDirectory
 * 
 * @author Rémy-Christophe Schermesser &lt;remy-christophe@schermesser.com&gt;
 *
 */
public class AD {

	// Utility class
	private AD() {}
	
	/**
	 * Set or unset some bits to a UserAccountControl attribute of an AD
	 *
	 * @param origValue the original value of UserAccessControl
	 * @param constToApply an Array of constants to apply
	 * @return the modified value
	 */
	public static int userAccountControlSet(int origValue, String[] constToApply) {
		int result = origValue;

		for(String str: constToApply) {
			Integer constValue = Integer.parseInt(str);
			if (setHexValue.containsKey(constValue)) {
				result = result | setHexValue.get(constValue);
			}

			if (unsetHexValue.containsKey(constValue)) {
				result = result & unsetHexValue.get(constValue);
			}
		}

		return result;
	}

	/**
	 * Check if a bit is set in UserAccountControl
	 *
	 * @param value the value of UserAccountControl
	 * @param constToCheck a constant to test
	 * @return is the attribute present
	 */
	public static boolean userAccountControlCheck(int value, String constToCheck) {
		Integer constValue = Integer.parseInt(constToCheck);
		return ((value & constValue) > 0);
	}

	/**
	 * Toggle a bit in UserAccountControl
	 *
	 * @param value the value of UserAccountControl
	 * @param constToApply the bit to toggle
	 * @return the modified value
	 */
	public static int userAccountControlToggle(int value, String constToApply) {
		Integer constValue = Integer.parseInt(constToApply);
		if ((value & constValue) == constValue) {
			return (value & ~constValue);
		} else {
			return (value | constValue);
		}
	}

	/**
	 * Encode a password so that it can be updated in Active Directory
	 * in the field unicodePwd.
	 *
	 * @param password The cleartext password to be encoded
	 * @return The value to write in AD's unicodePwd attribute
	 * @throws UnsupportedEncodingException
	 */
	public static String getUnicodePwd(String password) throws UnsupportedEncodingException {
		String quotedPassword = "\"" + password + "\"";
		return new String(quotedPassword.getBytes("UTF-16LE"));
	}


	/**
	 * The Unix epoch (1 January 1970 00:00:00 UT) in AD's time format 
	 */
	private static final Long UNIX_EPOCH_IN_AD_TIME = 116444736000000000L;

	/**
	 * <p>Transform an AD timestamp to a Unix timestamp.</p>
	 * 
	 * <p>AD timestamps are the number of 100-nanosecond ticks since 1 January 1601 00:00:00 UT.
	 * Unix timestamps are the number of seconds elapsed since the start of the epoch at 1 January 1970 00:00:00 UT.
	 * Source: http://en.wikipedia.org/wiki/System_time.</p>
	 * 
	 * <p>This method returns the number of seconds elapsed since the start of the Unix epoch
	 * as represented by the AD timestamp given. Any extra precision as provided by the AD timestamp
	 * is discarded (truncated, not rounded).</p>
	 * 
	 * @param aDTime An AD timestamp as a long
	 * @return Timestamp in seconds since the Unix epoch (1 January 1970 00:00:00 UT)
	 */
	public static long aDTimeToUnixTimestamp(long aDTime) {
		// Subtract Unix epoch in AD time, and divide by 10^7 to switch from 100 ns intervals to seconds
		return (long) ( (aDTime - UNIX_EPOCH_IN_AD_TIME) / (long) Math.pow(10, 7) );
	}
	
	/**
	 * <p>Helper method to automatically parse an AD timestamp from a String before
	 * calling {@link #aDTimeToUnixTimestamp(long)}.</p>
	 * 
	 * @param aDTimeString A string containing an AD timestamp
	 * @return Timestamp in seconds since the Unix epoch (1 January 1970 00:00:00 UT)
	 * @see #aDTimeToUnixTimestamp(long)
	 */
	public static long aDTimeToUnixTimestamp(String aDTimeString) {
		Long ts = Long.parseLong(aDTimeString);
		return aDTimeToUnixTimestamp(ts);
	}
	
	/**
	 * <p>Transform a Unix timestamp to an AD timestamp.</p>
	 * 
	 * <p>AD timestamps are the number of 100-nanosecond ticks since 1 January 1601 00:00:00 UT.
	 * Unix timestamps are the number of seconds elapsed since the start of the epoch at 1 January 1970 00:00:00 UT.
	 * Source: http://en.wikipedia.org/wiki/System_time.</p>
	 * 
	 * <p>This method returns the number of 100-nanosecond ticks elapsed since the start of the AD epoch
	 * as represented by the Unix timestamp given. The extra precision in the AD timestamp representation
	 * is set to zeroes (0).</p>
	 * 
	 * @param unixTimestamp A Unix timestamp as an int
	 * @return Timestamp in 100-nanosecond ticks since the AD epoch (1 January 1601 00:00:00 UT)
	 */
	public static long unixTimestampToADTime(long unixTimestamp) {
		// Multiply by 10^7 to switch from seconds to 100 ns intervals and add Unix epoch in AD time
		return ( unixTimestamp * (long) Math.pow(10, 7) ) + UNIX_EPOCH_IN_AD_TIME;
	}
	
	/**
	 * <p>Helper method to automatically parse a Unix timestamp from a String before
	 * calling {@link #unixTimestampToADTime(long)}.</p>
	 * 
	 * @param unixTimestampString A Unix timestamp as an long
	 * @return Timestamp in 100-nanosecond ticks since the AD epoch (1 January 1601 00:00:00 UT)
	 * @see #aDTimeToUnixTimestamp(long)
	 */
	public static long unixTimestampToADTime(String unixTimestampString) {
		return unixTimestampToADTime(Long.parseLong(unixTimestampString));
	}
	
	
	/**
	 * Return the number of weeks since the last logon
	 *
	 * @param lastLogonTimestamp
	 * @return the number of weeks since the last logon
	 */
	public static int getNumberOfWeeksSinceLastLogon(String lastLogonTimestamp) {
		if (lastLogonTimestamp == null || lastLogonTimestamp.length() == 0) {
			return 0;
		}
		long secondsToUnixTimeStamp = aDTimeToUnixTimestamp(lastLogonTimestamp);
		long lastLogonTime = (new Date().getTime() / 1000) - secondsToUnixTimeStamp;

		return (int) (lastLogonTime / (60 * 60 * 24 * 7));
	}

	/**
	 * Return the accountexpires time in Microsoft format
	 *
	 * @param expireDate the date in any format
	 * @param format The format of the date expireDate.  See http://java.sun.com/j2se/1.5.0/docs/api/java/text/SimpleDateFormat.html.
	 * @return the date in Microsoft AD format
	 * @throws ParseException
	 */
	public static long getAccountExpires(String expireDate, String format) throws ParseException {
		if (expireDate == null || expireDate.length() == 0) {
			return 0;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		long accountExpiresTimeStampMs = sdf.parse(expireDate).getTime() * (long) Math.pow(10, 4);
		return accountExpiresTimeStampMs + UNIX_EPOCH_IN_AD_TIME;
	}

	/**
	 * Returns the accountexpires time in Microsoft format
	 *
	 * @param expireDate the date in the format yyyy-MM-dd.
	 * @return the date in Microsoft AD format
	 * @throws ParseException
	 */
	public static long getAccountExpires(String expireDate) throws ParseException {
		return getAccountExpires(expireDate, "yyyy-MM-dd");
	}

	/**
	 * <p>Decode the binary value of a GUID and convert into a readble string representation of this UUID.</p>
	 * <p>Call this method with binary value of objectGUID attribute:<br/> 
	 * 	AD.binaryGuidToReadableUUID(srcBean.getDatasetFirstBinaryValueById("objectGUID"));</p>
	 * <p>Attribute objectGUID needs to be declared as binary in your AD connexion for this method to work.</p>
	 * @param GUID the binary GUID as sent by Microsoft AD
	 * @return the UUID/string representation of this binary GUID
	 */
	public static String binaryGuidToReadableUUID(byte[] GUID) {
		if (GUID != null) {
			String hex = new String(Hex.encodeHex(GUID));
			return hex.replaceFirst("^(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)(..)$","$4$3$2$1-$6$5-$8$7-$9$10-$11$12$13$14$15$16").toUpperCase();
		}
		return "";
	}

	/* The Hash of values to set or to unset  */
	private static final Map<Integer, Integer> setHexValue = new HashMap<Integer, Integer>();
	private static final Map<Integer, Integer> unsetHexValue = new HashMap<Integer, Integer>();

	/**
	 * Internal values in the AD in Hex
	 * See : http://support.microsoft.com/kb/305144
	 */
	public static final Integer UAC_SCRIPT = 0x0001;
	public static final Integer UAC_ACCOUNTDISABLE = 0x0002;
	public static final Integer UAC_HOMEDIR_REQUIRED = 0x0008;
	public static final Integer UAC_LOCKOUT = 0x0010;
	public static final Integer UAC_PASSWD_NOTREQD = 0x0020;
	public static final Integer UAC_PASSWD_CANT_CHANGE = 0x0040;
	public static final Integer UAC_ENCRYPTED_TEXT_PWD_ALLOWED = 0x0080;
	public static final Integer UAC_TEMP_DUPLICATE_ACCOUNT = 0x0100;
	public static final Integer UAC_NORMAL_ACCOUNT = 0x0200;
	public static final Integer UAC_INTERDOMAIN_TRUST_ACCOUNT = 0x0800;
	public static final Integer UAC_WORKSTATION_TRUST_ACCOUNT = 0x1000;
	public static final Integer UAC_SERVER_TRUST_ACCOUNT = 0x2000;
	public static final Integer UAC_DONT_EXPIRE_PASSWORD = 0x10000;
	public static final Integer UAC_MNS_LOGON_ACCOUNT = 0x20000;
	public static final Integer UAC_SMARTCARD_REQUIRED = 0x40000;
	public static final Integer UAC_TRUSTED_FOR_DELEGATION = 0x80000;
	public static final Integer UAC_NOT_DELEGATED = 0x100000;
	public static final Integer UAC_USE_DES_KEY_ONLY = 0x200000;
	public static final Integer UAC_DONT_REQ_PREAUTH = 0x400000;
	public static final Integer UAC_PASSWORD_EXPIRED = 0x800000;
	public static final Integer UAC_TRUSTED_TO_AUTH_FOR_DELEGATION = 0x1000000;

	/**
	 * The constants available in the configuration
	 * If it is starting by SET, it is to force the value on in the AD
	 * If it is starting by UNSET, it is to force the removing of the value in the AD
	 */
	public static final Integer UAC_SET_SCRIPT = UAC_SCRIPT;
	public static final Integer UAC_UNSET_SCRIPT = -UAC_SCRIPT;
	public static final Integer UAC_SET_ACCOUNTDISABLE = UAC_ACCOUNTDISABLE;
	public static final Integer UAC_UNSET_ACCOUNTDISABLE = -UAC_ACCOUNTDISABLE;
	public static final Integer UAC_SET_HOMEDIR_REQUIRED = UAC_HOMEDIR_REQUIRED;
	public static final Integer UAC_UNSET_HOMEDIR_REQUIRED = -UAC_HOMEDIR_REQUIRED;
	public static final Integer UAC_SET_LOCKOUT = UAC_LOCKOUT;
	public static final Integer UAC_UNSET_LOCKOUT = -UAC_LOCKOUT;
	public static final Integer UAC_SET_PASSWD_NOTREQD = UAC_PASSWD_NOTREQD;
	public static final Integer UAC_UNSET_PASSWD_NOTREQD = -UAC_PASSWD_NOTREQD;
	public static final Integer UAC_SET_PASSWD_CANT_CHANGE = UAC_PASSWD_CANT_CHANGE;
	public static final Integer UAC_UNSET_PASSWD_CANT_CHANGE = -UAC_PASSWD_CANT_CHANGE;
	public static final Integer UAC_SET_ENCRYPTED_TEXT_PWD_ALLOWED = UAC_ENCRYPTED_TEXT_PWD_ALLOWED;
	public static final Integer UAC_UNSET_ENCRYPTED_TEXT_PWD_ALLOWED = -UAC_ENCRYPTED_TEXT_PWD_ALLOWED;
	public static final Integer UAC_SET_TEMP_DUPLICATE_ACCOUNT = UAC_TEMP_DUPLICATE_ACCOUNT;
	public static final Integer UAC_UNSET_TEMP_DUPLICATE_ACCOUNT = -UAC_TEMP_DUPLICATE_ACCOUNT;
	public static final Integer UAC_SET_NORMAL_ACCOUNT = UAC_NORMAL_ACCOUNT;
	public static final Integer UAC_UNSET_NORMAL_ACCOUNT = -UAC_NORMAL_ACCOUNT;
	public static final Integer UAC_SET_INTERDOMAIN_TRUST_ACCOUNT = UAC_INTERDOMAIN_TRUST_ACCOUNT;
	public static final Integer UAC_UNSET_INTERDOMAIN_TRUST_ACCOUNT = -UAC_INTERDOMAIN_TRUST_ACCOUNT;
	public static final Integer UAC_SET_WORKSTATION_TRUST_ACCOUNT = UAC_WORKSTATION_TRUST_ACCOUNT;
	public static final Integer UAC_UNSET_WORKSTATION_TRUST_ACCOUNT = -UAC_WORKSTATION_TRUST_ACCOUNT;
	public static final Integer UAC_SET_SERVER_TRUST_ACCOUNT = UAC_SERVER_TRUST_ACCOUNT;
	public static final Integer UAC_UNSET_SERVER_TRUST_ACCOUNT = -UAC_SERVER_TRUST_ACCOUNT;
	public static final Integer UAC_SET_DONT_EXPIRE_PASSWORD = UAC_DONT_EXPIRE_PASSWORD;
	public static final Integer UAC_UNSET_DONT_EXPIRE_PASSWORD = -UAC_DONT_EXPIRE_PASSWORD;
	public static final Integer UAC_SET_MNS_LOGON_ACCOUNT = UAC_MNS_LOGON_ACCOUNT;
	public static final Integer UAC_UNSET_MNS_LOGON_ACCOUNT = -UAC_MNS_LOGON_ACCOUNT;
	public static final Integer UAC_SET_SMARTCARD_REQUIRED = UAC_SMARTCARD_REQUIRED;
	public static final Integer UAC_UNSET_SMARTCARD_REQUIRED = -UAC_SMARTCARD_REQUIRED;
	public static final Integer UAC_SET_TRUSTED_FOR_DELEGATION = UAC_TRUSTED_FOR_DELEGATION;
	public static final Integer UAC_UNSET_TRUSTED_FOR_DELEGATION = -UAC_TRUSTED_FOR_DELEGATION;
	public static final Integer UAC_SET_NOT_DELEGATED = UAC_NOT_DELEGATED;
	public static final Integer UAC_UNSET_NOT_DELEGATED = -UAC_NOT_DELEGATED;
	public static final Integer UAC_SET_USE_DES_KEY_ONLY = UAC_USE_DES_KEY_ONLY;
	public static final Integer UAC_UNSET_USE_DES_KEY_ONLY = -UAC_USE_DES_KEY_ONLY;
	public static final Integer UAC_SET_DONT_REQ_PREAUTH = UAC_DONT_REQ_PREAUTH;
	public static final Integer UAC_UNSET_DONT_REQ_PREAUTH = -UAC_DONT_REQ_PREAUTH;
	public static final Integer UAC_SET_PASSWORD_EXPIRED = UAC_PASSWORD_EXPIRED;
	public static final Integer UAC_UNSET_PASSWORD_EXPIRED = -UAC_PASSWORD_EXPIRED;
	public static final Integer UAC_SET_TRUSTED_TO_AUTH_FOR_DELEGATION = UAC_TRUSTED_TO_AUTH_FOR_DELEGATION;
	public static final Integer UAC_UNSET_TRUSTED_TO_AUTH_FOR_DELEGATION = -UAC_TRUSTED_TO_AUTH_FOR_DELEGATION;

	/* Populating the two hash */
	static {
		setHexValue.put(UAC_SET_SCRIPT, UAC_SCRIPT);
		unsetHexValue.put(UAC_UNSET_SCRIPT, ~UAC_SCRIPT);

		setHexValue.put(UAC_SET_ACCOUNTDISABLE, UAC_ACCOUNTDISABLE);
		unsetHexValue.put(UAC_UNSET_ACCOUNTDISABLE, ~UAC_ACCOUNTDISABLE);

		setHexValue.put(UAC_SET_HOMEDIR_REQUIRED, UAC_HOMEDIR_REQUIRED);
		unsetHexValue.put(UAC_UNSET_HOMEDIR_REQUIRED, ~UAC_HOMEDIR_REQUIRED);

		setHexValue.put(UAC_SET_LOCKOUT, UAC_LOCKOUT);
		unsetHexValue.put(UAC_UNSET_LOCKOUT, ~UAC_LOCKOUT);

		setHexValue.put(UAC_SET_PASSWD_NOTREQD, UAC_PASSWD_NOTREQD);
		unsetHexValue.put(UAC_UNSET_PASSWD_NOTREQD, ~UAC_PASSWD_NOTREQD);

		setHexValue.put(UAC_SET_PASSWD_CANT_CHANGE, UAC_PASSWD_CANT_CHANGE);
		unsetHexValue.put(UAC_UNSET_PASSWD_CANT_CHANGE, ~UAC_PASSWD_CANT_CHANGE);

		setHexValue.put(UAC_SET_ENCRYPTED_TEXT_PWD_ALLOWED, UAC_ENCRYPTED_TEXT_PWD_ALLOWED);
		unsetHexValue.put(UAC_UNSET_ENCRYPTED_TEXT_PWD_ALLOWED, ~UAC_ENCRYPTED_TEXT_PWD_ALLOWED);

		setHexValue.put(UAC_SET_TEMP_DUPLICATE_ACCOUNT, UAC_TEMP_DUPLICATE_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_TEMP_DUPLICATE_ACCOUNT, ~UAC_TEMP_DUPLICATE_ACCOUNT);

		setHexValue.put(UAC_SET_NORMAL_ACCOUNT, UAC_NORMAL_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_NORMAL_ACCOUNT, ~UAC_NORMAL_ACCOUNT);

		setHexValue.put(UAC_SET_INTERDOMAIN_TRUST_ACCOUNT, UAC_INTERDOMAIN_TRUST_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_INTERDOMAIN_TRUST_ACCOUNT, ~UAC_INTERDOMAIN_TRUST_ACCOUNT);

		setHexValue.put(UAC_SET_WORKSTATION_TRUST_ACCOUNT, UAC_WORKSTATION_TRUST_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_WORKSTATION_TRUST_ACCOUNT, ~UAC_WORKSTATION_TRUST_ACCOUNT);

		setHexValue.put(UAC_SET_SERVER_TRUST_ACCOUNT, UAC_SERVER_TRUST_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_SERVER_TRUST_ACCOUNT, ~UAC_SERVER_TRUST_ACCOUNT);

		setHexValue.put(UAC_SET_DONT_EXPIRE_PASSWORD, UAC_DONT_EXPIRE_PASSWORD);
		unsetHexValue.put(UAC_UNSET_DONT_EXPIRE_PASSWORD, ~UAC_DONT_EXPIRE_PASSWORD);

		setHexValue.put(UAC_SET_MNS_LOGON_ACCOUNT, UAC_MNS_LOGON_ACCOUNT);
		unsetHexValue.put(UAC_UNSET_MNS_LOGON_ACCOUNT, ~UAC_MNS_LOGON_ACCOUNT);

		setHexValue.put(UAC_SET_SMARTCARD_REQUIRED, UAC_SMARTCARD_REQUIRED);
		unsetHexValue.put(UAC_UNSET_SMARTCARD_REQUIRED, ~UAC_SMARTCARD_REQUIRED);

		setHexValue.put(UAC_SET_TRUSTED_FOR_DELEGATION, UAC_TRUSTED_FOR_DELEGATION);
		unsetHexValue.put(UAC_UNSET_TRUSTED_FOR_DELEGATION, ~UAC_TRUSTED_FOR_DELEGATION);

		setHexValue.put(UAC_SET_NOT_DELEGATED, UAC_NOT_DELEGATED);
		unsetHexValue.put(UAC_UNSET_NOT_DELEGATED, ~UAC_NOT_DELEGATED);

		setHexValue.put(UAC_SET_USE_DES_KEY_ONLY, UAC_USE_DES_KEY_ONLY);
		unsetHexValue.put(UAC_UNSET_USE_DES_KEY_ONLY, ~UAC_USE_DES_KEY_ONLY);

		setHexValue.put(UAC_SET_DONT_REQ_PREAUTH, UAC_DONT_REQ_PREAUTH);
		unsetHexValue.put(UAC_UNSET_DONT_REQ_PREAUTH, ~UAC_DONT_REQ_PREAUTH);

		setHexValue.put(UAC_SET_PASSWORD_EXPIRED, UAC_PASSWORD_EXPIRED);
		unsetHexValue.put(UAC_UNSET_PASSWORD_EXPIRED, ~UAC_PASSWORD_EXPIRED);

		setHexValue.put(UAC_SET_TRUSTED_TO_AUTH_FOR_DELEGATION, UAC_TRUSTED_TO_AUTH_FOR_DELEGATION);
		unsetHexValue.put(UAC_UNSET_TRUSTED_TO_AUTH_FOR_DELEGATION, ~UAC_TRUSTED_TO_AUTH_FOR_DELEGATION);
	}

}
