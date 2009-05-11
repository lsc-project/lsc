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
package org.lsc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ibatis.sqlmap.client.SqlMapClientBuilder;

/**
 * Ldap Synchronization Connector Configuration.
 * 
 * @author Sebastien Bahloul <seb@lsc-project.org>
 * @author Remy-Christophe Schermesser <rcs@lsc-project.org>
 */
public class Configuration {

	// Logger
	private static final Logger LOGGER = Logger.getLogger(Configuration.class);

	// Filename of the <code>lsc.properties</code>
	public static final String PROPERTIES_FILENAME = "lsc.properties";

	// Filename of the <code>database.properties</code>
	public static final String DATABASE_PROPERTIES_FILENAME = "database.properties";
	
	/** Default location for configuration filename */
	public static String location = PROPERTIES_FILENAME;

	// People DN
	public static String DN_PEOPLE = "ou=People";

	// LDAP schema DN
	public static String DN_LDAP_SCHEMA = "cn=Subschema";

	// Enhanced schema DN
	public static String DN_ENHANCED_SCHEMA = "ou=Schema,ou=System";

	// Structures DN
	public static String DN_STRUCTURES = "ou=Structures";

	// Accounts DN
	public static String DN_ACCOUNTS = "ou=Accounts";

	// objectClass for a person
	public static String OBJECTCLASS_PERSON = "inetOrgPerson";

	// objectClass for an employee
	public static String OBJECTCLASS_EMPLOYEE = "inetOrgPerson";

	/**
	 * Numbers of days between an entry is set to be deleted and its actual
	 * deletion.
	 */
	public static int DAYS_BEFORE_SUPPRESSION = 90;

	// The real LDAP base DN
	public static String DN_REAL_ROOT = "dc=lsc-project,dc=org";

	// The maximum user identifier length
	public static int UID_MAX_LENGTH = 8;

	// LSC configuration of the application
	private static PropertiesConfiguration config = null;

	// Default constructor.
	protected Configuration() {
	}

	/**
	 * Get data source connection properties.
	 * 
	 * @return the data source connection properties
	 */
	public static Properties getSrcProperties() {
		return getAsProperties("src");
	}

	/**
	 * Get data destination connection properties.
	 * 
	 * @return the data destination connection properties
	 */
	public static Properties getDstProperties() {
		Properties dst = getAsProperties("dst");
		if (dst == null || dst.size() == 0) {
			dst = getAsProperties("ldap");
		}
		return dst;
	}

	public static List<String> getListFromString(String propertyValue) {
		List<String> result = new ArrayList<String>();
		if (propertyValue != null) {
			StringTokenizer st = new StringTokenizer(propertyValue, " ");
			for (int i = 0; st.hasMoreTokens(); i++) {
				result.add(st.nextToken().toLowerCase());
			}
		}
		return result;
	}

