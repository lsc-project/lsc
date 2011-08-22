package org.lsc.configuration.objects.syncoptions;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import org.lsc.beans.syncoptions.ISyncOptions;
import org.lsc.beans.syncoptions.ISyncOptions.STATUS_TYPE;
import org.lsc.configuration.objects.SyncOptions;
import org.lsc.exception.LscConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("propertiesBasedSyncOptions")
public class PropertiesBasedSyncOptions extends SyncOptions {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesBasedSyncOptions.class);
	private String defaultDelimiter;
	
	private ISyncOptions.STATUS_TYPE defaultPolicy;

	@XStreamImplicit
	private List<PBSOAttribute> attributes;
	
	public PropertiesBasedSyncOptions() {
		defaultPolicy = ISyncOptions.STATUS_TYPE.FORCE;
		attributes = new ArrayList<PBSOAttribute>();
		defaultDelimiter = ";";
	}

	public String getDefaultDelimiter() {
		return defaultDelimiter;
	}

	public void setDefaultDelimiter(String defaultDelimiter) {
		this.defaultDelimiter = defaultDelimiter;
	}

	public ISyncOptions.STATUS_TYPE getDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(ISyncOptions.STATUS_TYPE defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public List<PBSOAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<PBSOAttribute> attributes) {
		this.attributes = attributes;
	}
	
	@Override
	public void load(String syncName, Properties props) {
		Enumeration<Object> en = props.keys();

		// temporary cache to store values read from properties
		Map<String, String> defaultValueStrings = new HashMap<String, String>();
		Map<String, String> createValueStrings = new HashMap<String, String>();
		Map<String, String> forceValueStrings = new HashMap<String, String>();
		
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
			PBSOAttribute attribute = getAttribute(attributeName);
			attributes.add(attribute);
			if (typeName.equalsIgnoreCase("action")) {
				STATUS_TYPE policy = parseSyncType(value);
				if (policy == STATUS_TYPE.UNKNOWN) {
					LOGGER.error("Unable to analyze action type \"{}\" for the following attribute : lsc.{}.{} ! Bypassing ...",
									new Object[]{value, syncName, key});
					continue;
				}
				LOGGER.debug("Adding '{}' sync type for attribute name {}.", value, attributeName);
				if (attributeName.equalsIgnoreCase("default")) {
					defaultPolicy = policy;
				} else {
					attribute.setPolicy(policy);
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
					attribute.setDelimiter(value);
				}
			} else {
				LOGGER.error("Unable to identify attribute option \"{}\" in this name : lsc.{}.{} ! Bypassing.",
								new Object[]{typeName, syncName, key});
				continue;
			}
		}

		// now we've read everything, cut up multiple values using the delimiters
		cutUpValues(defaultValueStrings, createValueStrings, forceValueStrings);

		// use default values for create values if there aren't specific ones
		for (PBSOAttribute attribute : attributes) {
			if (attribute.getCreateValues() == null
					&& attribute.getDefaultValues() != null) {
				attribute.setCreateValues(attribute.getDefaultValues());
			}
		}
	}
	
	public PBSOAttribute getAttribute(String attributeName) {
		for(PBSOAttribute attribute : attributes) {
			if(attribute.getName().equalsIgnoreCase(attributeName)) {
				return attribute;
			}
		}
		PBSOAttribute attr = new PBSOAttribute(attributeName);
		return attr;
	}

	private void cutUpValues(Map<String, String> defaultValues, 
			Map<String, String> createValues,
			Map<String, String> forceValues) {

		for (Entry<String, String> entry : defaultValues.entrySet()) {
			PBSOAttribute attribute = getAttribute(entry.getKey());
			String delimiter = (attribute.getDelimiter() != null ? attribute.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				attribute.setDefaultValues(values);
			}
		}

		for (Entry<String, String> entry : createValues.entrySet()) {
			PBSOAttribute attribute = getAttribute(entry.getKey());
			String delimiter = (attribute.getDelimiter() != null ? attribute.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				attribute.setCreateValues(values);
			}
		}
		
		for (Entry<String, String> entry : forceValues.entrySet()) {
			PBSOAttribute attribute = getAttribute(entry.getKey());
			String delimiter = (attribute.getDelimiter() != null ? attribute.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				attribute.setForceValues(values);
			}
		}
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

	public Class<? extends ISyncOptions> getImplementation() {
		return org.lsc.beans.syncoptions.PropertiesBasedSyncOptions.class;
	}

	@Override
	public void validate() throws LscConfigurationException {
	}
}
