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

import java.util.Properties;

import javax.naming.NamingException;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.AbstractGenerator;
import org.lsc.Configuration;
import org.lsc.Generator;

/**
 * Append properties on lsc.properties file.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class PropertiesGenerator extends AbstractGenerator {

	/** The local LOG4J logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesGenerator.class);

	/** The generator type. */
	private Generator.GEN_TYPE genType;

	/** This property contains the jdbc source service class name. */
	private String jdbcSrcServiceClassName;

	/**
	 * Generate the file.
	 * 
	 * @param taskName the task name
	 * @return the generation status
	 * @throws NamingException
	 *                 thrown if an directory exception is encountered while
	 *                 extending properties
	 */
	@Override
	public final boolean generate(final String taskName) throws NamingException {
		String prefix = "lsc";
		Properties props = Configuration.getAsProperties(prefix);
		props = checkAndAdd(props, "tasks", taskName);
		props = replace(props, "tasks." + taskName + ".bean", "org.lsc.beans.SimpleBean");

		// Add default DN generation configuration parameter
		props = replace(props, "tasks." + taskName + ".dn", 
				"\"uid=\" + srcBean.getAttributeValueById(\"uid\") + \",ou=People\"");
		
		switch (genType) {
			case CSV2LDAP:
			case DATABASE2LDAP:
				// the POJO object used in db2ldap sync seems to be used directly as a destination object
				props = replace(props, "tasks." + taskName + ".type", "db2ldap");
				props = replace(props, "tasks." + taskName + ".srcService", jdbcSrcServiceClassName);
				break;
			case LDAP2LDAP:
				// the POJO object used in ldap2ldap sync is to store original object from the source directory
				props = replace(props, "tasks." + taskName + ".type", "ldap2ldap");
				props = replace(props, "tasks." + taskName + ".srcService",
						"org.lsc.jndi.SimpleJndiSrcService");
				props = replaceDefaultSimpleJndiService(props, "tasks." + taskName + ".srcService", taskName);
				break;
			default:
				throw new UnsupportedOperationException("Must never be here !");
		}
		props = replace(props, "tasks." + taskName + ".dstService", "org.lsc.jndi.SimpleJndiDstService");
		props = replaceDefaultSimpleJndiService(props, "tasks." + taskName + ".dstService", taskName);
		try {
			Configuration.setProperties(prefix, props);
		} catch (ConfigurationException e) {
			LOGGER.error("Unable to save configuration file: " + e, e);
			return false;
		}
		return true;
	}

	/**
	 * This method initiate all required Jndi properties.
	 * 
	 * @param props
	 *                the properties table
	 * @param propertyPrefix
	 *                the property prefix
	 * @return the updated properties
	 */
	private Properties replaceDefaultSimpleJndiService(final Properties props,
			final String propertyPrefix, final String objectClassName) {
		Properties localProps = props;
		localProps = replace(localProps, propertyPrefix + ".baseDn", "ou=People");
		localProps = replace(localProps, propertyPrefix + ".pivotAttrs", "employeeNumber");
		localProps = replace(localProps, propertyPrefix + ".filterId", "(&(objectClass=" + objectClassName + ")(employeeNumber={employeeNumber}))");
		localProps = replace(localProps, propertyPrefix + ".filterAll", "(objectClass=" + objectClassName + ")");
		localProps = replace(localProps, propertyPrefix + ".attrs", "uid cn sn givenName mail objectClass");
		return localProps;
	}

	/**
	 * Replace the property value in the properties.
	 * 
	 * @param props
	 *                the properties table
	 * @param property
	 *                the property name
	 * @param value
	 *                the value to set
	 * @return the updated properties
	 */
	private Properties replace(final Properties props, final String property,
			final String value) {
		props.setProperty(property, value);
		return props;
	}

	/**
	 * Set the specified value to the attribute, existing or not, in the
	 * specified table.
	 * 
	 * @param props
	 *                the properties table
	 * @param property
	 *                the property name
	 * @param value
	 *                the value to set
	 * @return the updated properties
	 */
	private Properties checkAndAdd(final Properties props,
			final String property, final String value) {
		String propertyStr = props.getProperty(property);
		if (propertyStr == null) {
			propertyStr = value;
		} else {
			propertyStr = propertyStr.trim();
			if (propertyStr.indexOf(value) < 0) {
				if (propertyStr.length() > 0) {
					propertyStr += "," + value;
				} else {
					propertyStr = value;
				}
			}
		}
		props.setProperty(property, propertyStr);
		return props;
	}

	/**
	 * Unused.
	 * @return nothing
	 */
	@Override
	protected final String generateContent() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unused.
	 * @return nothing
	 */
	@Override
	public final String getGenericPackageName() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Run the CSV2SQL generator.
	 * 
	 * @param taskName
	 *                the task name
	 * @param destination
	 *                the destination directory
	 * @param genType
	 *                the Generation type
	 * @param beanClassName
	 *                the bean class name
	 * @param dstObjectClassName
	 *                the destination object class name
	 * @param srcObjectClassName
	 *                the source object class name, or null if there is none
	 * @param jdbcSrcServiceClassName
	 *                the jdbc source service name
	 * @throws NamingException
	 *                 thrown if an directory exception is encountered while
	 *                 generating the new bean
	 */
	public static void run(final String taskName, final String destination,
			final Generator.GEN_TYPE genType, final String jdbcSrcServiceClassName)
	throws NamingException {
		PropertiesGenerator pg = new PropertiesGenerator();
		pg.init(genType, jdbcSrcServiceClassName);
		pg.setDestination(destination);
		pg.generate(taskName);
	}

	/**
	 * Initialized all required parameters.
	 * 
	 * @param lgenType
	 *                the Generation type
	 * @param lbeanClassName
	 *                the bean class name
	 * @param dstObjectClassName
	 *                the destination object class name
	 * @param srcObjectClassName
	 *                the source object class name or null if there is none
	 * @param ljdbcSrcServiceClassName
	 *                the jdbc source service name
	 */
	private void init(final Generator.GEN_TYPE lgenType, final String ljdbcSrcServiceClassName) {
		genType = lgenType;
		jdbcSrcServiceClassName = ljdbcSrcServiceClassName;
	}

	/**
	 * Return a generic file name for latest generated file.
	 * @return A java generic file name.
	 */
	public final String getFileName() {
		return getStandardFileName();
	}
}
