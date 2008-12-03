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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.lsc.jndi.JndiModifications;

/**
 * This class represents all the different modes available to limit
 * synchronization tasks. <p/> By default nothing is filtered. If the command
 * line gets the right options you can filter entries creations, updates or
 * deletes events.
 */
public class JndiModificationsFilter {

    /** This is the local LOG4J logger. */
    private static final Logger LOGGER = Logger.getLogger(JndiModificationsFilter.class);

    /**
     * This is the flag to prevent entries add operation in the target
     * directory.
     */
    private boolean nocreate;

    /**
     * This is the flag to prevent entries update operation in the target
     * directory.
     */
    private boolean noupdate;

    /**
     * This is the flag to prevent entries delete operation in the target
     * directory.
     */
    private boolean nodelete;

    /**
     * Default constructor.
     */
    public JndiModificationsFilter() {
        nocreate = false;
        noupdate = false;
        nodelete = false;
    }

    /**
     * Parse the command line options.
     * 
     * @param args
     *                the command line arguments
     * @param options
     *                the command line options that may be valid
     * @return the parsing status
     */
    public final boolean parseCommandLine(final String[] args, 
            final Options options) {
        CommandLineParser parser = new GnuParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            if (cmdLine.getOptions().length > 0) {
                if (cmdLine.hasOption("nc")) {
                    nocreate = true;
                }
                if (cmdLine.hasOption("nu")) {
                    noupdate = true;
                }
                if (cmdLine.hasOption("nd")) {
                    nodelete = true;
                }
                if (cmdLine.hasOption("n")) {
                    nocreate = true;
                    noupdate = true;
                    nodelete = true;
                }
            } else {
                return false;
            }
        } catch (final ParseException e) {
            LOGGER.fatal("Unable to parse options : " + args + " (" + e + ")", e);
            return false;
        }
        return true;
    }

    /**
     * Filter the jndi modifications.
     * 
     * @param jm
     *                the jndi modifications parameter
     * @return the updated jndi modifications
     */
    public final JndiModifications filter(final JndiModifications jm) {
        switch (jm.getOperation()) {
        case ADD_ENTRY:
            if (nocreate) {
                return null;
            }
            break;
        case MODIFY_ENTRY:
            if (noupdate) {
                return null;
            }
            break;
        case MODRDN_ENTRY:
            if (noupdate) {
                return null;
            }
            break;
        case DELETE_ENTRY:
            if (nodelete) {
                return null;
            }
            break;
        default:
            return null;
        }
        return jm;
    }
}
