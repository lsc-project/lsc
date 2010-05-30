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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import org.lsc.utils.output.CsvLayout;
import org.lsc.utils.output.LdifLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ldap Synchronization Connector Configuration.
 * 
 * @author Sebastien Bahloul <seb@lsc-project.org>
 * @author Remy-Christophe Schermesser <rcs@lsc-project.org>
 */
public class Configuration {

	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

	/** Filename of the <code>lsc.properties</code> file */
	public static final String PROPERTIES_FILENAME = "lsc.properties";

	/** Filename of the <code>database.properties</code> file */
	public static final String DATABASE_PROPERTIES_FILENAME = "database.properties";

	/** Default location for configuration filename */
	public static String location = PROPERTIES_FILENAME;

	/** Flag to detect if logging is configured or not yet */
	private static boolean loggingSetup = false;
	
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

	/** Prefix for tasks configuration elements in lsc.properties */
	public static final String LSC_TASKS_PREFIX = "lsc.tasks";
	
	/** Prefix for syncoptions configuration elements in lsc.properties */
	public static final String LSC_SYNCOPTIONS_PREFIX = "lsc.syncoptions";
	
	// Default constructor.
	protected Configuration() {
	}

	/**
	 * Get data source connection properties.
	 * 
	 * @return the data source connection properties
	 */
	public static Properties getSrcProperties() {
		Properties srcProps = getAsProperties("src");
		if (srcProps.size() > 0) {
			checkLdapProperties(srcProps);
		}
		return srcProps;
	}

	private static void checkLdapProperties(Properties props) {
		// sanity check
		String contextDn = null;
		String ldapUrl = (String) props.get("java.naming.provider.url");

		if (ldapUrl == null) {
			throw new RuntimeException("No LDAP provider url specified. Aborting.");
		}
		try {
			contextDn = new LDAPURL(ldapUrl).getBaseDN().toString();
		} catch (LDAPException e) {
			throw new RuntimeException("Error getting context DN from LDAP provider url", e);
		}

		if (contextDn == null || contextDn.length() == 0) {
			throw new RuntimeException("No context DN specified in LDAP provider url (" + props.get("java.naming.provider.url") + "). Aborting.");
		}
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
		checkLdapProperties(dst);
		return dst;
	}

	public static Properties getCsvProperties() {
		return getAsProperties("lsc.output.csv");
	}

	public static Properties getLdifProperties() {
		return getAsProperties("lsc.output.ldif");
	}

