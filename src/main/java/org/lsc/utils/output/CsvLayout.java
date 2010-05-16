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

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *    * Neither the name of the LSC Project nor the names of its
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
package org.lsc.utils.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;

/**
 *
 *
 * @author R&eacute;my-Christophe Schermesser &lt;remy-christophe@schermesser.com&gt;
 *
 */
public class CsvLayout extends LayoutBase<ILoggingEvent> {

	/* The logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(CsvLayout.class);

	/* Default values for the parameters */
	protected static final String DEFAULT_SEPARATOR = ";";

	/* The separator of the log operations */
	protected static final String OPTIONS_SEPARATOR = ",";

	/* Configurations from the log configuration */
	private String logOperations;
	private String attrs;
	private String separator = DEFAULT_SEPARATOR;
	private String taskNames;

	/* The attributes to write */
	protected List<String> attributes;

	/* The taskNames to log for */
	protected Set<String> taskNamesList;

	/* The operations to log */
	protected Set<JndiModificationType> operations;

	/* Output header to a CSV file? */
	private Boolean outputHeader = false;
	
	/**
	 * Name of the attribute for the dn
	 */
	private final static String DN_STRING = "dn";

	/**
	 * Output log events in CSV format for the JndiModifications class
	 * WARN : We only write the first value of each attribute because we write in a 2 dimensional format
	 * @param event {@link ILoggingEvent} object representing an event to log
	 * @return The String to log
	 */
	public String doLayout(ILoggingEvent event) {
		Object[] messages = event.getArgumentArray();
		String result = "";

		if (messages != null &&
						messages.length != 0 &&
						messages[0] != null &&
						JndiModifications.class.isAssignableFrom(messages[0].getClass()) ) {
			JndiModifications jm = (JndiModifications) messages[0];


			if (operations.contains(jm.getOperation()) && 
							( taskNamesList.size() == 0 ||
							  taskNamesList.contains(jm.getTaskName().toLowerCase()))) {
				StringBuilder sb = new StringBuilder(1024);

				Map<String, List<String>> modifications = jm.getModificationsItemsByHash();

				List<String> values = null;

				for(String attributeName: attributes) {
					/* Does the modification has the attribute ? */
					if (modifications.containsKey(attributeName)) {
						values = modifications.get(attributeName);
						if (values.size() > 0) {
							sb.append(values.get(0));
						}
					} else if (attributeName.equalsIgnoreCase(DN_STRING)) {
						sb.append(jm.getDistinguishName());
					}
					
					sb.append(separator);
				}
				//Remove the last unecessary separator
				sb.deleteCharAt(sb.length()-1).append("\n");
				result = sb.toString();
			}
		}
		return result;
	}

	public String getHeader() {
		String result = "";
		if (outputHeader) {
			result = attrs + "\n";
		}
		return result;
	}

	/**
	 * Parse options
	 *
	 */
	@Override
	public void start() {
		/* Parse logOperations */
		operations = new HashSet<JndiModificationType>();
		if (logOperations != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(logOperations, CsvLayout.OPTIONS_SEPARATOR);
			String token = null;
			for (int i = 0; st.hasMoreTokens(); i++) {
				token = st.nextToken().toLowerCase();
				JndiModificationType op = JndiModificationType.getFromDescription(token);
				if (op != null) {
					operations.add(op);
				} else {
					LOGGER.error("Invalid operation in the CSV export ({})", token);
				}
			}
		} else {
			/* Add all the operations */
			for(JndiModificationType type: JndiModificationType.values()) {
				operations.add(type);
			}
		}

		/* Parse attributes to log */
		attributes = new ArrayList<String>();
		if (attrs != null) {
			for(String st: attrs.split(separator)) {
				attributes.add(st.toLowerCase());
			}
		} else {
			LOGGER.warn("There is no attributes to write in the CSV file.\nSet the attrs property in the logback configuration file");
		}

		/* Parse task names to log for */
		taskNamesList = new HashSet<String>();
		if (taskNames != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(taskNames, CsvLayout.OPTIONS_SEPARATOR);
			String token = null;
			for (int i = 0; st.hasMoreTokens(); i++) {
				token = st.nextToken().toLowerCase();
				taskNamesList.add(token);
			}
		}
	}

    /**
	 * @param logOperations the logOperation to set
	 */
	public void setLogOperations(String logOperations) {
		this.logOperations = logOperations;
	}

	/**
	 * @param attrs the attrs to set
	 */
	public void setAttrs(String attrs) {
		this.attrs = attrs;
	}

	/**
	 * @param separator the separator to set
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * @param taskNames the taskNames to set
	 */
	public void setTaskNames(String taskNames) {
		this.taskNames = taskNames;
	}

	/**
	 * @param outputHeader the outputHeader to set
	 */
	public void setOutputHeader(Boolean outputHeader) {
		this.outputHeader = outputHeader;
	}
}
