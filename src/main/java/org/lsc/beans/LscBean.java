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
package org.lsc.beans;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.lsc.Configuration;
import org.lsc.service.DataSchemaProvider;
import org.lsc.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic bean used to centralize methods across all beans
 *
 * <P>
 * This object mainly provides methods to store and access "attributes",
 * i.e. named fields and their values.
 * </P>
 * 
 * <P>
 * This implementation ignores the case of attribute names, and uses Java
 * Sets to store lists of values, so values must be unique and are unordered.
 * </P>
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public abstract class LscBean implements IBean {

	private static final long serialVersionUID = 5074469517356449901L;

	private static final Logger LOGGER = LoggerFactory.getLogger(LscBean.class);

	/** The distinguished name. */
	private String distinguishedName;

	/** The attributes map. */
	private Map<String, Set<Object>> attrs;

	/** Data schema related to this bean - must always be set just after initiating the bean */
	private DataSchemaProvider dataSchemaProvider;

	public LscBean() {
		attrs = new HashMap<String, Set<Object>>();
	}

	/**
	 * Get an attribute from its name.
	 *
	 * @param id the name
	 * @return the LDAP attribute
	 */
	public final Attribute getAttributeById(final String id) {
		// use lower case since attribute names are case-insensitive
		return SetUtils.setToAttribute(id, attrs.get(id.toLowerCase()));
	}

	/**
	 * Get an attribute from its name as a Set.
	 *
	 * @param id the name
	 * @return the LDAP attribute
	 */
	public final Set<Object> getAttributeAsSetById(final String id) {
		// use lower case since attribute names are case-insensitive
		return attrs.get(id.toLowerCase());
	}

	/**
	 * Get the <b>first</b> value of an attribute from its name
	 *
	 * @param id The attribute name (case insensitive)
	 * @return String The first value of the attribute, or the empty string ("")
	 * @throws NamingException
	 * @deprecated
	 */
	public final String getAttributeValueById(final String id)
					throws NamingException {
		return getAttributeFirstValueById(id);
	}

	/**
	 * Get the <b>first</b> value of an attribute from its name
	 * 
	 * @param id
	 *            The attribute name (case insensitive)
	 * @return String The first value of the attribute, or the empty string ("")
	 * @throws NamingException
	 */
	public final String getAttributeFirstValueById(final String id)
					throws NamingException {
		List<String> allValues = getAttributeValuesById(id);
		return allValues.size() >= 1 ? allValues.get(0) : "";
	}

	/**
	 * Get all values of an attribute from its name
	 * 
	 * @param id
	 *            The attribute name (case insensitive)
	 * @return List<String> List of attribute values, or an empty list
	 * @throws NamingException
	 */
	public final List<String> getAttributeValuesById(final String id)
					throws NamingException {
		List<String> resultsArray = new ArrayList<String>();

		Set<Object> attributeValues = attrs.get(id.toLowerCase());

		if (attributeValues != null) {
			for (Object value : attributeValues) {
				String stringValue;

				// convert to String because this method only returns Strings
				if (value instanceof byte[]) {
					stringValue = new String((byte[]) value);
				} else {
					stringValue = value.toString();
				}

				resultsArray.add(stringValue);
			}
		}

		return resultsArray;
	}

	/**
	 * Get the attributes name list.
	 *
	 * @return the attributes list
	 */
	public final Set<String> getAttributesNames() {
		return attrs.keySet();
	}

	/**
	 * Set an attribute.
	 * API CHANGE: Do nothing if attribute is empty
	 *
	 * @param attr
	 *                the attribute to set
	 */
	public final void setAttribute(final Attribute attr) {
		if (attr != null && attr.size() > 0) {
			// convert the Attribute into a Set of values
			try {
				setAttribute(attr.getID(), SetUtils.attributeToSet(attr));
			} catch (NamingException e) {
				LOGGER.error("Error storing the attribute {}: {}", attr.getID(), e.toString());
				LOGGER.debug(e.toString(), e);
			}
		}
	}

	/**
	 * Set an attribute.
	 *
	 * @param attrName The attribute name.
	 * @param attrValues A set of values for the attribute.
	 */
	public final void setAttribute(String attrName, Set<Object> attrValues) {
		// use lower case since attribute names are case-insensitive
		attrs.put(attrName.toLowerCase(), attrValues);
	}

	/**
	 * Default distinguish name getter.
	 *
	 * @return the distinguishName
	 */
	public final String getDistinguishName() {
		return distinguishedName;
	}

	/**
	 * Distinguish name getter that makes sure to return the FULL DN (including suffix).
	 *
	 * @return the distinguishName
	 */
	public final String getFullDistinguishedName() {
		if (!distinguishedName.endsWith("," + Configuration.DN_REAL_ROOT)) {
			return distinguishedName + "," + Configuration.DN_REAL_ROOT;
		} else {
			return distinguishedName;
		}
	}

	/**
	 * Default distinguishName setter.
	 *
	 * @param dn
	 *                the distinguishName to set
	 */
	public final void setDistinguishName(final String dn) {
		distinguishedName = dn;
	}

	public void generateDn() throws NamingException {
	}

	/**
	 * Bean pretty printer.
	 *
	 * @return the pretty formatted string to display
	 */
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("dn: ").append(distinguishedName).append('\n');

		for (String key : attrs.keySet()) {
			Set<Object> values = attrs.get(key);
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
	 * @return Object
	 * @throws java.lang.CloneNotSupportedException
	 */
	@Override
	public LscBean clone() throws CloneNotSupportedException {
		try {
			LscBean bean = (LscBean) this.getClass().newInstance();
			bean.setDistinguishName(this.getDistinguishName());

			for (String attributeName : this.getAttributesNames()) {
				bean.setAttribute(attributeName, this.getAttributeAsSetById(attributeName));
			}
			return bean;
		} catch (InstantiationException ex) {
			throw new CloneNotSupportedException(ex.getLocalizedMessage());
		} catch (IllegalAccessException ex) {
			throw new CloneNotSupportedException(ex.getLocalizedMessage());
		}
	}

	public void setDataSchema(DataSchemaProvider dataSchema) {
		this.dataSchemaProvider = dataSchema;
	}

	public DataSchemaProvider getDataSchema() {
		return dataSchemaProvider;
	}

	/**
	 * Manage something there !
	 * @param metaData
	 */
	public static void setMetadata(ResultSetMetaData metaData) {
		// TODO Auto-generated method stub
	}
}
