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
package org.lsc.utils.output.csv;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.jndi.JndiModificationType;
import org.lsc.jndi.JndiModifications;

/**
 * 
 * 
 * @author R�my-Christophe Schermesser <remy-christophe@schermesser.com>
 *
 */
public class CsvLayout extends LayoutBase<ILoggingEvent> {

	/* The logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(CsvLayout.class);

	/* Default values for the parameters */
	protected static String DEFAULT_SEPARATOR = ";";

	/* The separator of the log operations */
	protected static String OPTIONS_SEPARATOR = ",";

	/**
	 * Name of the attribute for the dn
	 */
	private final static String DN_STRING = "dn";

	/* Configurations from the log4j.properties */
	protected String attrs;
	protected String separator = DEFAULT_SEPARATOR;
	protected Boolean outputHeader = false;

	/* The attributes to write */
	protected List<String> attributes;

	/* The taskNames to log for */
	protected Set<String> taskNamesList;

	/* The operations to log */
	protected Set<JndiModificationType> operations;

	/**
	 * Default constructor
	 */
	public CsvLayout() {
		super();
	}

	/**
	 *
	 * WARN : We only write the first value of each attribute because we write in a 2 dimensional format
	 *
	 */
	@Override
	public String doLayout(ILoggingEvent event) {
		Object message = event.getMessage();

		String result = "";

		if (message != null && JndiModifications.class.isAssignableFrom(message.getClass())) {
			JndiModifications jm = (JndiModifications) message;


			if (operations.contains(jm.getOperation()) &&
							(taskNamesList.size() == 0 || taskNamesList.contains(jm.getTaskName().toLowerCase()))) {
				StringBuffer sb = new StringBuffer(1024);

				Iterator<String> iterator = attributes.iterator();

				HashMap<String, List<String>> modifications = jm.getModificationsItemsByHash();

				String attributeName = null;
				List<String> values = null;
				while (iterator.hasNext()) {

					/* Does the modification has the attribute ? */
					attributeName = iterator.next();
					if (modifications.containsKey(attributeName)) {
						values = modifications.get(attributeName);
						if (values.size() > 0) {
							sb.append(values.get(0));
						}
					} else if (attributeName.equalsIgnoreCase(DN_STRING)) {
						sb.append(jm.getDistinguishName());
					}
					if (iterator.hasNext()) {
						sb.append(this.separator);
					}
				}
				result += sb.toString();
				result += "\n";
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
	 * @param logOperation the logOperation to set
	 */
	public void setLogOperation(String logOperation) {
		/* Parse logOperations */
		operations = new HashSet<JndiModificationType>();
		if (logOperation != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(logOperation, CsvLayout.OPTIONS_SEPARATOR);
			String token = null;
			for (int i = 0; st.hasMoreTokens(); i++) {
				token = st.nextToken().toLowerCase();
				JndiModificationType op = JndiModificationType.getFromDescription(token);
				if (op != null) {
					operations.add(op);
				} else {
					LOGGER.error("Invalid operation in the CSV export (" + token + ")");
				}
			}
		} else {
			/* Add all the operations */
			JndiModificationType[] values = JndiModificationType.values();
			for (int i = 0; i < values.length; i++) {
				operations.add(values[i]);
			}
		}
	}

	/**
	 * @param attrs the attrs to set
	 */
	public void setAttrs(String attrs) {
		/* Parse attributes to log */
		attributes = new ArrayList<String>();
		if (attrs != null) {
			String[] st = attrs.split(this.separator);
			String token = null;
			for (int i = 0; i < st.length; i++) {
				token = st[i].toLowerCase();
				attributes.add(token);
			}
		} else {
			LOGGER.warn("There is no attributes to write in the CSV file.\nSet the " +
							"log4j.appender.NAME.layout.attrs property.");
		}
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
	 * @param outputHeader the outputHeader to set
	 */
	public void setOutputHeader(Boolean outputHeader) {
		this.outputHeader = outputHeader;
	}
}
