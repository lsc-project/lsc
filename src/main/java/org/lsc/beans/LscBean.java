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
 *         Sebastien Bahloul&lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau&lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke&lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser&lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.beans;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.SerializationUtils;
import org.lsc.LscDatasets;
import org.lsc.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic bean used to centralize methods across all beans
 * 
 * <P>
 * This object mainly provides methods to store and access "attributes", i.e.
 * named fields and their values.
 * </P>
 * 
 * <P>
 * This implementation ignores the case of attribute names, and uses Java Sets
 * to store lists of values, so values must be unique and are unordered.
 * </P>
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public abstract class LscBean implements IBean, Serializable {

	private static final long serialVersionUID = 5074469517356449901L;

	private static final Logger LOGGER = LoggerFactory.getLogger(LscBean.class);

	/** The distinguished name. */
	private String mainIdentifier;

	/** The attributes map. */
	private Map<String, Set<Object>> datasets;

	// /** Data schema related to this bean - must always be set just after
	// initiating the bean */
	// private DataSchemaProvider dataSchemaProvider;

	public LscBean() {
		datasets = new HashMap<String, Set<Object>>();
	}

	/**
	 * Get an dataset from its name.
	 * 
	 * @param id the name
	 * @return the values dataset or null if non existent
	 */
	@Override
	public final Set<Object> getDatasetById(final String id) {
		// use lower case since attribute names are case-insensitive
		return datasets.get(id.toLowerCase());
	}

	@Override
	@Deprecated
	public final Attribute getAttributeById(final String id) {
		LOGGER.warn("The method getAttributeById() is deprecated and will be removed in a future version of LSC. Please use getDatasetById() instead.");
		Set<Object> values = getDatasetById(id);
		return (values != null ? new BasicAttribute(id, values) : null);
	}

	/**
	 * Get an attribute from its name as a Set.
	 * 
	 * @param id
	 *            the name
	 * @return the LDAP attribute
	 */
	public final Set<Object> getDatasetAsSetById(final String id) {
		// use lower case since attribute names are case-insensitive
		return datasets.get(id.toLowerCase());
	}

	@Override
	@Deprecated
	public final Set<Object> getAttributeAsSetById(final String id) {
		LOGGER.warn("The method getAttributeAsSetById() is deprecated and will be removed in a future version of LSC. Please use getDatasetAsSetById() instead.");
		return getDatasetAsSetById(id);
	}

	/**
	 * Get the <b>first</b> value of an attribute from its name
	 * 
	 * @param id The attribute name (case insensitive)
	 * @return String The first value of the attribute, or the empty string ("")
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 */
	@Override
	public final String getDatasetFirstValueById(final String id)
			throws NamingException {
		List<String> allValues = getDatasetValuesById(id);
		return allValues.size() >= 1 ? allValues.get(0) : "";
	}

	@Override
	@Deprecated
	public final String getAttributeFirstValueById(final String id)
			throws NamingException {
		LOGGER.warn("The method getAttributeFirstValueById() is deprecated and will be removed in a future version of LSC. Please use getDatasetFirstValueById() instead.");
		return getDatasetFirstValueById(id);
	}

	/**
	 * Get the <b>first</b> binary value of an attribute from its name
	 * 
	 * @param id The attribute name (case insensitive)
	 * @return String The first value of the attribute, or null.
	 * @throws NamingException
	 */
	@Override
	public byte[] getDatasetFirstBinaryValueById(String id) throws NamingException {
		List<byte[]> allValues = getDatasetBinaryValuesById(id);
		return allValues.size() >= 1 ? allValues.get(0) : null;
	}

	/**
	 * Get all values of an attribute from its name
	 * 
	 * @param id The attribute name (case insensitive)
	 * @return List<String> List of attribute values, or an empty list
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 */
	public final List<String> getDatasetValuesById(final String id)
			throws NamingException {
		List<String> resultsArray = new ArrayList<String>();

		Set<Object> attributeValues = datasets.get(id.toLowerCase());

		if (attributeValues != null) {
			for (Object value : attributeValues) {
				if (value != null) {
					String stringValue;

					// convert to String because this method only returns
					// Strings
					if (value instanceof byte[]) {
						stringValue = new String((byte[]) value);
					} else {
						stringValue = value.toString();
					}

					resultsArray.add(stringValue);
				}
			}
		}

		return resultsArray;
	}

	/**
	 * Get all binary values of an attribute from its name
	 * 
	 * @param id The attribute name (case insensitive)
	 * @return List of attribute values, or an empty list
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 */
	public final List<byte[]> getDatasetBinaryValuesById(final String id) throws NamingException {
		List<byte[]> resultsArray = new ArrayList<byte[]>();

		Set<Object> attributeValues = datasets.get(id.toLowerCase());

		if (attributeValues != null) {
			for (Object value : attributeValues) {
				if (value != null) {
					if (value instanceof byte[]) {
						resultsArray.add((byte[]) value);
					} else {
						resultsArray.add(value.toString().getBytes());
					}
				}
			}
		}

		return resultsArray;
	}

	@Override
	public final List<String> getAttributeValuesById(final String id)
			throws NamingException {
		return getDatasetValuesById(id);
	}

	/**
	 * Get the attributes name list.
	 * 
	 * @return the attributes list
	 */
	public final Set<String> getDatasetsNames() {
		return datasets.keySet();
	}

	@Override
	public final Set<String> getAttributesNames() {
		return getDatasetsNames();
	}

	/**
	 * Set an attribute. API CHANGE: Do nothing if attribute is empty
	 * 
	 * @param attr the attribute to set
	 */
	@Override
	public final void setAttribute(final Attribute attr) {
		if (attr != null && attr.size() > 0) {
			// convert the Attribute into a Set of values
			try {
				setAttribute(attr.getID(), SetUtils.attributeToSet(attr));
			} catch (NamingException e) {
				LOGGER.error("Error storing the attribute {}: {}",
						attr.getID(), e.toString());
				LOGGER.debug(e.toString(), e);
			}
		}
	}

	/**
	 * Set a dataset.
	 * 
	 * @param name The dataset name.
	 * @param values A set of values for this dataset.
	 */
	@Override
	public final void setDataset(String name, Set<Object> values) {
		// use lower case since attribute names are case-insensitive
		datasets.put(name.toLowerCase(), values);
	}

	@Override
	@Deprecated
	public final void setAttribute(String name, Set<Object> values) {
		setDataset(name, values);
	}

	/**
	 * Default distinguished name getter.
	 * 
	 * @return the distinguishedName
	 */
	@Override
	public final String getMainIdentifier() {
		return mainIdentifier;
	}

	@Override
	@Deprecated
	public final String getDistinguishedName() {
		return getMainIdentifier();
	}

	/**
	 * Default distinguished name getter.
	 * 
	 * @return the distinguishedName
	 */
	@Deprecated
	public final String getDN() {
		return getMainIdentifier();
	}

	/**
	 * Distinguish name getter that makes sure to return the FULL DN 
	 * The mainIdentifier is always including the suffix, so this method
	 * is the same as getDistinguishedName()
	 *
	 * @return the distinguishedName
	 */
	@Deprecated
	public final String getFullDistinguishedName() {
		return getMainIdentifier();
	}

	/**
	 * Default main object identifier setter.
	 * 
	 * @param id
	 *            The main identifier to set
	 */
	@Override
	public final void setMainIdentifier(final String id) {
		mainIdentifier = id;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	@Override
	public final void setDistinguishName(final String dn) {
		if (dn != null) {
			setMainIdentifier(dn);
		}
	}

	/**
	 * Bean pretty printer.
	 * 
	 * @return the pretty formatted string to display
	 */
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ").append(mainIdentifier).append('\n');

		for (String key : datasets.keySet()) {
			Set<Object> values = datasets.get(key);
			if (values != null) {
				sb.append("=> " + key);
				for (Object value : values) {
					sb.append(" - ").append(value).append('\n');
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Clone this Bean object.
	 * 
	 * @return Object
	 * @throws java.lang.CloneNotSupportedException can't clone
	 */
	@Override
	public LscBean clone() throws CloneNotSupportedException {
		try {
			LscBean bean = this.getClass().newInstance();
			bean.setMainIdentifier(this.getMainIdentifier());

			for (String attributeName : this.getAttributesNames()) {
				bean.setAttribute(attributeName,
						this.getDatasetAsSetById(attributeName));
			}
			return bean;
		} catch (InstantiationException ex) {
			throw new CloneNotSupportedException(ex.getLocalizedMessage());
		} catch (IllegalAccessException ex) {
			throw new CloneNotSupportedException(ex.getLocalizedMessage());
		}
	}

	// public void setDataSchema(DataSchemaProvider dataSchema) {
	// this.dataSchemaProvider = dataSchema;
	// }
	//
	// public DataSchemaProvider getDataSchema() {
	// return dataSchemaProvider;
	// }

	/**
	 * Manage something there !
	 * 
	 * @param metaData
	 */
	public static void setMetadata(ResultSetMetaData metaData) {
		// TODO Auto-generated method stub
	}

	/**
	 * Set a bean from an LDAP entry
	 * 
	 * @param entry
	 *            the LDAP entry
	 * @param baseDn
	 *            the base Dn used to set the right Dn
	 * @param c
	 *            class to instantiate
	 * @return the bean
	 * @throws NamingException
	 *             thrown if a directory exception is encountered while looking
	 *             at the entry
	 */
	public static LscBean getInstance(final SearchResult entry,
			final String baseDn, final Class<?> c) throws NamingException {
		try {
			if (entry != null) {
				LscBean ab = (LscBean) c.newInstance();
				String dn = entry.getName();

				if ((dn.length() > 0) && (dn.charAt(0) == '"')
						&& (dn.charAt(dn.length() - 1) == '"')) {
					dn = dn.substring(1, dn.length() - 1);
				}
				
				if (dn.startsWith("ldap://")) {
					ab.setDistinguishName(entry.getNameInNamespace());
				} else {
					// Manually concat baseDn because getNameInNamespace returns
					// a differently escaped DN, causing LSC to detect a MODRDN
					if ((baseDn != null) && (baseDn.length() > 0)) {
						if (dn.length() > 0) {
							ab.setDistinguishName(dn + "," + baseDn);
						} else {
							ab.setDistinguishName(baseDn);
						}
					} else {
						ab.setDistinguishName(dn);
					}
				}

				NamingEnumeration<?> ne = entry.getAttributes().getAll();

				while (ne.hasMore()) {
					ab.setAttribute((Attribute) ne.next());
				}

				return ab;
			} else {
				return null;
			}
		} catch (InstantiationException ie) {
			LOGGER.error(ie.toString());
			LOGGER.debug(ie.toString(), ie);
		} catch (IllegalAccessException iae) {
			LOGGER.error(iae.toString());
			LOGGER.debug(iae.toString(), iae);
		}

		return null;
	}

	@Override
	public LscDatasets datasets() {
		return new LscDatasets(datasets);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setDatasets(LscDatasets datasets) {
		Map<String, Set<Object>> tmp = new HashMap<String, Set<Object>>();
		for (String name : datasets.getAttributesNames()) {
			Object values = datasets.getDatasets().get(name);
			if (values instanceof Set<?>) {
				tmp.put(name, (Set<Object>) values);
			} else if (values instanceof List<?>) {
				Set<Object> valuesAsSet = new LinkedHashSet<Object>();
				valuesAsSet.addAll((List<?>) values);
				tmp.put(name, valuesAsSet);
			} else if (values instanceof String) {
				Set<Object> valuesAsSet = new LinkedHashSet<Object>();
				valuesAsSet.add(values);
				tmp.put(name, valuesAsSet);
            } else if (values instanceof Boolean) {
                Set<Object> valuesAsSet = new LinkedHashSet<Object>();
                valuesAsSet.add(values.toString());
                tmp.put(name, valuesAsSet);
            } else if (values instanceof Integer) {
                Set<Object> valuesAsSet = new LinkedHashSet<Object>();
                valuesAsSet.add("" + values);
                tmp.put(name, valuesAsSet);
			} else {
				LOGGER.warn("Appending unknown type inside lsc bean as Set: "
						+ values);
				tmp.put(name, (Set<Object>) values);
			}
		}
		this.datasets = tmp;
	}

	public byte[] getDatasetsBytes() {
		return SerializationUtils.serialize((Serializable) datasets);
	}
}
