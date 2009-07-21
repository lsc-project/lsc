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
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.lsc.AbstractGenerator;
import org.lsc.Configuration;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * In order to complete the generation process, the sql-map-config.xml file used
 * by iBatis must contains the new XML file synchronization reference. This
 * class is responsible to add it.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SqlMapXmlFileGenerator extends AbstractGenerator {
	/** The private local LOG4J logger. */
	private static final Logger LOGGER = Logger
	.getLogger(SqlMapXmlFileGenerator.class);

	/**  */
	private static final String SQL_MAP_CONFIG_FILENAME = "org/lsc/persistence/xml/sql-map-config.xml";

	/** The full filename (with destination prefix if needed). */
	private String sqlMapConfigFullFilename;

	/** The xml file resource to add to sql Map. */
	private String xmlFilename;

	/**
	 * Initialize class parameters.
	 * 
	 * @param filename
	 *                the xml filen resource to add
	 */
	protected final void init(final String filename) {
		xmlFilename = filename;
	}

	/**
	 * This method launch the xml map generation.
	 * 
	 * @param unused
	 *                not used
	 * @return the generation status
	 */
	protected final boolean generate(final String unused) {
		
		// Test if we have a IBATIS_SQLMAP_CONFIGURATION_FILENAME file in the global config dir.
		// This test is for backwards compatibility since the IBATIS_SQLMAP_CONFIGURATION_FILENAME
		// file always used to be in a JAR file. It should be removed in the future.
		try
		{
			File configFile = new File(Configuration.getConfigurationDirectory() + DaoConfig.IBATIS_SQLMAP_CONFIGURATION_FILENAME);
			if (configFile.exists())
			{
				// set sql-map-config.xml file path
				sqlMapConfigFullFilename = configFile.toURI().toURL().getPath();
				
				// adapt the generated .xml file name to be relative to the config dir
				xmlFilename = xmlFilename.substring(Configuration.getConfigurationDirectory().length());
			} else {
				// revert back to old behavior - this should be removed soon!
				LOGGER.warn("Falling back to old-style configuration files");

				if (getDestination() != null && getDestination().length() > 0) {
					sqlMapConfigFullFilename = getDestination() + getSeparator()
					+ SQL_MAP_CONFIG_FILENAME;
					// Then remove the destination from the filename
					if(xmlFilename.indexOf(getDestination()) == 0) {
						xmlFilename = xmlFilename.substring(getDestination().length()+1);
					}
				} else {
					sqlMapConfigFullFilename = SQL_MAP_CONFIG_FILENAME;
				}
			}
		}
		catch (MalformedURLException e)
		{
			throw new ExceptionInInitializerError("Error reading the IBatis SQLMap configuration file");
		}

		String content = generateContent();

		if (content != null) {
			return writeContent(content, true);
		}

		return false;
	}

	/**
	 * Generate the file content.
	 * 
	 * @return the generated content
	 */
	protected final String generateContent() {

		DOMParser parser = new DOMParser();

		// initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new StringWriter());

		try {
			File sqlMapConfigFile = new File(sqlMapConfigFullFilename);
			if (!(sqlMapConfigFile.exists())) {
				throw new RuntimeException(
						sqlMapConfigFullFilename
						+ " missing ! Please copy the default file structure before launching generation.");
			}

			// Setting the external DTD/Xschema location
			//	    parser.setFeature("http://xml.org/sax/features/validation", false);
			parser.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-external-dtd",
					false);

			// make sure any special characters are encoded in URI format
			parser.parse(sqlMapConfigFile.toURI().toASCIIString());
			Document doc = parser.getDocument();
			// docBuilder.parse();

			// Get the last node
			Node last = doc.getLastChild();

			// Create a new sqlMap entry
			Node newSqlMapEntry = doc.createElement("sqlMap");

			// Create the new sqlMap object attributes
			NamedNodeMap newSqlMapAttributes = newSqlMapEntry.getAttributes();

			// Create a new resource attribute ...
			Attr url = doc.createAttribute("url");
			// ... with the xml ibatis description filename as value
			url.setValue("file://${lsc.config}/" + xmlFilename);
			// Set the attribute in the new object attributes
			newSqlMapAttributes.setNamedItem(url);
			// Append the new node as the new last node
			try {
				last.appendChild(newSqlMapEntry);
			} catch (DOMException e) {
				LOGGER.error("Please check your xerces version. Consider upgrading to Java 6 or to Xerces 2.0 ! (" + e + ")", e);
				return null;
			}

			Transformer transformer = TransformerFactory.newInstance()
			.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//iBATIS.com//DTD SQL Map Config 2.0//EN");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.ibatis.com/dtd/sql-map-config-2.dtd");
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
		} catch (SAXException e) {
			LOGGER.fatal("Failed to parse XML file : " + e, e);

			return null;
		} catch (IOException e) {
			LOGGER.fatal("Failed to read XML file : " + e, e);

			return null;
		} catch (TransformerConfigurationException e) {
			LOGGER.fatal("Failed to read XML transformation "
					+ "configuration : " + e, e);

			return null;
		} catch (TransformerFactoryConfigurationError e) {
			LOGGER.fatal("Failed to read XML transformation "
					+ "configuration : " + e, e);

			return null;
		} catch (TransformerException e) {
			LOGGER.fatal("Failed to transform XML structure : " + e, e);
			e.printStackTrace();
		}

		return result.getWriter().toString();
	}

	/**
	 * Default filename getter.
	 * 
	 * @return the filename
	 */
	@Override
	public final String getFileName() {
		return sqlMapConfigFullFilename;
	}

	/**
	 * Unused.
	 * 
	 * @return the package name - unused
	 */
	@Override
	protected final String getGenericPackageName() {
		return null;
	}

	/**
	 * Run the sql map completion.
	 * 
	 * @param xmlDatasourceDescriptionFilename
	 *                the xml ibatis resource to reference
	 * @param destination
	 *                the base location
	 * @return the file name that has been updated
	 */
	public static String run(final String xmlDatasourceDescriptionFilename,
			final String destination) {
		SqlMapXmlFileGenerator generator = new SqlMapXmlFileGenerator();
		generator.setDestination(destination);
		generator.init(xmlDatasourceDescriptionFilename);

		if (generator.generate(null)) {
			return generator.getFileName();
		} else {
			return null;
		}
	}
}
