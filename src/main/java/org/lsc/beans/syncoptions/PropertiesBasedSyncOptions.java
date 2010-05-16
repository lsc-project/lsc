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
package org.lsc.beans.syncoptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.Configuration;
import org.lsc.jndi.JndiModificationType;

/**
 * Synchronization options based on a properties file
 * 
 * This class interprets properties to get detailed options for
 * synchronization, including behavior and values for the general
 * case or attribute by attribute.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public class PropertiesBasedSyncOptions implements ISyncOptions {

	/** the synchronization status cache */
	private Map<String, STATUS_TYPE> status;

	/** the default values cache */
	private Map<String, List<String>> defaultValues;

	/** the create values cache */
	private Map<String, List<String>> createValues;

	/** the force values cache */
	private Map<String, List<String>> forceValues;

	/** When nothing else is available, use the default status */
	private STATUS_TYPE defaultStatus;

	/** Default separator is ";" */
	private String defaultDelimiter = ";";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesBasedSyncOptions.class);

	private String syncName;

	/**
	 * When initiating a new object, needs
	 * &lt;ul&gt;
	 * &lt;li&gt;the synchronization name&lt;/li&gt; to read properties in general configuration file
	 * &lt;li&gt;the general default status&lt;/li&gt; (force if not specified) which will be used if
	 * no default status has been specified for this synchronization name
	 * &lt;ul&gt;
	 * @param syncName
	 */
	public final void initialize(String syncName) {
		this.syncName = syncName;

		status = new HashMap<String, STATUS_TYPE>();
		defaultStatus = STATUS_TYPE.FORCE;

		// temporary cache to store values read from properties
		Map<String, String> defaultValueStrings = new HashMap<String, String>();
		Map<String, String> createValueStrings = new HashMap<String, String>();
		Map<String, String> forceValueStrings = new HashMap<String, String>();

		// temporary cache to store delimiters for each attribute to read {default,create,force}_value
		Map<String, String> delimiters = new HashMap<String, String>();

		// first, just read in all the properties (they are read in a random order, not the order in the file)
		Properties props = Configuration.getAsProperties(Configuration.LSC_SYNCOPTIONS_PREFIX + "." + syncName);
		Enumeration<Object> en = props.keys();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			if (key.equals("")) {
				continue;
			}
			String value = props.getProperty(key);
			StringTokenizer stok = new StringTokenizer(key, ".");
			if (stok.countTokens() != 2) {
				LOGGER.error("Unable to use invalid name : lsc.{}.{} ! Bypassing ...", syncName, key);
				continue;
			}
			String attributeName = stok.nextToken();
			String typeName = stok.nextToken();
			if (typeName.equalsIgnoreCase("action")) {
				STATUS_TYPE st = parseSyncType(value);
				if (st == STATUS_TYPE.UNKNOWN) {
					LOGGER.error("Unable to analyze action type \"{}\" for the following attribute : lsc.{}.{} ! Bypassing ...",
									new Object[]{value, syncName, key});
					continue;
				}
				LOGGER.debug("Adding '{}' sync type for attribute name {}.", value, attributeName);
				if (attributeName.equalsIgnoreCase("default")) {
					defaultStatus = st;
				} else {
					status.put(attributeName.toLowerCase(), st);
				}
			} else if (typeName.equalsIgnoreCase("default_value")) {
				defaultValueStrings.put(attributeName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("create_value")) {
				createValueStrings.put(attributeName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("force_value")) {
				forceValueStrings.put(attributeName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("delimiter")) {
				if (value.length() > 1) {
					LOGGER.error("Invalid delimiter for {} attribute. Delimiters must be 1 character maximum. Ignoring.", attributeName);
					continue;
				}
				if (attributeName.equalsIgnoreCase("default")) {
					defaultDelimiter = value;
				} else {
					delimiters.put(attributeName.toLowerCase(), value);
				}
			} else {
				LOGGER.error("Unable to identify attribute option \"{}\" in this name : lsc.{}.{} ! Bypassing.",
								new Object[]{typeName, syncName, key});
				continue;
			}
		}

		// now we've read everything, cut up multiple values using the delimiters
		defaultValues = cutUpValues(defaultValueStrings, delimiters);
		createValues = cutUpValues(createValueStrings, delimiters);
		forceValues = cutUpValues(forceValueStrings, delimiters);

		// use default values for create values if there aren't specific ones
		for (String attributeName : defaultValues.keySet()) {
			if (createValues.get(attributeName) == null) {
				createValues.put(attributeName, defaultValues.get(attributeName));
			}
		}

	}

	private Map<String, List<String>> cutUpValues(Map<String, String> originalValues, Map<String, String> delimiters) {
		Map<String, List<String>> res = new HashMap<String, List<String>>(originalValues.size());

		for (Entry<String, String> entry : originalValues.entrySet()) {
			String delimiter = getDelimiter(delimiters, entry.getKey());

			// cut up the existing string on the delimiter
			StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
			List<String> values = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				values.add(st.nextToken());
			}

			// store the result
			res.put(entry.getKey(), values);
		}

		return res;
	}

	private String getDelimiter(Map<String, String> delimiters, String attributeName) {
		String delimiter = delimiters.get(attributeName.toLowerCase());
		if (delimiter == null || delimiter.length() == 0) {
			delimiter = defaultDelimiter;
		}
		return delimiter;
	}

	protected final STATUS_TYPE parseSyncType(String value) {
		if (value.equalsIgnoreCase("K")) {
			return STATUS_TYPE.KEEP;
		} else if (value.equalsIgnoreCase("F")) {
			return STATUS_TYPE.FORCE;
		} else if (value.equalsIgnoreCase("M")) {
			return STATUS_TYPE.MERGE;
		} else {
			return STATUS_TYPE.UNKNOWN;
		}
	}

	public final STATUS_TYPE getStatus(final String id, final String attributeName) {
		if (!status.containsKey(attributeName.toLowerCase()) || status.get(attributeName.toLowerCase()) == STATUS_TYPE.UNKNOWN) {
			return defaultStatus;
		} else {
			return status.get(attributeName.toLowerCase());
		}
	}

	public final List<String> getDefaultValues(final String id, final String attributeName) {
		List<String> values = defaultValues.get(attributeName.toLowerCase());
		ArrayList<String> copy = null;
		if (values != null) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public final List<String> getCreateValues(final String id, final String attributeName) {
		List<String> values = createValues.get(attributeName.toLowerCase());
		ArrayList<String> copy = null;
		if (values != null) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public final List<String> getForceValues(final String id, final String attributeName) {
		List<String> values = forceValues.get(attributeName.toLowerCase());
		ArrayList<String> copy = null;
		if (values != null) {
			copy = new ArrayList<String>(values);
		}
		return copy;
	}

	public final Set<String> getCreateAttributeNames() {
		return createValues.keySet();
	}

	public final Set<String> getDefaultValuedAttributeNames() {
		return defaultValues.keySet();
	}

	public final Set<String> getForceValuedAttributeNames() {
		return forceValues.keySet();

	}

	public List<String> getWriteAttributes() {
		String property = Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".dstService.attrs");
		if (property == null) {
			return null;
		}

		List<String> writeAttributes = Arrays.asList(property.split(" "));
		if (writeAttributes.size() == 0) {
			LOGGER.warn("No attributes set to write in the destination. This means that LSC will not change anything! Update {}.{}.dstService.attrs to change this.",
					Configuration.LSC_TASKS_PREFIX, syncName);
		}
		return writeAttributes;
	}

	public String getCreateCondition() {
		String property = Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".condition.create");
		if (property == null) {
			return DEFAULT_CONDITION;
		}
		return property;
	}

	public String getDeleteCondition() {
		String property = Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".condition.delete");
		if (property == null) {
			return DEFAULT_CONDITION;
		}
		return property;
	}

	public String getUpdateCondition() {
		String property = Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".condition.update");
		if (property == null) {
			return DEFAULT_CONDITION;
		}
		return property;
	}

	public String getModrdnCondition() {
		String property = Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".condition.modrdn");
		if (property == null) {
			return DEFAULT_CONDITION;
		}
		return property;
	}

	public String getCondition(JndiModificationType operation) {
		String result = DEFAULT_CONDITION;
		switch (operation) {
			case ADD_ENTRY:
				result = this.getCreateCondition();
				break;
			case MODIFY_ENTRY:
				result = this.getUpdateCondition();
				break;
			case DELETE_ENTRY:
				result = this.getDeleteCondition();
				break;
			case MODRDN_ENTRY:
				result = this.getModrdnCondition();
				break;
		}
		return result;
	}

	/**
	 * Get the setting to generate a DN
	 *
	 * @return String The script to generate a DN
	 */
	public String getDn() {
		return Configuration.getString(Configuration.LSC_TASKS_PREFIX + "." + syncName + ".dn");
	}

	/**
	 * Get the task name
	 *
	 * @return String The current task name.
	 */
	public String getTaskName() {
		return syncName;
	}
}