	/**
	 * Create a Properties object that is a subset of this configuration.
	 * 
	 * @param prefix
	 *            The prefix used to select the properties.
	 */
	public static Properties getAsProperties(final String prefix) {
		org.apache.commons.configuration.Configuration conf = getConfiguration()
				.subset(prefix);
		if (conf == null) {
			return null;
		}
		Iterator<?> it = conf.getKeys();
		Properties result = new Properties();
		String key = null;
		Object value = null;
		while (it.hasNext()) {
			key = (String) it.next();
			value = asString(conf.getProperty(key));
			result.put(key, value);
		}
		return result;
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
	 * Get a string associated with the given property key
	 * 
	 * @param key
	 *            The property key.
	 * @return The associated string.
	 */
	public static String getString(final String key) {
		// beware of List problems, so get the object and convert it to a string
		return asString(getConfiguration().getProperty(key));
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
	 * Set the configuration properties location
	 * 
	 * @param configurationLocation
	 *            the user defined location
	 */
	public static void setLocation(String configurationLocation) {
		location = appendDirSeperator(configurationLocation);
	}
	
	private static String appendDirSeperator(String path) {
		String seperator = System.getProperty("file.separator");
		if (!path.endsWith(seperator)) {
			return path + seperator;
		}
		return path;
	}

	/**
	 * Get the path to the directory where configuration files
	 * are stored, with a "/" at the end (or "\" on Windows).
	 * 
	 * All configuration files MUST be read from this directory.
	 * 
	 * If no directory is specified when launching LSC, this returns 
	 * the user's current directory.
	 * @return Path to configuration directory
	 */
	public static String getConfigurationDirectory() {
		String ret;
		
		if (new File(location).isDirectory()) {
			ret = location;
		}
		
		/* Backward compatibility: if no directory was specified,
		 * we must find the directory where configuration files are.
		 * This is in the classpath, so we look for "lsc.properties"
		 * in the classpath and use that directory.
		 */
		URL propertiesURL = Configuration.class.getClassLoader().getResource(PROPERTIES_FILENAME);
		ret = appendDirSeperator(new File(propertiesURL.getPath()).getParent());
		
		LOGGER.debug("Configuration directory is " + ret);
		return ret;
	}
	
	/**
	 * Helper method to do lazy default configuration. This was mainly done to
	 * make this class easily testable.
	 * 
	 * @return the configuration instance used by this class.
	 */
	protected static PropertiesConfiguration getConfiguration() {
		if (config == null) {
			URL url = null;
			try {
				url = new File(getConfigurationDirectory(), PROPERTIES_FILENAME).toURI().toURL();
				setConfiguration(url);

				DN_PEOPLE = Configuration.getString("dn.people", DN_PEOPLE);
				DN_LDAP_SCHEMA = Configuration.getString("dn.ldap_schema",
						DN_LDAP_SCHEMA);
				DN_ENHANCED_SCHEMA = Configuration.getString("dn.ldap_schema",
						DN_ENHANCED_SCHEMA);
				DN_STRUCTURES = Configuration.getString("dn.structures",
						DN_STRUCTURES);
				DN_ACCOUNTS = Configuration.getString("dn.accounts",
						DN_STRUCTURES);
				OBJECTCLASS_PERSON = Configuration.getString(
						"objectclass.person", OBJECTCLASS_PERSON);
				OBJECTCLASS_EMPLOYEE = Configuration.getString(
						"objectclass.employee", OBJECTCLASS_EMPLOYEE);
				DAYS_BEFORE_SUPPRESSION = Configuration.getInt(
						"suppression.MARQUAGE_NOMBRE_DE_JOURS", DAYS_BEFORE_SUPPRESSION);
				DN_REAL_ROOT = Configuration.getString("dn.real_root",
						DN_REAL_ROOT);
				UID_MAX_LENGTH = Configuration.getInt("uid.maxlength", UID_MAX_LENGTH);

			} catch (ConfigurationException e) {
				LOGGER.error(e, e);
				throw new ExceptionInInitializerError("Unable to find '" + url
						+ "' file. (" + e + ")");
			} catch (MalformedURLException e) {
				LOGGER.error(e, e);
				throw new ExceptionInInitializerError("Unable to find '" + url
						+ "' file. (" + e + ")");
			}
		}
		return config;
	}

	/**
	 * commons-configuration automatically parse a comma separated value in key
	 * and return a list, that's not what we want here, we need to conserve the
	 * commas. An appropriate method should be added soon to the API.
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
			value = StringUtils.join(list.iterator(), ",");
		}
		return (String) value;
	}

	/**
	 * Look for a configuration file in the classpath and set it. This is mainly
	 * a hook for testing purposes.
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
		while (configKeys.hasNext()) {
			String key = (String) configKeys.next();
			String value = (String) configTmp.getProperty(key);
			if (config.containsKey(key)) {
				LOGGER.warn("Property " + key + " ("
						+ configTmp.getProperty(key) + ") in file " + url
						+ " override main value (" + config.getProperty(key)
						+ ")");
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
		while (propsEnum.hasMoreElements()) {
			String key = (String) propsEnum.nextElement();
			conf.setProperty((prefix != null ? prefix + "." : "") + key, props.getProperty(key));
		}
		conf.save();
	}
	
	/**
	 * Helper method to read a file from the filesystem and return it as Properties.
	 * 
	 * @param pathToFile Absolute filename on the filesystem to read.
	 * @return Properties from the file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Properties getPropertiesFromFile(String pathToFile) throws FileNotFoundException, IOException {
		File propertiesFile = new File(pathToFile);
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));
		return props;
	}
	
	/**
	 * Helper method to read a file from the configuration directory and return it as Properties.

	 * @param fileName Filename relative to the configuration directory.
	 * @return Properties from the file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Properties getPropertiesFromFileInConfigDir(String fileName) throws FileNotFoundException, IOException {
		return getPropertiesFromFile(Configuration.getConfigurationDirectory() + fileName);
	}
}
