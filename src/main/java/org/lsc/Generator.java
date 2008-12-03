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

import javax.naming.NamingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.lsc.beans.BeanGenerator;
import org.lsc.objects.ObjectClassGenerator;
import org.lsc.persistence.SqlMapXmlFileGenerator;
import org.lsc.service.JdbcSrcServiceObjectGenerator;
import org.lsc.utils.Csv2SqlObjectGenerator;
import org.lsc.utils.PropertiesGenerator;


/**
 * Main generation class.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public final class Generator {
    /** This is the Log4j configuration file. */
    public static final String LOG4J_CONFIGURATION_FILE = "log4j.properties";

    /** This is the local Log4j logger. */
    private static final Logger LOGGER = Logger.getLogger(Generator.class);

    /** When the CSV separator is rounded by ' or ", just remove them. */
    private static final int CSV_SEPARATOR_ROUNDED = 3;

    /**
     * This is the source Directory Object class name which is the
     * generator base.
     */
    private String srcClassName;

    /**
     * This is the Destination Directory Object class name which is
     * the generator base.
     */
    private String dstClassName;

    /** This is the location where generated classes must be stored. */
    private String destination;

    /** Generation type for this particular launch time. */
    private GEN_TYPE genType;

    /** The CSV separator (if needed or null). */
    private String csvSeparator;

    /** The CSV filename (if needed or null). */
    private String csvFilename;

    /** The task name */
    private String taskName;

    /**
     * Private constructor.
     */
    private Generator() {}

    /**
     * Generate synchronization engine skeleton.
     *
     * @throws NamingException thrown if a directory exception is encountered
     */
    private void launch() throws NamingException {
        String srcServiceClassName = null;
        String xmlFilename = null;
        String beanClassName = null;
        
        LOGGER.info("Generating bean for class \"" + dstClassName + "\" from target directory...");
        beanClassName = BeanGenerator.run(dstClassName, destination, false);

        if (genType == GEN_TYPE.LDAP2LDAP) {
        	// Generating a JndiSrcService is no longer necessary - use SimpleJndiSrcService instead
            /*srcServiceClassName = JndiSrcObjectGenerator.run(srcClassName,
                                                             destination); */
        } else {
            JdbcSrcServiceObjectGenerator jssoGenerator = new JdbcSrcServiceObjectGenerator(); 
            srcServiceClassName = jssoGenerator.run(dstClassName, destination);
            xmlFilename = jssoGenerator.getMyXMLFileName();
            SqlMapXmlFileGenerator.run(xmlFilename, destination);
        }

        LOGGER.info("Generating Java class for LDAP objectclass \"" + dstClassName + "\" from target directory...");
        String objectClassName = ObjectClassGenerator.run(dstClassName, destination, false);

        if (genType == GEN_TYPE.LDAP2LDAP) {
            LOGGER.info("Generating Java class for LDAP objectclass \"" + srcClassName + "\" from source directory...");
            ObjectClassGenerator.run(srcClassName, destination, true);
        }

        if (genType == GEN_TYPE.CSV2LDAP) {
            Csv2SqlObjectGenerator.run(dstClassName, destination, csvFilename,  csvSeparator);
        }

        LOGGER.info("Writing properties file...");
        PropertiesGenerator.run(taskName, destination, genType,
                                beanClassName, objectClassName,
                                srcServiceClassName);
    }

    /**
     * DOCUMENT ME!
     *
     * @param args command line parameters
     *
     * @throws NamingException DOCUMENT ME!
     */
    public static void main(final String[] args) throws NamingException {
        PropertyConfigurator.configure(LOG4J_CONFIGURATION_FILE);

        Generator instance = new Generator();

        if (instance.parseArgs(args) == 0) {
            instance.launch();
        }
    }

    /**
     * Print the general usage through Ant.
     * @param options the options
     */
    private static void printHelp(final Options options) {
        System.err.println("Usage:");
        String cmn = "\tant -Dgenerator.parameters=\"-dir ../../src/impl/java -doc 'localObjectClass' -name MyTask ";
        System.err.println(cmn
            + "-db2ldap\" lsc::generator\n");
        System.err.println(cmn
            + "-csv2ldap "
            + "-csvf ../../sample/csvtosql/sample.csv "
            + "-csvsep ';'\" lsc::generator\n");
        System.err.println(cmn
            + "-ldap2ldap -soc='sourceObjectClass'\" lsc::generator\n");
    }

    /**
     * Manage command line options.
     * @param args command line
     * @return the status code (0: OK, >=1 : failed)
     */
    private int parseArgs(final String[] args) {
        Options options = new Options();

        options.addOption("soc", "sourceOcName", true,
                          "Specify the source object class name");
        options.addOption("doc", "destOcName", true,
                          "Specify the destination object class name");
        options.addOption("dir", "directory", true,
                          "Specify the destination directory");
        options.addOption("csvsep", "csvseparator", true,
                          "Specify the csv separator");
        options.addOption("csvf", "csvfilename", true,
                          "Specify the csv filename");
        options.addOption("name", "taskName", true, "Specify the task name");

        OptionGroup genTypeOption = new OptionGroup();
        genTypeOption.setRequired(true);
        genTypeOption.addOption(new Option("ldap2ldap", "ld", false,
                               "Generate a LDAP to LDAP engine skeleton"));
        genTypeOption.addOption(new Option("db2ldap", "db", false,
                               "Generate a database to LDAP engine skeleton"));
        genTypeOption.addOption(new Option("csv2ldap", "csv", false,
                               "Generate a CSV to LDAP engine skeleton"));
        options.addOptionGroup(genTypeOption);

        CommandLineParser parser = new GnuParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.getOptions().length > 0) {
                if (cmdLine.hasOption("soc")) {
                    srcClassName = cmdLine.getOptionValue("soc");
                }

                if (cmdLine.hasOption("doc")) {
                    dstClassName = cmdLine.getOptionValue("doc");
                }

                if (cmdLine.hasOption("dir")) {
                    destination = cmdLine.getOptionValue("dir");
                }

                if (cmdLine.hasOption("csvsep")) {
                    csvSeparator = cmdLine.getOptionValue("csvsep");
                    if (csvSeparator.length() == CSV_SEPARATOR_ROUNDED) {
                        csvSeparator = "" + csvSeparator.charAt(1);
                    }
                }

                if (cmdLine.hasOption("csvf")) {
                    csvFilename = cmdLine.getOptionValue("csvf");
                }

                if (cmdLine.hasOption("name")) {
                    taskName = cmdLine.getOptionValue("name");
                }

                if (cmdLine.hasOption("ldap2ldap")) {
                    genType = GEN_TYPE.LDAP2LDAP;
                } else if (cmdLine.hasOption("db2ldap")) {
                    genType = GEN_TYPE.DATABASE2LDAP;
                } else if (cmdLine.hasOption("csv2ldap")) {
                    genType = GEN_TYPE.CSV2LDAP;
                } else {
                    genType = GEN_TYPE.UNKNOWN;
                }

                if ((dstClassName == null) || (genType == GEN_TYPE.UNKNOWN)
                        || ((genType == GEN_TYPE.LDAP2LDAP)
                                && (srcClassName == null))) {
                    printHelp(options);

                    return 1;
                }

                if ( (genType == GEN_TYPE.CSV2LDAP)
                        && ( (csvSeparator == null) || (csvFilename == null) ) ) {
                    printHelp(options);

                    return 1;
                }
            } else {
                printHelp(options);

                return 1;
            }
        } catch (MissingOptionException e) {
            printHelp(options);
            
            return 1;
        } catch (ParseException e) {
            LOGGER.fatal("Unable to parse options : " + args + " (" + e + ")", e);
            printHelp(options);

            return 1;
        }

        return 0;
    }

    /** This is the list of the available generation type. */
    public static enum GEN_TYPE {
        /** CSV to Database to directory generation type. */
        CSV2LDAP, 
        /** Directory to directory generation type. */
        LDAP2LDAP, 
        /** Database to directory generation type. */
        DATABASE2LDAP, 
        /** Unknown. */
        UNKNOWN;
    }
}
