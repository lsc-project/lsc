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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Main launching class This is the main wrapper for generic launcher.
 * This class is responsible of parameters analysis
 *
 * @author S. Bahloul &lt;seb@lsc-project.org&gt;
 * @deprecated
 */
public final class Launcher {
    /** The local LOG4J logger. */
    private static final Logger LOGGER = Logger.getLogger(Launcher.class);

    /** List of the synchronizing types. */
    private List<String> syncType;

    /** List of the cleaning types. */
    private List<String> cleanType;

    /** Default synchronize instance. */
    private SimpleSynchronize sync;

    /**
     * Default constructor - instantiate objects.
     */
    public Launcher() {
        syncType = new ArrayList<String>();
        cleanType = new ArrayList<String>();
        sync = new SimpleSynchronize();
    }

    /**
     * Main launcher.
     *
     * @param args parameters passed by the JRE
     *
     * @throws MalformedURLException thrown
     */
    public static void main(final String[] args) throws MalformedURLException {
        //Initiate log4j engine
        PropertyConfigurator.configure("log4j.properties");

        // Create the object and parse options
        Launcher obj = new Launcher();
        int retCode = obj.usage(args);

        if (retCode != 0) {
            System.exit(retCode);
        }

        // Wrap the launcher
        obj.run();
    }

    /**
     * Launch the synchronization and cleaning process.
     */
    public void run() {
        try {
            sync.launch(syncType, cleanType);
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

    /**
     * Manage command line options.
     * @param args command line
     * @return the status code (0: OK, >=1 : failed)
     */
    private int usage(final String[] args) {
        Options options = sync.getOptions();
        options.addOption("l", "startLdapServer", false,
                "Start the embedded OpenDS LDAP server "
                + "(will be shutdown at the end)");
        options.addOption("s", "synchronization", true,
                "Synchronization type (one of the available "
                + "tasks or 'all')");
        options.addOption("c", "cleaning", true,
                "Cleaning type (one of the available "
                + "tasks or 'all')");
        options.addOption("h", "help", false, "Get this text");

        CommandLineParser parser = new GnuParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.getOptions().length > 0) {
                if (cmdLine.hasOption("s")) {
                    syncType = parseSyncType(cmdLine.getOptionValue("s"));
                }
                if (cmdLine.hasOption("c")) {
                    cleanType = parseSyncType(cmdLine.getOptionValue("c"));
                }
                if (!sync.parseOptions(args)) {
                    printHelp(options);
                    return 1;
                }
                if (cmdLine.hasOption("h")
                        || ((syncType.size() == 0) 
                                && (cleanType.size() == 0))) {
                    printHelp(options);
                    return 1;
                }
            } else {
                printHelp(options);
                return 1;
            }
        } catch (ParseException e) {
            LOGGER.fatal("Unable to parse options : " + args + " (" + e + ")",  e);
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
     * @param options specified options to manage
     */
    private static void printHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("lsc", options);
    }
}
