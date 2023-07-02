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
package org.lsc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.lsc.configuration.LscConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main launching class This is the main wrapper for generic launcher.
 * This class is responsible of parameters analysis
 *
 * @author S. Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class Launcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

	/** List of the synchronizing types. */
	private List<String> syncType;

	/** List of the asynchronous synchronizing types. */
	private List<String> asyncType;

	/** List of the cleaning types. */
	private List<String> cleanType;

	/** Configuration files location */
	private String configurationLocation;

	/** Default synchronize instance. */
	private SimpleSynchronize sync;

	/** Number of parallel threads to run a task */
	private int threads;

	/** Time limit in seconds*/
	private int timeLimit;
	
	/** Available command line options definition */
	private static Options options;
	
	/** Convert the old properties format to new XML */
	private boolean convertConfiguration;
	
	/** Perform a complete configuration validation */
	private boolean validateConfiguration;
	
	/** Parsed command line options */
	private CommandLine cmdLine;
	
	// define command line options recognized
	static {
		options = SimpleSynchronize.getOptions();
		options.addOption("a", "asynchronous-synchronize", true,
						"Asynchronous synchronization task (one of the available tasks or 'all')");
		options.addOption("s", "synchronize", true, "Synchronization task (one of the available tasks or 'all')");
		options.addOption("c", "clean", true, "Cleaning type (one of the available tasks or 'all')");
		options.addOption("v", "validate", false, "Validate configuration (check connections ...)");
		options.addOption("f", "config", true, "Specify configuration directory");
		options.addOption("t", "threads", true, "Number of parallel threads to synchronize a task (default: 5)");
		options.addOption("i", "time-limit", true, "Time limit in parallel server mode in seconds (default: 3600)");
		options.addOption("h", "help", false, "Get this text");
		options.addOption("V", "version", false, "Get project version");
	}
	
	/**
	 * Default constructor - instantiate objects.
	 */
	public Launcher() {
		syncType = new ArrayList<String>();
		asyncType = new ArrayList<String>();
		cleanType = new ArrayList<String>();
	}

	/**
	 * Main launcher.
	 *
	 * @param args parameters passed by the JRE
	 */
	public static void main(final String[] args) {
		int status = launch(args);
		if(status != 0) {
			System.exit(status);
		}
	}

		
	public static int launch(final String[] args) {
		try {
			// Create the object and parse options
			Launcher obj = new Launcher();
			int retCode = obj.parseOptions(args);
	
			if (retCode != 0) {
				return retCode;
			}
			// Wrap the launcher
			return obj.run();
		} catch (Exception e) {
			if (!Configuration.isLoggingSetup()) {
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace();
			} else {
				LOGGER.error(e.toString());
				LOGGER.debug(e.toString(), e);
			}
			return 1;
		}
	}

	/**
	 * Launch the synchronization and cleaning process.
	 */
	public int run() {
		try {
			if (validateConfiguration) {
				if(configurationLocation == null) {
					printHelp();
					return 1;
				}
				// if a configuration directory was set on command line, use it to set up Configuration
				Configuration.setUp(configurationLocation, true);
				if(LscConfiguration.isInitialized()) {
	                LOGGER.info("Configuration and environment successfully checked !");
	                return 0;
				} else {
				    LOGGER.info("Configuration validation failed !");
				    return 255;
				}
			}
			
			// if a configuration directory was set on command line, use it to set up Configuration
			Configuration.setUp(configurationLocation);
			if(!LscConfiguration.isInitialized()) {
			    return 255;
			}

			// initialize the synchronization engine
			sync = new SimpleSynchronize();
			if (!sync.parseOptions(cmdLine)) {
				printHelp();
				return 1;
			}
			// do the work!
			if (threads > 0) {
				sync.setThreads( threads );
			}
			if (timeLimit > 0) {
				sync.setTimeLimit( timeLimit );
			}
			sync.launch(asyncType, syncType, cleanType);
		} catch (Exception e) {
			if (!Configuration.isLoggingSetup()) {
				System.err.println("Error: " + e.toString());
				e.printStackTrace();
			} else {
				LOGGER.error(e.toString());
				LOGGER.debug(e.toString(), e);
			}
			return 1;
		}
		return 0;
	}

	/**
	 * Manage command line options.
	 * @param args command line
	 * @return the status code (0: OK, >=1 : failed)
	 */
	private int parseOptions(final String[] args) {
		CommandLineParser parser = new DefaultParser();

		try {
			cmdLine = parser.parse(options, args);

			if (cmdLine.hasOption("a")) {
				asyncType = parseSyncType(cmdLine.getOptionValue("a"));
			}
			if (cmdLine.hasOption("s")) {
				syncType = parseSyncType(cmdLine.getOptionValue("s"));
			}
			if (cmdLine.hasOption("f")) {
				configurationLocation = new File(cmdLine.getOptionValue("f")).getAbsolutePath();
			}
			if (cmdLine.hasOption("t")) {
				threads = Integer.parseInt(cmdLine.getOptionValue("t"));
			}
			if (cmdLine.hasOption("i")) {
				timeLimit = Integer.parseInt(cmdLine.getOptionValue("i"));
			}
			if (cmdLine.hasOption("c")) {
				cleanType = parseSyncType(cmdLine.getOptionValue("c"));
			}
			if (cmdLine.hasOption("v")) {
				validateConfiguration = true;
			}
			if (cmdLine.hasOption("V")) {
                            final Properties properties = new Properties();
                            try {
                                properties.load(Launcher.class.getClassLoader().getResourceAsStream(".properties"));
                                System.out.println(properties.getProperty("lsc.version"));
                            }
                            catch(IOException e) {
                                System.err.println(".properties missing in jar, this is a build issue");
                                e.printStackTrace(System.err);
                            }
                            return 1;

			}
		
			if(cmdLine.getOptions().length == 0 || 
							cmdLine.hasOption("h") || 
							((asyncType.size() == 0) && (syncType.size() == 0) && (cleanType.size() == 0)) 
							&& ! convertConfiguration && ! validateConfiguration ) {
				printHelp();
				return 1;
			}
			if(!asyncType.isEmpty() && (!syncType.isEmpty() || !cleanType.isEmpty())) {
				System.err.println("Asynchronous synchronization is mutually exclusive with synchronous synchronizing and cleaning !");
				printHelp();
				return 1;
			}
		} catch (MissingArgumentException e) {
			LOGGER.error("Missing arguments ({})", e.toString());
			LOGGER.debug(e.toString(), e);
			return 1;
		} catch (ParseException e) {
			LOGGER.error("Unable to parse the options ({})", e.toString());
			LOGGER.debug(e.toString(), e);
			return 1;
		}
		return 0;
	}

	/**
	 * Parse the synchronization string to find the right type of
	 * synchronization or cleaning.
	 * @param syncValue the string comma separated synchronization name
	 * @return the synchronizations name
	 */
	private List<String> parseSyncType(final String syncValue) {
		List<String> ret = new ArrayList<String>();

		// Add each value to returned strings list
		StringTokenizer st = new StringTokenizer(syncValue, ",");

		while (st.hasMoreTokens()) {
			ret.add(st.nextToken());
		}

		return ret;
	}

	/**
	 * Print the command line help.
	 */
	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("lsc", options);
	}

}