	/**
	 * Create a Properties object that is a subset of this configuration.
	 * If there are no properties matching the prefix, an empty Properties
	 * object is returned.
	 * 
	 * @param prefix
	 *            The prefix used to select the properties.
	 * @return Properties object with the requests properties without the prefix
	 */
	public static Properties getAsProperties(final String prefix) {
		org.apache.commons.configuration.Configuration conf = getConfiguration().subset(prefix);
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
	
	public static Properties getPropertiesSubset(final Properties originalProperties, String prefix) {
		if (originalProperties == null) {
			return null;
		}
		Properties result = new Properties();
		for (Object propertyName: originalProperties.keySet()) {
			String propertyNameStr = (String) propertyName;
			if(propertyNameStr.startsWith(prefix + ".")) {
				String newPropertyName = propertyNameStr.substring(propertyNameStr.indexOf(prefix.length()+1));
				result.put(newPropertyName, originalProperties.getProperty(propertyNameStr));
			}
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
		configurationLocation = cleanup(configurationLocation);
		location = appendDirSeparator(configurationLocation);
		
		// check the new location actually exists
		if (! new File(location).exists()) {
			// no point logging anything here, the logging configuration can't be read if we don't have a config location
			throw new RuntimeException("Configuration location doesn't exist! (" + location + "). Aborting.");
		}
	}

	private static String appendDirSeparator(String path) {
		if (!path.endsWith(getSeparator())) {
			return path + getSeparator();
		}
		return path;
	}

	private static String cleanup(String path) {
		String ret = path.trim();
		if (ret.charAt(0) == '\'' && ret.charAt(ret.length() - 1) == '\'') {
			ret = ret.substring(1, ret.length() - 1);
		}
		return ret;
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
		} else {
			String errorMessage = "Could not understand where the configuration is! Try using -f option. Aborting.";
			
			/* Backward compatibility: if no directory was specified,
			 * we must find the directory where configuration files are.
			 * This is in the classpath, so we look for "lsc.properties"
			 * in the classpath and use that directory.
			 */
			URL propertiesURL = Configuration.class.getClassLoader().getResource(PROPERTIES_FILENAME);
			if (propertiesURL == null) {
				throw new RuntimeException(errorMessage);
			}

			try {
				// convert the URL to a URI to reverse any character encoding (" " -> "%20" for example)
				ret = appendDirSeparator(new File(propertiesURL.toURI().getPath()).getParent());
			} catch (URISyntaxException e) {
				throw new RuntimeException(errorMessage, e);
			}
		}
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
				throw new RuntimeException("Unable to find '" + url + "' file", e);
			} catch (MalformedURLException e) {
				throw new RuntimeException("Unable to find file", e);
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
	 * @throws ConfigurationException If an error occurs while setting the properties
	 */
	static void setConfiguration(URL url) throws ConfigurationException {
		LOGGER.debug("Loading configuration url: {}", url);
		config = new PropertiesConfiguration(url);
		config.getKeys();
	}

	/**
	 * Look for a configuration file in the classpath and add it.
	 * 
	 * @param url
	 *            the url of the configuration file to load
	 * @throws ConfigurationException If an error occurs while setting the properties
	 */
	static void addConfiguration(URL url) throws ConfigurationException {
		LOGGER.debug("Adding configuration: {}", url);
		PropertiesConfiguration configTmp = new PropertiesConfiguration(url);
		Iterator<?> configKeys = configTmp.getKeys();
		while (configKeys.hasNext()) {
			String key = (String) configKeys.next();
			String value = (String) configTmp.getProperty(key);
			if (config.containsKey(key)) {
				LOGGER.warn("Property {} ({}) in file {} override main value ({})",
								new Object[] { key, configTmp.getProperty(key), url, config.getProperty(key) });
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
		InputStream st = new FileInputStream(propertiesFile);
		try {
			props.load(st);
		} finally {
			st.close();
		}
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

	public static String getSeparator() {
		return System.getProperty("file.separator");
	}

	/**
	 * Set up configuration for the given location, including logback.
	 * IMPORTANT: don't log ANYTHING before calling this method!
	 * @param configurationLocation
	 */
	public static void setUp(String configurationLocation) {
		if (configurationLocation != null) {
			Configuration.setLocation(configurationLocation);
		}

		// setup LogBack
		// first, reset the configuration because LogBack automatically loads it from xml
		// while this may be the Java way, it's not our way, we like real text files, not JARs.
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset(); //reset configuration

		String logBackXMLPropertiesFile = Configuration.getConfigurationDirectory() + "logback.xml";

		try {
			configurator.doConfigure(logBackXMLPropertiesFile);
			if(!getCsvProperties().isEmpty()) {
				setUpCsvLogging(context);
			}

			if(!getLdifProperties().isEmpty()) {
				setUpLdifLogging(context);
			}
		} catch (JoranException je) {
			System.err.println("Can not find LogBack configuration file");
		}
		
		// Logging configured
		setLoggingSetup(true);

		// WARNING: don't log anything before HERE!
		LOGGER.debug("Reading configuration from {}", Configuration.getConfigurationDirectory());
	}
	
	/**
	 * <P>Helper method to check that a String read in from a property is not empty or null</P>
	 * 
	 * @param propertyName Name of the property key as read from lsc.properties
	 * @param propertyValue Value read from the configuration
	 * @param location Where this property is read from, to display a meaningful error message (example: class name, task name, etc)
	 * @throws RuntimeException If the property is null or empty.
	 */
	public static void assertPropertyNotEmpty(String propertyName, String propertyValue, String location) throws RuntimeException {
		if (propertyValue == null || propertyValue.length() == 0	) {
			throw new RuntimeException("No " + propertyName + " property specified in " + location + ". Aborting.");
		}
	}

	/**
	 * Set the flag to determine if logging is configured or not yet
	 * 
	 * @param loggingSetup Is logging setup yet?
	 */
	public static void setLoggingSetup(boolean loggingSetup) {
		Configuration.loggingSetup = loggingSetup;
	}

	/**
	 * Get the flag to determine if logging is configured or not yet
	 * 
	 * @return boolean loggingSetup
	 */
	public static boolean isLoggingSetup() {
		return loggingSetup;
	}

	protected static void setUpCsvLogging(LoggerContext context) {
		Properties properties = getLdifProperties();

		FileAppender appender = new FileAppender();
		appender.setName("csv");
		appender.setAppend(Boolean.parseBoolean(properties.getProperty("append", "false")));
		appender.setFile(properties.getProperty("file"));
		appender.setContext(context);

		CsvLayout csvLayout = new CsvLayout();
		csvLayout.setLogOperations(properties.getProperty("logOperations"));
		csvLayout.setAttrs(properties.getProperty("attrs"));
		csvLayout.setSeparator(properties.getProperty("separator", ";"));
		csvLayout.setOutputHeader(Boolean.parseBoolean(properties.getProperty("outputHeader", "true")));
		csvLayout.setTaskNames(properties.getProperty("taskNames"));
		csvLayout.setContext(context);
		csvLayout.start();

		appender.setLayout(csvLayout);
		appender.start();
		ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(appender);
	}

	protected static void setUpLdifLogging(LoggerContext context) {
		Properties properties = getLdifProperties();

		FileAppender appender = new FileAppender();
		appender.setName("ldif");
		appender.setAppend(Boolean.parseBoolean(properties.getProperty("append", "false")));
		appender.setFile(properties.getProperty("file"));
		appender.setContext(context);

		LdifLayout ldifLayout = new LdifLayout();
		ldifLayout.setLogOperations(properties.getProperty("logOperations"));
		ldifLayout.setOnlyLdif(Boolean.parseBoolean(properties.getProperty("onlyLdif", "false")));
		ldifLayout.setContext(context);
		ldifLayout.start();

		appender.setLayout(ldifLayout);
		appender.start();
		ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(appender);
	}
	
}
