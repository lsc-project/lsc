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
package org.lsc.connectors.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains most of the required client methods to handle XmlRpc calls
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractLscXmlRpcClient extends AbstractLscXmlRpcObject {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLscXmlRpcClient.class);
	
	private URL url;
	private String username;
	private String password;
	protected XmlRpcClient client;
	protected Options options;
	protected CommandLine cmdLine;
	
	public AbstractLscXmlRpcClient() {
		options = new Options();
		options.addOption("h", "hosturl", true, "Specify the XML RPC server URL");
		options.addOption("u", "username", true, "Specify the username");
		options.addOption("p", "password", true, "Specify the password");

		client = new XmlRpcClient();
	}
	
	public abstract void run() throws XmlRpcException;

	public boolean bind()  {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(url);
		if(username != null && password != null) {
			config.setBasicUserName(username);
			config.setBasicPassword(password);
		}
		config.setEnabledForExtensions(false);
		config.setGzipCompressing(false);
		client.setConfig(config);
		return true;
	}
	
	protected int parseOptions(final String[] args) throws MalformedURLException {
		CommandLineParser parser = new GnuParser();
		
		try {
			cmdLine = parser.parse(options, args);
			if ( cmdLine.hasOption("h") ) {
				url = new URL(cmdLine.getOptionValue("h"));
			}
			if ( cmdLine.hasOption("u") ) {
				username = cmdLine.getOptionValue("u");
			}
			if ( cmdLine.hasOption("p") ) {
				password = cmdLine.getOptionValue("p");
			}
			if( url == null ) {
				printHelp(options);
				return -1;
			}
		} catch (ParseException e) {
			LOGGER.error("Unable to parse the options ({})", e.toString());
			LOGGER.debug(e.toString(), e);
			return 1;
		}
		return 0;
	}
	
	/**
	 * Print the command line help.
	 * 
	 * @param options
	 *            specified options to manage
	 */
	protected static void printHelp(final Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("lsc", options);
	}
}
