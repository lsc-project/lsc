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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Support different languages
 * 
 * @author Sebastien Bahloul <sbahloul@linagora.com>
 */
public class I18n {

	/** The logger */
	private static Logger logger = Logger.getLogger(I18n.class);

	/** The messages in the current language */
	private ResourceBundle messages;

	/** the instance */
	private static I18n instance;

	/** the current locale used to get the message */
	private Locale currentLocale;

	/** The directory containing the language specific files */
	private static final String localeDirectory = "resources";

	/** Change according the project name with the '_' character */
	private static final String PROJECT_NAME = "lsc";

	private static String sep = System.getProperty("file.separator");

	/**
	 * Get the private instance
	 * 
	 * @return the local instance
	 */
	private static I18n getInstance() {
		if (instance == null || instance.messages == null) {
			instance = new I18n();
			instance.defaultInitialize();
		}
		return instance;
	}

	/**
	 * Get the private instance specifying the locale to use.
	 * 
	 * @param locale
	 *            the locale to use
	 * @return the local instance
	 */
	private static I18n getInstance(final Locale locale) {
		if (instance == null || instance.messages == null) {
			instance = new I18n();
			try {
				instance.setLocaleAndLoadMessages(locale);
			} catch (IOException ie) {
				instance.defaultInitialize();
			}
		}
		return instance;
	}

	/**
	 * Initialize by default the resources loading engine
	 * 
	 * @throw java.lang.RuntimeException if the corresponding locale or file not found
	 */
	private void defaultInitialize() {
		Locale currentLocale = null;
		String lang = System.getenv("LANG");
		if (lang != null) {
			if (lang.indexOf(".") > 0) {
				lang = lang.substring(0, lang.indexOf("."));
			}
		} else {
			logger.info("No environemental LANG variable found. Defaulting to en_US.");
			lang = "en_US";
		}
		if (lang != null) {
			Locale[] locales = Locale.getAvailableLocales();
			for (int i = 0; i < locales.length; i++) {
				if (lang.compareToIgnoreCase(locales[i].toString()) == 0) {
					currentLocale = locales[i];
					break;
				}
			}
			if (currentLocale != null) {
				try {
					setLocaleAndLoadMessages(currentLocale);
				} catch (IOException e) {
					logger.fatal("Unable to open the locale message file for " + currentLocale + " ! Exiting...");
					throw new RuntimeException("Unable to find locale : " + lang + " ! Exiting ...");
				}
			} else {
				logger.fatal("Unable to find locale : " + lang + " ! Exiting ...");
				throw new RuntimeException("Unable to find locale : " + lang + " ! Exiting ...");
			}
		}
	}

	/**
	 * @param locales
	 */
	public static void setLocale(Locale locale) throws IOException {
		getInstance(locale);
	}

	private void setLocaleAndLoadMessages(Locale locale) throws IOException {
		currentLocale = locale;
		if (logger.isDebugEnabled())
			logger.debug("Setting locale to " + locale);
		try {
			// this.getClass().getClassLoader().getResource(".");
			messages = ResourceBundle.getBundle(localeDirectory + sep + PROJECT_NAME, currentLocale);
		} catch (MissingResourceException mre) {
			logger.fatal(mre, mre);
			if (logger.isDebugEnabled()) {
				logger.debug(System.getenv("CLASSPATH"));
			}
			throw mre;
		}
	}

	public String getLocalizedMessage(String code) {
		return getInstance().getLocalizedMessage(code);
	}

	public String getMessage(String code) {
		// if (code == null) {
		// return "I18n layer: Unknown null code !";
		// }
		try {
			return messages.getString(code);
		} catch (MissingResourceException mre) {
			System.err.println("I18n layer: unknown code " + code);
		}
		return null;
	}

	public static String getMessage(Object obj, String code) {
		return getInstance().getMessage((obj != null ? obj.getClass().getName() + "." : "") + code);
	}

	public static String getMessage(Object obj, String code, Object param1) {
		return getMessage(obj, code, new Object[] { param1 });
	}

	public static String getMessage(Object obj, String code, Object param1, Object param2) {
		return getMessage(obj, code, new Object[] { param1, param2 });
	}

	public static String getMessage(Object obj, String code, Object param1, Object param2, Object param3) {
		return getMessage(obj, code, new Object[] { param1, param2, param3 });
	}

	public static String getMessage(Object obj, String code, Object[] objs) {
		String message = getMessage(obj, code);
		if (message != null) {
			for (int i = 0; i < objs.length; i++) {
				if (objs[i] != null && message.indexOf("{" + i + "}") >= 0) {
					try {
						message = StringUtils.replace(message, "{" + i + "}", objs[i].toString());
					} catch (java.lang.IllegalArgumentException e) {
						logger.fatal(e, e);
					}
				}
			}
		}
		return message;
	}

	/**
	 * Return messages from locale chosen starting with a specified string.
	 * 
	 * @param prefix
	 * @return the map with the corresponding key / value pairs
	 */
	private Map<String, String> getKeysStarting(String prefix) {
		Map<String, String> m = new HashMap<String, String>();
		Enumeration<String> messagesNameEnum = messages.getKeys();
		while (messagesNameEnum.hasMoreElements()) {
			String messageName = messagesNameEnum.nextElement();
			if (messageName.startsWith(prefix)) {
				m.put(messageName, messages.getString(messageName));
			}
		}
		return m;
	}

	/**
	 * Return messages from locale chosen starting with a specified string.
	 * 
	 * @param prefix
	 *            the string to use to test the messages name
	 * @return the map with the corresponding key / value pairs
	 */
	public static Map<String, String> getKeysStartingWith(String prefix) {
		return getInstance().getKeysStarting(prefix);
	}
    
}
