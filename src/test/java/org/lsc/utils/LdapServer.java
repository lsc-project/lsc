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
import java.net.URISyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.Configuration;
import org.lsc.opends.EmbeddedOpenDS;
import org.opends.server.api.Backend;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.CanceledOperationException;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.InitializationException;
import org.opends.server.util.LDIFException;
import org.opends.server.util.StaticUtils;

/**
 * Used to manage directory state (start/stop/status/...)
 * @author Sebastien Bahloul <seb@lsc-project.org>
 */
public class LdapServer {

	/** The local logger */
	private static Logger LOGGER = LoggerFactory.getLogger(LdapServer.class);
	
	public final static void start() throws InitializationException, IOException, URISyntaxException, DirectoryException, ConfigException, CanceledOperationException, LDIFException {
		EmbeddedOpenDS.startServer();
		EmbeddedOpenDS.initializeTestBackend(false, Configuration.DN_REAL_ROOT);
		Backend backend = DirectoryServer.getBackend(DN.decode(Configuration.DN_REAL_ROOT));
		backend.addEntry(StaticUtils.createEntry(DN.decode(Configuration.DN_REAL_ROOT)), null);
		if(EmbeddedOpenDS.class.getResource("test.ldif") == null || EmbeddedOpenDS.class.getResource("test.ldif").toURI().getPath() == null) {
			LOGGER.error("Unable to load LDIF sample content !");
		} else {
			EmbeddedOpenDS.importLdif(EmbeddedOpenDS.class.getResource("test.ldif").toURI().getPath());
			LOGGER.info("LDIF sample content loaded successfully");
		}
	}
	
	public final static void stop() {
		EmbeddedOpenDS.shutdownServer("Normal stop process");
	}

	/**
	 * Main launcher
	 * 
	 * @param args parameters passed by the JRE
	 */
	public static void main(final String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		int retCode = 1;
		try {
			retCode = usage(args);
		} catch (InitializationException e) {
			LOGGER.error(e.toString(), e);
		} catch (DirectoryException e) {
			LOGGER.error(e.toString(), e);
		} catch (CanceledOperationException e) {
			LOGGER.error(e.toString(), e);
		} catch (LDIFException e) {
			LOGGER.error(e.toString(), e);
		} catch (IOException e) {
			LOGGER.error(e.toString(), e);
		} catch (URISyntaxException e) {
			LOGGER.error(e.toString(), e);
		} catch (ConfigException e) {
			LOGGER.error(e.toString(), e);
		}
		if (retCode != 0) {
			System.exit(retCode);
		}
	}

	/**
	 * Manage command line options
	 * @param args command line
	 * @return the status code (0: OK, >=1 : failed)
	 * @throws IOException 
	 * @throws LDIFException 
	 * @throws CancelledOperationException 
	 * @throws DirectoryException 
	 * @throws InitializationException 
	 * @throws URISyntaxException 
	 * @throws ConfigException 
	 */
	private static int usage(String[] args) throws InitializationException, DirectoryException, CanceledOperationException, LDIFException, IOException, URISyntaxException, ConfigException {
		Options options = new Options();
		options.addOption("a", "start", false, "Start the embedded directory");
		options.addOption("o", "stop", false, "Stop the embedded directory");
		options.addOption("h", "help", false, "Get this text");
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine cmdLine = parser.parse(options, args);
			if (cmdLine.getOptions().length > 0) {
				if (cmdLine.hasOption("a")) {
					start();
				} else if (cmdLine.hasOption("o")) {
					stop();
				}
				if (cmdLine.hasOption("h")) {
					printHelp(options);
					return 1;
				}
			} else {
				printHelp(options);
				return 1;
			}
		} catch (ParseException e) {
			LOGGER.error("Unable to parse options : " + args + " (" + e + ")", e);
			return 1;
		}
		return 0;
	}

	/**
	 * Print the formater
	 * @param options
	 */
	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("lsc", options);
	}
}
