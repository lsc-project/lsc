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
package org.lsc.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.lsc.Configuration;
import org.lsc.beans.BeanComparator;

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
public final class DaoConfig
{
	/** Log4J local logger. */
	private static final Logger LOGGER = Logger.getLogger(BeanComparator.class);

	/** The base name of the iBatis configuration file */
	public static final String IBATIS_SQLMAP_CONFIGURATION_FILENAME = "sql-map-config.xml";

	/** The localization of the iBatis configuration file. */
	public static final String IBATIS_SQLMAP_CONFIGURATION_FILE = "org/lsc/persistence/xml/"
		+ IBATIS_SQLMAP_CONFIGURATION_FILENAME;

	/** iBatis sqlMap configuration file directory for sqlMap files */
	public static final String IBATIS_SQLMAP_FILES_DIRNAME = "sql-map-config.d";

	/**
	 * SqlMapClient instances are thread safe, so you only need one. In this
	 * case, we'll use a static singleton. So sue me. ;-)
	 */
	private static SqlMapClient sqlMapper;

	/** Tool class. */
	private DaoConfig()
	{
	}

	/**
	 * Return the SQLMap object who manage data access.
	 * 
	 * @return the data accessor manager
	 */
	public static SqlMapClient getSqlMapClient()
	{
		if (sqlMapper == null)
		{
			try
			{
				Reader reader = null;

				// Test if we have a IBATIS_SQLMAP_CONFIGURATION_FILENAME file in the global config dir.
				// This test is for backwards compatibility since the IBATIS_SQLMAP_CONFIGURATION_FILENAME
				// file always used to be in a JAR file. It should be removed in the future.
				File configFile = new File(Configuration.getConfigurationDirectory() + IBATIS_SQLMAP_CONFIGURATION_FILENAME);
				if (configFile.exists())
				{
					// read the file from the configuration directory
					String pathToFile = configFile.toURI().toURL().toString();
					LOGGER.debug("Reading " + IBATIS_SQLMAP_CONFIGURATION_FILENAME + " from " + pathToFile);
					reader = Resources.getUrlAsReader(pathToFile);
				}
				else
				{
					// read the file from the classpath (it's in a JAR file)
					reader = Resources.getResourceAsReader(IBATIS_SQLMAP_CONFIGURATION_FILE);
				}

				Properties props = new Properties();
				
				// read the database configuration file to pass to sql-map-config XML file
				// this is maintained for backwards compatibility, although the database.properties file no longer exists
				try
				{
					props.putAll(Configuration.getPropertiesFromFileInConfigDir(Configuration.DATABASE_PROPERTIES_FILENAME));
				}
				catch (FileNotFoundException e) {} 
				// ignore this, it probably just means that we're not using database.properties file anymore
				
				// add the database configuration properties from lsc.properties
				props.putAll(Configuration.getAsProperties("src.database"));
				
				// add the configuration directory to properties so that sql-map-config can use relative paths
				props.put("lsc.config", Configuration.getConfigurationDirectory());
				
				sqlMapper = SqlMapClientBuilder.buildSqlMapClient(reader, props);

				// clean up
				reader.close();
			}
			catch (IOException e)
			{
				throw new RuntimeException("Something bad happened while building the SqlMapClient instance." + e, e);
			}
		}

		return sqlMapper;
	}
}
