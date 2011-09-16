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
import org.lsc.configuration.objects.Task;
import org.lsc.configuration.objects.connection.directory.Ldap;
import org.lsc.exception.LscConfigurationException;
import org.lsc.jndi.JndiServices;
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
	private List<PBSODataset> datasets;
	
	public PropertiesBasedSyncOptions() {
		defaultPolicy = ISyncOptions.STATUS_TYPE.FORCE;
		datasets = new ArrayList<PBSODataset>();
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

	public List<PBSODataset> getDatasets() {
		return datasets;
	}

	public void setDatasets(List<PBSODataset> datasets) {
		this.datasets = datasets;
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
			String datasetName = stok.nextToken();
			String typeName = stok.nextToken();
			PBSODataset dataset = getDataset(datasetName);
			datasets.add(dataset);
			if (typeName.equalsIgnoreCase("action")) {
				STATUS_TYPE policy = parseSyncType(value);
				if (policy == STATUS_TYPE.UNKNOWN) {
					LOGGER.error("Unable to analyze action type \"{}\" for the following dataset : lsc.{}.{} ! Bypassing ...",
									new Object[]{value, syncName, key});
					continue;
				}
				LOGGER.debug("Adding '{}' sync type for dataset name {}.", value, datasetName);
				if (datasetName.equalsIgnoreCase("default")) {
					defaultPolicy = policy;
				} else {
					dataset.setPolicy(policy);
				}
			} else if (typeName.equalsIgnoreCase("default_value")) {
				defaultValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("create_value")) {
				createValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("force_value")) {
				forceValueStrings.put(datasetName.toLowerCase(), value);
			} else if (typeName.equalsIgnoreCase("delimiter")) {
				if (value.length() > 1) {
					LOGGER.error("Invalid delimiter for {} dataset. Delimiters must be 1 character maximum. Ignoring.", datasetName);
					continue;
				}
				if (datasetName.equalsIgnoreCase("default")) {
					defaultDelimiter = value;
				} else {
					dataset.setDelimiter(value);
				}
			} else {
				LOGGER.error("Unable to identify dataset option \"{}\" in this name : lsc.{}.{} ! Bypassing.",
								new Object[]{typeName, syncName, key});
				continue;
			}
		}

		// now we've read everything, cut up multiple values using the delimiters
		cutUpValues(defaultValueStrings, createValueStrings, forceValueStrings);

		// use default values for create values if there aren't specific ones
		for (PBSODataset dataset : datasets) {
			if (dataset.getCreateValues() == null
					&& dataset.getDefaultValues() != null) {
				dataset.setCreateValues(dataset.getDefaultValues());
			}
		}
	}
	
	public PBSODataset getDataset(String datasetName) {
		if(datasets != null) {
			for(PBSODataset dataset : datasets) {
				if(dataset.getName().equalsIgnoreCase(datasetName)) {
					return dataset;
				}
			}
		}
		PBSODataset attr = new PBSODataset(datasetName);
		attr.setPolicy(this.defaultPolicy);
		return attr;
	}

	private void cutUpValues(Map<String, String> defaultValues, 
			Map<String, String> createValues,
			Map<String, String> forceValues) {

		for (Entry<String, String> entry : defaultValues.entrySet()) {
			PBSODataset dataset = getDataset(entry.getKey());
			String delimiter = (dataset.getDelimiter() != null ? dataset.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				dataset.setDefaultValues(values);
			}
		}

		for (Entry<String, String> entry : createValues.entrySet()) {
			PBSODataset dataset = getDataset(entry.getKey());
			String delimiter = (dataset.getDelimiter() != null ? dataset.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				dataset.setCreateValues(values);
			}
		}
		
		for (Entry<String, String> entry : forceValues.entrySet()) {
			PBSODataset dataset = getDataset(entry.getKey());
			String delimiter = (dataset.getDelimiter() != null ? dataset.getDelimiter() : defaultDelimiter);

			if(delimiter != null && entry.getValue() != null) {
				// cut up the existing string on the delimiter
				StringTokenizer st = new StringTokenizer(entry.getValue(), delimiter);
				List<String> values = new ArrayList<String>();
				while (st.hasMoreTokens()) {
					values.add(st.nextToken());
				}
				dataset.setForceValues(values);
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
	public void validate(Task task) throws LscConfigurationException {
		if(task.getDestinationService().getConnection() instanceof Ldap) {
			String contextDn = JndiServices.getInstance((Ldap)task.getDestinationService().getConnection()).getContextDn();
			if(!getMainIdentifier().endsWith(contextDn)) {
				LOGGER.warn("Your main identifier will be used as a DN (" + getMainIdentifier() + ") in LDAP destination service and does not end with the context dn (" + contextDn + "). This is probably an error ! For LSC 1.X users, this is part of the changelog to 2.X.");
			}
		} 
	}
}
