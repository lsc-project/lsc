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

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public abstract class Configuration {

	/**
	 * Logger of this class
	 */
	private static final Logger LOGGER = Logger.getLogger(Configuration.class);

	/**
	 * Filename of the <code>lsc.properties</code>
	 */
	public final static String PROPERTIES_FILENAME = "lsc.properties";

	/**
	 * real root
	 */
	public static String DN_REAL_ROOT;// = Configuration.getString("dn.real-root", "dc=lsc,dc=org");

	public static String PROPERTIES_DIRECTORY = "lsc.d";

	/**
	 * LSC configuration of the application
	 */
	private static org.apache.commons.configuration.PropertiesConfiguration config = null;

	/**
	 * Utility classes should not have a public or default constructor.
	 */
	protected Configuration() {
	}

	/**
	 * Get the LDAP connection properties.
	 * 
	 * @return The LDAP connection properties.
	 */
	public static Properties getLdapProperties() {
		return getAsProperties("ldap");
	}

	/**
	 * Get the ressources connection properties.
	 * 
	 * @return The ressources connection properties.
	 */
	public static Properties getRessourcesProperties() {
		return getAsProperties("ressources");
	}

	/**
	 * Create a Properties object that is a subset of this configuration.
	 * 
	 * @param prefix
	 *            The prefix used to select the properties.
	 */
	public static Properties getAsProperties(final String prefix) {
		org.apache.commons.configuration.Configuration conf = getConfiguration().subset(prefix);
		if (conf == null) {
			return null;
		}
		Iterator<?> it = conf.getKeys();
		Properties result = new Properties();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object value = conf.getProperty(key);
			value = asString(value);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Helper method to do lazy default configuration. This was mainly done to make this class easily testable.
	 * 
	 * @return the configuration instance used by this class.
	 */
	protected static org.apache.commons.configuration.PropertiesConfiguration getConfiguration() {
		if (config == null) {
			try {
				URL url = Configuration.class.getClassLoader().getResource(PROPERTIES_FILENAME);
				if (url == null) {
					url = new java.io.File(PROPERTIES_FILENAME).toURL();
				}
				setConfiguration(url);
				URL dirUrl = Configuration.class.getClassLoader().getResource(PROPERTIES_DIRECTORY);
				if(dirUrl != null) {
					File confDir = new File(dirUrl.toURI());
					if (confDir == null) {
						confDir = new java.io.File(PROPERTIES_DIRECTORY);
					}
					if(confDir.isDirectory()) {
						FilenameFilter ff = new FilenameFilter() {
							public boolean accept(File arg0, String arg1) {
								// TODO Auto-generated method stub
								return false;
							}
						};
						File[] files = confDir.listFiles(ff);
						for (int i = 0; i < files.length ; i++) {
							addConfiguration(files[i].toURL());
						}
					}
				}
			} catch (ConfigurationException e) {
				LOGGER.error(e, e);
				throw new ExceptionInInitializerError("Unable to find '" + PROPERTIES_FILENAME + "' file. ("
						+ e + ")");
			} catch (MalformedURLException e) {
				LOGGER.error(e, e);
				throw new ExceptionInInitializerError("Unable to find '" + PROPERTIES_FILENAME + "' file. ("
						+ e + ")");
			} catch (URISyntaxException e) {
				LOGGER.error(e, e);
				throw new ExceptionInInitializerError("Unable to find '" + PROPERTIES_FILENAME + "' file. ("
						+ e + ")");
			}
		}
		return config;
	}

	/**
	 * Get a int associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @return The associated int.
	 */
	public static int getInt(final String key) {
		return getConfiguration().getInt(key);
	}

	/**
	 * Get a int associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @param defaultValue
	 *            The default value.
	 * @return The associated int.
	 */
	public static int getInt(final String key, int defaultValue) {
		return getConfiguration().getInt(key, defaultValue);
	}

	/**
	 * Get a boolean associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @return The associated boolean.
	 */
	public static boolean getBoolean(final String key) {
		return getConfiguration().getBoolean(key);
	}

	/**
	 * Get a boolean associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @param defaultValue
	 *            The default value.
	 * @return The associated boolean.
	 */
	public static boolean getBoolean(final String key, boolean defaultValue) {
		return getConfiguration().getBoolean(key, defaultValue);
	}

	/**
	 * Get a string associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @return The associated string.
	 */
	public static String getString(final String key) {
		// beware of List problems, so get the object and convert it to a string
		Object o = getConfiguration().getProperty(key);
		return asString(o);
	}

	/**
	 * Get a string associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @param defaultValue
	 *            The default value.
	 * @return The associated string.
	 */
	public static String getString(final String key, String defaultValue) {
		// beware of List problems, so get the object and convert it to a string
		Object o = getConfiguration().getProperty(key);
		if (o == null) {
			return defaultValue;
		}
		return asString(o);
	}

	/**
	 * commons-configuration automatically parse a comma separated value in key and return a list, that's not what we
	 * want here, we need to conserve the commas. An appropriate method should be added soon to the API.
	 * 
	 * @param value
	 *            the value to convert, it should be either a String or a List
	 * @return the object as a string.
	 * @throws ClassCastException
	 *             if the object is not a string nor a list.
	 */
	private static String asString(Object value) {
		if (value instanceof List) {
			List<?> list = (List<?>) value;
			value = StringUtils.join(list.iterator(), ',');
		}
		return (String) value;
	}

	/**
	 * Look for a configuration file in the classpath and set it. This is mainly a hook for testing purposes.
	 * 
	 * @param url
	 *            the url of the configuration file to load
	 */
	static void setConfiguration(URL url) throws ConfigurationException {
		LOGGER.debug("Loading configuration url : " + url);
		config = new PropertiesConfiguration(url);
		config.getKeys();
	}

	/**
	 * Look for a configuration file in the classpath and add it.
	 * 
	 * @param url
	 *            the url of the configuration file to load
	 */
	static void addConfiguration(URL url) throws ConfigurationException {
        LOGGER.debug("Adding configuration : " + url);
        PropertiesConfiguration configTmp = new PropertiesConfiguration(url);
        Iterator<?> configKeys = configTmp.getKeys();
        while(configKeys.hasNext()) {
        	String key = (String)configKeys.next();
        	String value = (String) configTmp.getProperty(key);
        	if(config.containsKey(key)) {
        		LOGGER.warn("Property " + key + " (" + configTmp.getProperty(key) + ") in file " + url + " override main value (" + config.getProperty(key) + ")");
        	}
        	config.addProperty(key, value);
        }
    }

	/**
	 * Set the new properties
	 * @param prefix the prefix or null
	 * @param props the news properties
	 * @throws ConfigurationException
	 */
	public static void setProperties(String prefix, Properties props) throws ConfigurationException {
		Enumeration<Object> propsEnum = props.keys();
		PropertiesConfiguration conf = Configuration.getConfiguration(); 
		while(propsEnum.hasMoreElements()) {
			String key = (String)propsEnum.nextElement();
			conf.setProperty((prefix != null ? prefix + "." : "") + key, props.getProperty(key));
		}
		conf.save();
	}
}
