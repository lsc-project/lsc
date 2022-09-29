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
package org.lsc.persistence;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.lsc.Configuration;
import org.lsc.configuration.DatabaseConnectionType;
import org.lsc.exception.LscServiceConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

/**
 * This class is used to interface IBatis Direct Access Object engine.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public final class DaoConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(DaoConfig.class);

	/** The base name of the iBatis configuration file */
	public static final String IBATIS_SQLMAP_CONFIGURATION_FILENAME = "sql-map-config.xml";

	/** The localization of the iBatis configuration file. */
	public static final String IBATIS_SQLMAP_CONFIGURATION_FILE = "org/lsc/persistence/xml/" + IBATIS_SQLMAP_CONFIGURATION_FILENAME;

	/** iBatis sqlMap configuration file directory for sqlMap files */
	public static final String IBATIS_SQLMAP_FILES_DIRNAME = "sql-map-config.d";

	/**
	 * SqlMapClient instances are thread safe, so you only need one. In this
	 * case, we'll use a static singleton. So sue me. ;-)
	 * Fix: use a map instead, allowing to have multiple mappers at the same time
	 */
	private static Map<String, SqlMapClient> sqlMappers = new HashMap<String, SqlMapClient>();

	/** Tool class. */
	private DaoConfig() {
		
	}

	/**
	 * Return the SQLMap object who manage data access.
	 * 
	 * @return the data accessor manager
	 * @throws LscServiceConfigurationException 
	 */
	public static SqlMapClient getSqlMapClient(Properties databaseProps) throws LscServiceConfigurationException {
		String mapperKey = new StringBuffer()
				.append(databaseProps.get("username"))
				.append("|")
				.append(databaseProps.get("password"))
				.append("|")
				.append(databaseProps.get("url"))
				.append("|")
				.append(databaseProps.get("driver"))
				.toString();
		SqlMapClient sqlMapper = sqlMappers.get(mapperKey);
		if(sqlMapper == null) {
			try {
				Reader reader = null;

				// Test if we have a IBATIS_SQLMAP_CONFIGURATION_FILENAME file in the global config dir.
				// This test is for backwards compatibility since the IBATIS_SQLMAP_CONFIGURATION_FILENAME
				// file always used to be in a JAR file. It should be removed in the future.
				File configFile = new File(Configuration.getConfigurationDirectory(), IBATIS_SQLMAP_CONFIGURATION_FILENAME);
				if (configFile.exists()) {
					// read the file from the configuration directory
					String pathToFile = configFile.toURI().toURL().toString();
					LOGGER.debug("Reading {} from {}", IBATIS_SQLMAP_CONFIGURATION_FILENAME, pathToFile);
					reader = Resources.getUrlAsReader(pathToFile);
				} else {
					throw new LscServiceConfigurationException("Unable to find iBatis SQL map file in " + Configuration.getConfigurationDirectory());
				}

				// add the configuration directory to properties so that sql-map-config can use relative paths
				databaseProps.put("lsc.config", new File(Configuration.getConfigurationDirectory()).toURI().toURL().getFile());
				try {
					sqlMapper = SqlMapClientBuilder.buildSqlMapClient(reader, databaseProps);
				} catch(RuntimeException re) {
					throw new LscServiceConfigurationException("Something bad happened while building the SqlMapClient instance." + re, re);
				}

				// clean up
				reader.close();
			} catch (IOException e) {
				throw new LscServiceConfigurationException("Something bad happened while building the SqlMapClient instance." + e, e);
			}
			sqlMappers.put(mapperKey, sqlMapper);
		}
		return sqlMapper;
	}

	public static SqlMapClient getSqlMapClient(DatabaseConnectionType connection) throws LscServiceConfigurationException {
		Properties databaseProps = new Properties();
		databaseProps.put("username", connection.getUsername());
		databaseProps.put("password", connection.getPassword());
		databaseProps.put("url", connection.getUrl());
		databaseProps.put("driver", connection.getDriver());
		return getSqlMapClient(databaseProps);
	}
}
