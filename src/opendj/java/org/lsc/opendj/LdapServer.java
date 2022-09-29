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
package org.lsc.opendj;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.lsc.Configuration;
import org.lsc.exception.LscException;
import org.opends.server.api.Backend;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.DN;
import org.opends.server.util.StaticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Used to manage directory state (start/stop/status/...)
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdapServer {

	/** The local logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(LdapServer.class);
	private static Options options;
	
	public final static void start() throws Exception {
		EmbeddedOpenDJ.startServer();
		EmbeddedOpenDJ.initializeTestBackend(false, Configuration.DN_REAL_ROOT);
		Backend backend = DirectoryServer.getBackend(DN.decode(Configuration.DN_REAL_ROOT));
		backend.addEntry(StaticUtils.createEntry(DN.decode(Configuration.DN_REAL_ROOT)), null);
		
		String ldifPath = EmbeddedOpenDJ.getPathToConfigFile("test.ldif");
		if(ldifPath == null || "".equals(ldifPath)) {
			LOGGER.error("Unable to load LDIF sample content !");
		} else {
			EmbeddedOpenDJ.importLdif(ldifPath);
			LOGGER.info("LDIF sample content loaded successfully");
		}
	}
	
	public final static void stop() {
		EmbeddedOpenDJ.shutdownServer("Normal stop process");
	}

	/**
	 * Main launcher
	 * 
	 * @param args parameters passed by the JRE
	 */
	public static void main(final String[] args) {
		parseOptionsForConfiguration(args);
		
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset(); //reset configuration

		String logBackXMLPropertiesFile = Configuration.getConfigurationDirectory() + "logback-opends.xml";

		try {
			configurator.doConfigure(logBackXMLPropertiesFile);
		} catch (JoranException je) {
			System.err.println("Can not find LogBack configuration file for OpenDS");
			System.exit(1);
		}

		int retCode = 1;
		try {
			retCode = usage(args);
		} catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
		}
		if (retCode != 0) {
			System.exit(retCode);
		}
	}

	private static CommandLine getOptionsCmdLine(String[] args) throws ParseException {
		options = new Options();
		options.addOption("a", "start", false, "Start the embedded directory");
		options.addOption("o", "stop", false, "Stop the embedded directory");
		options.addOption("f", "config", true, "Specify configuration directory");
		options.addOption("h", "help", false, "Get this text");
		CommandLineParser parser = new GnuParser();
		return parser.parse(options, args);
	}
	
	private static void parseOptionsForConfiguration(String[] args) {
		try {
			CommandLine cmdLine = getOptionsCmdLine(args);

			if (cmdLine.getOptions().length > 0 && !cmdLine.hasOption("h")) {
				if (cmdLine.hasOption("f")) {
					Configuration.setUp(new File(cmdLine.getOptionValue("f")).getAbsolutePath(), false);
				}
			} else {
				printHelp(options);
			}
		} catch (ParseException e) {
			if(LOGGER.isErrorEnabled()) {
				StringBuilder sbf = new StringBuilder();
				for(String arg: args) {
					sbf.append(arg).append(" ");
				}
				LOGGER.error("Unable to parse options : {}({})", sbf.toString(), e);
			}
			LOGGER.debug(e.toString(), e);
		} catch (LscException e) {
            LOGGER.warn("Error while loading configuration: " + e.toString());
		}
	}
	
	/**
	 * Manage command line options
	 * @param args command line
	 * @return the status code (0: OK, >=1 : failed)
	 * @throws Exception 
	 */
	private static int usage(String[] args) throws Exception {
		try {
			CommandLine cmdLine = getOptionsCmdLine(args);

			if (cmdLine.getOptions().length > 0 && !cmdLine.hasOption("h")) {
				if (cmdLine.hasOption("a")) {
					start();
				} else if (cmdLine.hasOption("o")) {
					stop();
				}
			} else {
				printHelp(options);
				return 1;
			}
		} catch (ParseException e) {
			if(LOGGER.isErrorEnabled()) {
				StringBuilder sbf = new StringBuilder();
				for(String arg: args) {
					sbf.append(arg).append(" ");
				}
				LOGGER.error("Unable to parse options : {}({})", sbf.toString(), e);
			}
			LOGGER.debug(e.toString(), e);
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
