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
 *               (c) 2008 - 2011 LSC Project
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.utils.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.lsc.LscModificationType;
import org.lsc.LscModifications;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;

/**
 *
 *
 * @author R&eacute;my-Christophe Schermesser &lt;remy-christophe@schermesser.com&gt;
 *
 */
public class CsvLayout extends LayoutBase<ILoggingEvent> {

	/* Default values for the parameters */
	protected static final String DEFAULT_SEPARATOR = ";";

	/* The separator of the array options */
	protected static final String OPTIONS_SEPARATOR = ",";

	/* Configurations from the logback.xml */
	private String logOperations;

	private String attrs;
	private String separator = DEFAULT_SEPARATOR;
	private String taskNames;

	/* The attributes to write */
	protected List<String> attributes;

	/* The taskNames to log for */
	protected Set<String> taskNamesList;

	/* The operations to log */
	protected Set<LscModificationType> operations;

	/* Output header to a CSV file? */
	private Boolean outputHeader = false;
	
	/**
	 * Name of the attribute for the dn
	 */
	private final static String DN_STRING = "dn";

	public CsvLayout() {
		operations = new HashSet<LscModificationType>();
		taskNamesList = new HashSet<String>();
	}
	
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
						LscModifications.class.isAssignableFrom(messages[0].getClass()) ) {
			LscModifications lm = (LscModifications) messages[0];


			if (operations.contains(lm.getOperation()) && 
							( taskNamesList.size() == 0 ||
							  taskNamesList.contains(lm.getTaskName().toLowerCase()))) {
				StringBuilder sb = new StringBuilder(1024);

				Map<String, List<Object>> modifications = lm.getModificationsItemsByHash();

				List<Object> values = null;

				for(String attributeName: attributes) {
					/* Does the modification has the attribute ? */
					if (modifications.containsKey(attributeName)) {
						values = modifications.get(attributeName);
						if (values.size() > 0) {
							sb.append(values.get(0));
						}
					} else if (attributeName.equalsIgnoreCase(DN_STRING)) {
						sb.append(lm.getMainIdentifier());
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
	 * Parse logOpertaions string for backward compatibility to configuration old style
	 */
	@Override
	public void start() {
		/* Parse logOperations */
		if (logOperations != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(logOperations, OPTIONS_SEPARATOR);
			String token = null;
			while (st.hasMoreTokens()) {
				token = st.nextToken().toLowerCase();
				LscModificationType op = LscModificationType.getFromDescription(token);
				if (op != null) {
					operations.add(op);
				}
			}
		} else if (operations.isEmpty()){
			/* Add all the operations */
			for(LscModificationType type: LscModificationType.values()) {
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
			addWarn("There is no attributes to write in the CSV file.\nSet the attrs property in the logback configuration file");
		}

		/* Parse task names to log for */
		if (taskNames != null) {
			/* We only add valid options */
			StringTokenizer st = new StringTokenizer(taskNames, CsvLayout.OPTIONS_SEPARATOR);
			String token = null;
			for (; st.hasMoreTokens();) {
				token = st.nextToken().toLowerCase();
				taskNamesList.add(token);
			}
		}
		
		super.start();
	}

    /**
	 * @param logOperations the logOperation to set
	 */
	public void setLogOperations(LscModificationType[] logOperations) {
		operations.addAll(Arrays.asList(logOperations));
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
	 * @param taskNames the taskNames to set
	 */
	public void setTaskNames(String[] taskNames) {
		if(taskNames != null) {
			this.taskNamesList.addAll(Arrays.asList(taskNames));
		}
	}

	/**
	 * @param outputHeader the outputHeader to set
	 */
	public void setOutputHeader(Boolean outputHeader) {
		this.outputHeader = outputHeader;
	}
	
	/* package */ Set<LscModificationType> getLogOperations() {
		return operations;
	}
}
