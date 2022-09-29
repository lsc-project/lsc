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

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *    * Neither the name of the LSC Project nor the names of its
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
package org.lsc.utils.output;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.lsc.LscDatasetModification;
import org.lsc.LscModificationType;
import org.lsc.LscModifications;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Provides a localized logback layout for LDAP entries.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdifLayout extends PatternLayout {
	
	/* The separator of the log operations */
	protected static final String LOG_OPERATIONS_SEPARATOR = ",";

	/* Configurations from the logback.xml */
	private String logOperations;
	private boolean onlyLdif = false;

	/* The operations to log */
	protected Set<LscModificationType> operations;

	public LdifLayout() {
		operations = new HashSet<LscModificationType>();
	}
	
	/**
	 * Format the logging event. This formatter will use the default formatter
	 * or a LDIF pretty printer
	 *
	 * @param le
	 *            the logging event to format
	 * @return the formatted string
	 */
	@Override
	public final String doLayout(final ILoggingEvent le) {
		Object[] messages = le.getArgumentArray();
		String msg = "";

		if (messages == null || 
						messages.length == 0 ||
						messages[0] == null ||
						!(LscModifications.class.isAssignableFrom(messages[0].getClass()))) {
			if (!onlyLdif) {
				msg = super.doLayout(le);
			}
		} else {
			LscModifications lm = (LscModifications) messages[0];
			if (operations.contains(lm.getOperation())) {
				msg = format(lm);
			}
		}
		return msg;
	}
	
	public static String format(LscModifications lm) {
		StringBuilder msgBuffer = new StringBuilder();
		
		printHeader(msgBuffer);
		
		String dn = "";
		if (lm.getMainIdentifier() != null && lm.getMainIdentifier().length() > 0) {
			dn = lm.getMainIdentifier();
		}

		// print dn and base64 encode if it's not a SAFE-STRING
		msgBuffer.append("dn");
		if(isLdifSafeString(dn)) {
			msgBuffer.append(": ").append(dn);
		} else {
			msgBuffer.append(":: ").append(toBase64(dn));
		}
		msgBuffer.append("\n");

		switch (lm.getOperation()) {
			case CREATE_OBJECT:
				msgBuffer.append("changetype: add\n");
				msgBuffer.append(listToLdif(lm.getLscAttributeModifications(), true));
				break;
			case CHANGE_ID:
				LdapName ln;
				try {
					ln = new LdapName(lm.getNewMainIdentifier());
					msgBuffer.append("changetype: modrdn\nnewrdn: ");
					msgBuffer.append(ln.get(ln.size()-1));
					msgBuffer.append("\ndeleteoldrdn: 1\nnewsuperior: ");
					if (ln.size() > 1) {
						msgBuffer.append(ln.getPrefix(ln.size()-1));
					}
					msgBuffer.append("\n");
				} catch (InvalidNameException e) {
					msgBuffer.append("changetype: modrdn\nnewrdn: ");
					msgBuffer.append(lm.getNewMainIdentifier());
					msgBuffer.append("\ndeleteoldrdn: 1\nnewsuperior: ");
					msgBuffer.append(lm.getNewMainIdentifier());
					msgBuffer.append("\n");
				}
				break;
			case UPDATE_OBJECT:
				msgBuffer.append("changetype: modify\n");
				msgBuffer.append(listToLdif(lm.getLscAttributeModifications(), false));
				break;
			case DELETE_OBJECT:
				msgBuffer.append("changetype: delete\n");
				break;
			default:
		}
		msgBuffer.append("\n");
		return msgBuffer.toString();
	}

	private static void printHeader(StringBuilder msgBuffer) {
		msgBuffer.append("# ").append(new Date()).append("\n");
	}

	/**
	 * Pretty print the modification items.
	 *
	 * @param modificationItems
	 *            the modification items to pretty print
	 * @param addEntry
	 *            is this a new entry
	 * @return the string to log
	 */
	private static String listToLdif(final List<LscDatasetModification> modificationItems, final boolean addEntry) {
		StringBuilder sb = new StringBuilder();

		for(LscDatasetModification mi: modificationItems) {
			try {
				if (!addEntry) {
					switch (mi.getOperation()) {
						case DELETE_VALUES:
							sb.append("delete: ").append(mi.getAttributeName()).append("\n");
							break;
						case REPLACE_VALUES:
							sb.append("replace: ").append(mi.getAttributeName()).append("\n");
							break;
						case ADD_VALUES:
						default:
							sb.append("add: ").append(mi.getAttributeName()).append("\n");
					}
				}
				printAttributeToStringBuffer(sb, mi.getAttributeName(), mi.getValues());
				if (!addEntry) {
					sb.append("-\n");
				}
			} catch (NamingException e) {
				sb.append(mi.getAttributeName()).append(": ").append("!!! Unable to print value !!!\n");
			}
		}
		return sb.toString();
	}

	public static void printAttributeToStringBuffer(StringBuilder sb, String attrName, List<Object> values) throws NamingException {
		String sValue = null;
		for (Object value: values) {
			// print attribute name
			sb.append(attrName);

			// print value and base64 encode it if it's not a
			// SAFE-STRING per RFC2849
			sValue = getStringValue(value);
			if(isLdifSafeString(sValue)) {
				if (value instanceof byte[]) {
					sb.append(": ").append(sValue);
				} else {
					sb.append(": ").append(value);
				}
			} else {
				if (value instanceof byte[]) {
					sb.append(":: ").append(toBase64((byte[])value));
				} else {
					sb.append(":: ").append(toBase64(sValue));
				}
			}
			
			// new line
			sb.append("\n");
		}

	}

	private static String getStringValue(Object value) {
		if (value instanceof byte[]) {
			return new String((byte[]) value);
		} else {
			return value.toString();
		}
	}

	public static String toBase64(String value) {
		return new String(new Base64().encode(value.getBytes(Charset.forName("utf-8"))));
	}
	
	public static String toBase64(byte[] value) {
		return new String(new Base64().encode(value));
	}

	/**
	 * <P>
	 * Test if a character is a SAFE-CHAR in a SAFE-STRING for LDIF attribute
	 * value format, as defined in RFC2849. This method should not be used for
	 * the first character of a string, see isLdifSafeInitChar(char c).
	 * </P>
	 * <P>
	 * In detail, this checks that the character is:
	 * <ul>
	 * <li>Less than or equal to ASCII 127 decimal character</li>
	 * <li>Not NUL</li>
	 * <li>Not LF</li>
	 * <li>Not CR</li>
	 * </ul>
	 * </P>
	 *
	 * @param c
	 *            The character to test
	 * @return true if char is a SAFE-CHAR, false otherwise
	 */
	private static boolean isLdifSafeChar(char c) {
		if ((int) c > 127) {
			return false;
		}
		switch ((int) c) {
			case 0x00: // NUL
			case 0x0A: // LF
			case 0x0D: // CR
				return false;
			default:
				return true;
		}
	}

	/**
	 * <P>
	 * Test if a character is a SAFE-INIT-CHAR for a SAFE-STRING for LDIF
	 * attribute value format, as defined in RFC2849. This method should only be
	 * used for the first character of a string, see isLdifSafeChar(char c).
	 * </P>
	 * <P>
	 * In detail, this checks that the character is:
	 * <ul>
	 * <li>Less than or equal to ASCII 127 decimal character</li>
	 * <li>Not NUL</li>
	 * <li>Not LF</li>
	 * <li>Not CR</li>
	 * <li>Not SPACE</li>
	 * <li>Not colon ":"</li>
	 * <li>Not less-than "<"</li>
	 * </ul>
	 * </P>
	 *
	 * @param c
	 *            The character to test
	 * @return true if char is SAFE-INIT-CHAR, false otherwise
	 */
	private static boolean isLdifSafeInitChar(char c) {
		if ((int) c > 127) {
			return false;
		}
		switch ((int) c) {
			case 0x00: // NUL
			case 0x0A: // LF
			case 0x0D: // CR
			case 0x20: // SPACE
			case 0x3A: // colon ":"
			case 0x3C: // less-than "<"
				return false;
			default:
				return true;
		}
	}

	/**
	 * <P>
	 * Test if a string is a valid SAFE-STRING for LDIF attribute value format,
	 * as defined in RFC2849.
	 * </P>
	 * <P>
	 * In detail, this checks that:
	 * <ul>
	 * <li>The string's first character is a safe init char</li>
	 * <li>The string's subsequent characters are a safe chars</li>
	 * </ul>
	 * </P>
	 *
	 * @param s
	 *            The string to test
	 * @return true if is a SAFE-STRING, false otherwise
	 */
	private static boolean isLdifSafeString(String s) {
		// check if first character is a SAFE-INIT-CHAR
		if (s.length() > 0 && !isLdifSafeInitChar(s.charAt(0))) {
			return false;
		}

		// fail if any subsequent characters are not SAFE-CHARs
		for (int i = 1; i < s.length(); i++) {
			if (!isLdifSafeChar(s.charAt(i))) {
				return false;
			}
		}

		// if we got here, there are no bad chars
		return true;
	}

	/**
	 * Parse logOpertaions string for backward compatibility to configuration old style
	 */
	@Override
	public void start() {
		/* Parse logOperations */
		if (logOperations != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(logOperations, LOG_OPERATIONS_SEPARATOR);
			String token = null;
			while (st.hasMoreTokens()) {
				token = st.nextToken().toLowerCase();
				LscModificationType op = LscModificationType.getFromDescription(token);
				if (op != null) {
					operations.add(op);
				}
			}
		} else if (operations.isEmpty()){
			/* Add all the operations */
			for(LscModificationType type: LscModificationType.values()) {
				operations.add(type);
			}
		}
		super.start();
	}

	/**
	 * @param onlyLdif the onlyLdif to set
	 */
	public void setOnlyLdif(boolean onlyLdif) {
		this.onlyLdif = onlyLdif;
	}

	public void setLogOperations(LscModificationType[] lscModificationTypes) {
		if(lscModificationTypes != null && lscModificationTypes.length > 0) {
			operations.addAll(Arrays.asList(lscModificationTypes));
		}
	}

	public void setLogOperations(String logOperations) {
		this.logOperations = logOperations;
	}
}
