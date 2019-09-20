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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.lsc.configuration.CsvAuditType;
import org.lsc.configuration.JaxbXmlConfigurationHelper;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdifAuditType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.PropertiesConfigurationHelper;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscException;
import org.lsc.jndi.JndiServices;
import org.lsc.utils.output.CsvLayout;
import org.lsc.utils.output.LdifLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Ldap Synchronization Connector Configuration.
 * 
 * This class was initially handling only properties configuration types
 * but with XML evolution, LscConfiguration is now the central point. This point
 * is maintained for historic compatibility issues 
 * 
 * It contains deprecated properties based methods to allow smooth updates of 
 * plugins and external methods components 
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
	public static String location;

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

	/** The maximum limit of data that can be synchronized by a synchronous task */
	public static final int MAX_CONCURRENT_SYNCHRONIZED = 100000;
	
	// Default constructor.
	protected Configuration() {
	}

	/**
	 * Get data source connection properties.
	 * 
	 * @return the data source connection properties
	 * @throws LscConfigurationException 
	 */
	@Deprecated
	public static Properties getSrcProperties() throws LscConfigurationException {
		return JndiServices.getLdapProperties((org.lsc.configuration.LdapConnectionType) LscConfiguration.getConnection("src-ldap"));
	}

	/**
	 * Get data destination connection properties.
	 * 
	 * @return the data destination connection properties
	 * @throws LscConfigurationException 
	 */
	@Deprecated
	public static Properties getDstProperties() throws LscConfigurationException {
		return JndiServices.getLdapProperties((LdapConnectionType) LscConfiguration.getConnection("dst-ldap"));
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
	@Deprecated
	public static Properties getAsProperties(final String prefix) {
		return getAsProperties(getConfiguration(), prefix);
	}

	@Deprecated
	public static Properties getAsProperties(final String filename, final String prefix) {
		return getAsProperties(getConfiguration(filename), prefix);
	}

	@Deprecated
	public static Properties getAsProperties(PropertiesConfiguration propsConf, final String prefix) {
		org.apache.commons.configuration.Configuration conf =  propsConf.subset(prefix);
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
				String newPropertyName = propertyNameStr.substring(prefix.length()+1);
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public static String getString(final String key, String defaultValue) {
		// beware of List problems, so get the object and convert it to a string
		Object o = getConfiguration().getProperty(key);
		if (o == null) {
			return defaultValue;
		}
		return asString(o);
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
		if(location == null) {
			setUp();
		}
		return (location != null ? new File(location).getAbsolutePath() + File.separator : "");
	}

	/**
	 * Helper method to do lazy default configuration. This was mainly done to
	 * make this class easily testable.
	 * 
	 * @return the configuration instance used by this class.
	 */
	@Deprecated
	protected static PropertiesConfiguration getConfiguration() {
		return getConfiguration(new File(location, PROPERTIES_FILENAME).getAbsolutePath());
	}

	@Deprecated
	protected static PropertiesConfiguration getConfiguration(String filename) {
		if (config == null) {
			URL url = null;
			try {
				url = new File(filename).toURI().toURL();
				LOGGER.debug("Loading configuration url: {}", url);
				config = new PropertiesConfiguration(url);
				config.getKeys();

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
	@Deprecated
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
	@Deprecated
	public static Properties getPropertiesFromFileInConfigDir(String fileName) throws FileNotFoundException, IOException {
		return getPropertiesFromFile(Configuration.getConfigurationDirectory() + fileName);
	}

	/**
	 * Set up configuration for the given location, including logback.
	 * MUST NEVER BE CALLED DIRECTLY : ONLY USED BY LscConfiguration static code instantiation
	 * IMPORTANT: don't log ANYTHING before calling this method!
	 */
	public static void setUp() {
		if(LscConfiguration.isInitialized()) {
			// Nothing to do there : default configuration must only be used if LSC is not already configured
			return;
		}
		try {
			if(new File(System.getProperty("LSC_HOME"), "etc").isDirectory() && new File(System.getProperty("LSC_HOME"), "etc/lsc.xml").exists()) {
				Configuration.setUp(new File(System.getProperty("LSC_HOME"), "etc").getAbsolutePath(), false);
			} else {
				// Silently bypass mis-configuration because if setUp(String) is called, this method is run first, probably with bad default settings
				if(Configuration.class.getClassLoader().getResource("etc") != null) {
					Configuration.setUp(Configuration.class.getClassLoader().getResource("etc").getPath(), false);
				}
			}
		} catch (LscException le) {
			System.err.println("Something strange appened: " + le.getMessage());// Silently forget le
		}
	}
	
	/**
	 * Set up configuration for the given location, including logback.
	 * IMPORTANT: don't log ANYTHING before calling this method!
	 * @param lscConfigurationPath
	 * @throws LscException 
	 */
	public static void setUp(String lscConfigurationPath) throws LscException {
		setUp(lscConfigurationPath, true);
	}
	
	/**
	 * Set up configuration for the given location, including logback.
	 * IMPORTANT: don't log ANYTHING before calling this method!
	 * @param lscConfigurationPath
	 * @throws LscException 
	 */
	public static void setUp(String lscConfigurationPath, boolean validate) throws LscException {
		String message = null;
		if(lscConfigurationPath == null 
				|| ! new File(lscConfigurationPath).isDirectory()
				|| ( ! new File(lscConfigurationPath, JaxbXmlConfigurationHelper.LSC_CONF_XML).isFile()
						&& ! new File(lscConfigurationPath, PROPERTIES_FILENAME).isFile())) {
			message = "Defined configuration location (" + lscConfigurationPath + ") points to a non existing LSC configured instance. " +
				"LSC configuration loading will fail !";
			LOGGER.error(message);
			throw new RuntimeException(message);
		}
		try {
			location = cleanup(lscConfigurationPath);
			if(!LscConfiguration.isInitialized()) {
				File xml = new File(location, JaxbXmlConfigurationHelper.LSC_CONF_XML);
				File properties = new File(location, Configuration.PROPERTIES_FILENAME);
				if(xml.exists() && xml.isFile()) {
					LscConfiguration.loadFromInstance(new JaxbXmlConfigurationHelper().getConfiguration(xml.toString(), System.getenv()));
				} else if (properties.exists() && properties.isFile()) {
					LOGGER.warn("LSC configuration loaded from old properties file format !");
					PropertiesConfigurationHelper.loadConfigurationFrom(properties.getAbsolutePath());
				} else {
		            message = "Unable to load configuration configuration inside the directory: " + location;
		            LOGGER.error(message);
		            return;
				}
			} else {
				LOGGER.error("LSC already configured. Unable to load new parameters ...");
			}
		} catch (LscConfigurationException e) {
			message = "Unable to load configuration (" + e + ")";
			LOGGER.error(message, e);
			return;
		}

		// setup LogBack
		// first, reset the configuration because LogBack automatically loads it from xml
		// while this may be the Java way, it's not our way, we like real text files, not JARs.
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset(); //reset configuration


		String logBackXMLPropertiesFile = new File(Configuration.getConfigurationDirectory(), "logback.xml").getAbsolutePath();
		try {
			configurator.doConfigure(logBackXMLPropertiesFile);
			LOGGER.info("Logging configuration successfully loaded from " + logBackXMLPropertiesFile + " ");
			if(LscConfiguration.getAudit("CSV") != null) {
				setUpCsvLogging(context);
			}

			if(LscConfiguration.getAudit("LDIF") != null) {
				setUpLdifLogging(context);
			}
		} catch (JoranException je) {
			System.err.println("Cannot find logging configuration file ("+logBackXMLPropertiesFile+") !");
		}
		
		// Logging configured
		setLoggingSetup(true);

		if(validate) {
			LscConfiguration.getInstance().validate();
		}
		
		// WARNING: don't log anything before HERE!
		LOGGER.info("LSC configuration successfully loaded from {}", Configuration.getConfigurationDirectory());
	}
	
	/**
	 * <P>Helper method to check that a String read in from a property is not empty or null</P>
	 * 
	 * @param propertyName Name of the property key as read from lsc.properties
	 * @param propertyValue Value read from the configuration
	 * @param location Where this property is read from, to display a meaningful error message (example: class name, task name, etc)
	 * @throws RuntimeException If the property is null or empty.
	 */
	public static void assertPropertyNotEmpty(String propertyName, String propertyValue, String location) throws LscConfigurationException {
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
		CsvAuditType audit = (CsvAuditType) LscConfiguration.getAudit("CSV");

		FileAppender<ILoggingEvent> appender = new FileAppender<ILoggingEvent>();
		appender.setName(audit.getName());
		appender.setAppend(audit.isAppend());
		appender.setFile(audit.getFile());
		appender.setContext(context);

		CsvLayout csvLayout = new CsvLayout();
		csvLayout.setLogOperations(audit.getOperations());
		csvLayout.setAttrs(audit.getDatasets());
		csvLayout.setSeparator(audit.getSeparator());
		csvLayout.setOutputHeader(audit.isOutputHeader());
		if(audit.getTaskNames() != null && audit.getTaskNames().getString() != null) {
			csvLayout.setTaskNames(audit.getTaskNames().getString().toArray(new String[audit.getTaskNames().getString().size()]));
		}
		csvLayout.setContext(context);
		csvLayout.start();

		appender.setLayout(csvLayout);
		appender.start();
		ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(appender);
	}

	protected static void setUpLdifLogging(LoggerContext context) {
		LdifAuditType audit = (LdifAuditType) LscConfiguration.getAudit("LDIF");

		FileAppender<ILoggingEvent> appender = new FileAppender<ILoggingEvent>();
		appender.setName(audit.getName());
		appender.setAppend(audit.isAppend());
		appender.setFile(audit.getFile());
		appender.setContext(context);

		LdifLayout ldifLayout = new LdifLayout();
		ldifLayout.setLogOperations(audit.getOperations());
		if(audit.isLogOnlyLdif() != null) {
			ldifLayout.setOnlyLdif(audit.isLogOnlyLdif());
		}
		ldifLayout.setContext(context);
		ldifLayout.start();

		appender.setLayout(ldifLayout);
		appender.start();
		ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		rootLogger.addAppender(appender);
	}
	
}
