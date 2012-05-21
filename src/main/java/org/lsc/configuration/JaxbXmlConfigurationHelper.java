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
package org.lsc.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.FileUtils;
import org.lsc.exception.LscConfigurationException;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Load/dump a configuration from/to XML file
 * 
 * @author Sebastien Bahloul
 */
public class JaxbXmlConfigurationHelper {

	public static final String LSC_CONF_XML = "lsc.xml";
	public static final String LSC_NAMESPACE = "http://lsc-project.org/XSD/lsc-core-2.0.xsd"; 
	private JAXBContext jaxbc;

	private static Logger LOGGER = LoggerFactory.getLogger(JaxbXmlConfigurationHelper.class);
	
	/**
	 * Initiate helper by adding XML aliases
	 * @throws LscConfigurationException 
	 */
	public JaxbXmlConfigurationHelper() throws LscConfigurationException {
	    String packagesName = Lsc.class.getPackage().getName();
	    String pluginsPackagePath = System.getProperty("LSC.PLUGINS.PACKAGEPATH");
	    if( pluginsPackagePath != null) {
	    	packagesName = packagesName + ":" + pluginsPackagePath;
	    }
	    try {
			jaxbc = JAXBContext.newInstance( packagesName );
		} catch (JAXBException e) {
			throw new LscConfigurationException(e);
		}
	}

//	private String appendAllPackageNames(String packageName,
//			Collection<String> findExtensions) {
//		for(String extension: findExtensions) {
//			String extensionPN = extension.substring(0, extension.lastIndexOf("."));
//			if(!packageName.contains( extensionPN + ":") && !packageName.endsWith(extensionPN)) {
//				packageName = packageName + ":" +  extensionPN;
//			}
//		}
//		return packageName;
//	}

    /**
     * Load an XML file to the object
     * 
     * @param filename
     *            filename to read from
     * @return the completed configuration object
     * @throws FileNotFoundException
     *             thrown if the file can not be accessed (either because of a
     *             misconfiguration or due to a rights issue)
     * @throws LscConfigurationException 
     */
    public Lsc getConfiguration(String filename)
            throws LscConfigurationException {
        LOGGER.debug("Loading XML configuration from: " + filename);
        try {
            Unmarshaller unmarshaller = jaxbc.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema lscSchema = null;
            try {
                int i = 0;
                Set<URL> urls = new HashSet<URL>();
                urls.addAll(ClasspathHelper.forPackage("org.lsc"));
                if(System.getProperty("LSC.PLUGINS.PACKAGEPATH") != null) {
                    String[] pathElements = System.getProperty("LSC.PLUGINS.PACKAGEPATH").split(System.getProperty("path.separator"));
                    for(String pathElement: pathElements) {
                        urls.addAll(ClasspathHelper.forPackage(pathElement));
                    }
                }
                Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .addUrls(urls).setScanners(new ResourcesScanner(), new SubTypesScanner()));
                
                Set<String> xsdFiles = reflections.getResources(Pattern.compile(".*\\.xsd"));
                Source[] schemasSource = new Source[xsdFiles.size()];
                List<String> xsdFilesList = new ArrayList<String>(xsdFiles);
                Collections.reverse(xsdFilesList);
                for(String schemaFile: xsdFilesList) {
                    LOGGER.debug("Importing XML schema file: " + schemaFile);
                    InputStream schemaStream = this.getClass().getClassLoader().getResourceAsStream(schemaFile);
                    schemasSource[i++] = new StreamSource(schemaStream);
                }
                lscSchema = schemaFactory.newSchema(schemasSource);
                unmarshaller.setSchema( lscSchema );
            } catch (VerifyError e) {
                throw new LscConfigurationException(e.toString(), e);
            } catch (SAXException e) {
                throw new LscConfigurationException(e);
            }
            
            return (Lsc)unmarshaller.unmarshal( new File(filename) );
        } catch (JAXBException e) {
            throw new LscConfigurationException(e);
        }
    }

	/**
	 * Dump the object to an XML file (by overriding if necessary)
	 * 
	 * @param filename
	 *            filename to write to
	 * @param lscConf
	 *            configuration object
	 * @throws FileNotFoundException
	 *             thrown if the file can not be accessed (either because of a
	 *             misconfiguration or due to a rights issue)
	 */
	public void saveConfiguration(String filename, Lsc lscConf)
			throws IOException {
		File existing = new File(filename);
		if(existing.exists()) {
			File backup = new File(existing + ".bak");
			if(backup.exists()) {
				backup.delete();
			}
			FileUtils.copyFile(existing, backup);
		}
		try {
			Marshaller marshaller = jaxbc.createMarshaller();
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
			marshaller.marshal(lscConf, new File(filename));
		} catch (JAXBException e) {
			LOGGER.error("Cannot save configuration file: " + e.toString(), e);
		}
	}
}
