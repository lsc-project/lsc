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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.utils.output;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;

import org.lsc.Configuration;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

/**
 * Provides a localized logback layout for LDAP entries.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdifLayout extends PatternLayout {

	/* The separator of the log operations */
	protected static String LOG_OPERATIONS_SEPARATOR = ",";

	/* Configurations from the logback.xml */
	private String logOperations;
	private boolean onlyLdif = false;

	/* The operations to log */
	protected Set<JndiModificationType> operations;

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
						!(JndiModifications.class.isAssignableFrom(messages[0].getClass()))) {
			if (!onlyLdif) {
				msg = super.doLayout(le);
			}
		} else {
			JndiModifications jm = (JndiModifications) messages[0];

			if (operations.contains(jm.getOperation())) {
				StringBuffer msgBuffer = new StringBuffer();
				String baseUrl = (String) Configuration.getDstProperties().get("java.naming.provider.url");
				baseUrl = baseUrl.substring(baseUrl.lastIndexOf('/') + 1);
				String dn = "";
				if (jm.getDistinguishName() != null && jm.getDistinguishName().length() > 0) {
					dn = jm.getDistinguishName();
					if (!dn.endsWith(baseUrl)) {
						dn += "," + baseUrl;
					}
				} else {
					dn = baseUrl;
				}

				// print dn and base64 encode if it's not a SAFE-STRING
				msgBuffer.append("dn" + (isLdifSafeString(dn) ? ": " + dn : ":: " + toBase64(dn)) + "\n");

				switch (jm.getOperation()) {
					case ADD_ENTRY:
						msgBuffer.append("changetype: add\n");
						msgBuffer.append(listToLdif(jm.getModificationItems(), true));
						break;
					case MODRDN_ENTRY:
						LdapName ln;
						try {
							ln = new LdapName(jm.getNewDistinguishName());
							msgBuffer.append("changetype: modrdn\nnewrdn: ");
							msgBuffer.append(ln.get(0));
							msgBuffer.append("\ndeleteoldrdn: 1\nnewsuperior: ");
							msgBuffer.append(ln.getSuffix(1));
							msgBuffer.append("\n");
						} catch (InvalidNameException e) {
							msgBuffer.append("changetype: modrdn\nnewrdn: ");
							msgBuffer.append(jm.getNewDistinguishName());
							msgBuffer.append("\ndeleteoldrdn: 1\nnewsuperior: ");
							msgBuffer.append(jm.getNewDistinguishName());
							msgBuffer.append(",");
							msgBuffer.append(baseUrl);
							msgBuffer.append("\n");
						}
						break;
					case MODIFY_ENTRY:
						msgBuffer.append("changetype: modify\n");
						msgBuffer.append(listToLdif(jm.getModificationItems(), false));
						break;
					case DELETE_ENTRY:
						msgBuffer.append("changetype: delete\n");
						break;
					default:
				}

				msgBuffer.append("\n");
				msg = msgBuffer.toString();
			}
		}
		return msg;
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
	private String listToLdif(final List<?> modificationItems, final boolean addEntry) {
		StringBuffer sb = new StringBuffer();
		Iterator<?> miIter = modificationItems.iterator();
		boolean first = true;

		while (miIter.hasNext()) {
			ModificationItem mi = (ModificationItem) miIter.next();
			Attribute attr = mi.getAttribute();
			try {
				if (!addEntry) {
					if (!first) {
						sb.append("-\n");
					}
					switch (mi.getModificationOp()) {
						case DirContext.REMOVE_ATTRIBUTE:
							sb.append("delete: ").append(attr.getID()).append("\n");
							break;
						case DirContext.REPLACE_ATTRIBUTE:
							sb.append("replace: ").append(attr.getID()).append("\n");
							break;
						case DirContext.ADD_ATTRIBUTE:
						default:
							sb.append("add: ").append(attr.getID()).append("\n");
					}
				}
				NamingEnumeration<?> ne = attr.getAll();
				String value = null;
				while (ne.hasMore()) {
					// print attribute name
					sb.append(attr.getID());

					// print value and base64 encode it if it's not a
					// SAFE-STRING per RFC2849
					value = getStringValue(ne.next());
					sb.append((isLdifSafeString(value) ? ": " + value : ":: " + toBase64(value)));

					// new line
					sb.append("\n");
				}
			} catch (NamingException e) {
				sb.append(attr.getID()).append(": ").append("!!! Unable to print value !!!\n");
			}
			first = false;
		}
		return sb.toString();
	}

	private String getStringValue(Object value) {
		if (value instanceof byte[]) {
			return new String((byte[]) value);
		} else {
			return value.toString();
		}
	}

	private String toBase64(String value) {
		return new String(new Base64().encode(value.getBytes()));
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
	private boolean isLdifSafeChar(char c) {
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
	private boolean isLdifSafeInitChar(char c) {
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
	private boolean isLdifSafeString(String s) {
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
	 * Parse options
	 *
	 */
	@Override
	public void start() {
		/* Parse logOperations */
		operations = new HashSet<JndiModificationType>();
		if (logOperations != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(logOperations, LOG_OPERATIONS_SEPARATOR);
			String token = null;
			while (st.hasMoreTokens()) {
				token = st.nextToken().toLowerCase();
				JndiModificationType op = JndiModificationType.getFromDescription(token);
				if (op != null) {
					operations.add(op);
				}
			}
		} else {
			/* Add all the operations */
			JndiModificationType[] values = JndiModificationType.values();
			for (int i = 0; i < values.length; i++) {
				operations.add(values[i]);
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

	/**
	 * @param logOperation the logOperation to set
	 */
	@Deprecated
	public void setLogOperation(String logOperation) {
		this.setLogOperations(logOperations);
	}

	public void setLogOperations(String logOperations) {
		this.logOperations = logOperations;
	}
}
