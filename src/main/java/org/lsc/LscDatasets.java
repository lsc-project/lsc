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
package org.lsc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.FilterEncoder;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.lsc.utils.CaseIgnoreStringHashMap;

/**
 * Class used to represent a set of datasets and their values.
 * 
 * @author rschermesser
 */
public class LscDatasets implements Serializable {

	/**	 */
	private static final long serialVersionUID = 746918525778409642L;
	
	/** The heart of this class - a map of datasets names to values */
	protected Map<String, Object> values;

	public LscDatasets() {
		values = new CaseIgnoreStringHashMap<Object>();
	}

	public LscDatasets(Map<String, ?> values) {
		this.values = new CaseIgnoreStringHashMap<Object>(values);
	}

	@SuppressWarnings("rawtypes")
	public String getStringValueAttribute(String attribute) {
		Object value = values.get(attribute);
		if(value instanceof Set && ((Set)value).size() > 0) {
			return ((Set)value).iterator().next().toString();
		} else if(value instanceof List && ((List)value).size() > 0){
			return ((List)value).get(0).toString();
		} else if(value instanceof byte[]) {
			return new String((byte[])value);
		}
		return value != null ? value.toString() : null;
	}
	
	public String getValueForFilter(String attribute) {
		Object value = values.get(attribute);
		if(value instanceof byte[]) {
			Value binValue = new Value((byte[])value);
			SimpleNode<byte[]> filter = new EqualityNode<>(new AttributeType(attribute), binValue);
			return filter.getEscapedValue();
		} else {
			String stringValue = getStringValueAttribute(attribute);
			if (stringValue != null) {
				return FilterEncoder.encodeFilterValue(stringValue);
			} else {
				return null;
			}
		}
	}

	public Integer getIntegerValueAttribute(String attribute) {
		return (Integer) values.get(attribute);
	}

	public Boolean getBooleanValueAttribute(String attribute) {
		return (Boolean) values.get(attribute);
	}

	@SuppressWarnings("unchecked")
	public List<Object> getListValueAttribute(String attribute) {
		return (List<Object>) values.get(attribute);
	}

	@SuppressWarnings("unchecked")
	public List<String> getListStringValueAttribute(String attribute) {
		return (List<String>) values.get(attribute);
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getListIntegerValueAttribute(String attribute) {
		return (List<Integer>) values.get(attribute);
	}

	public List<String> getAttributesNames() {
		return new ArrayList<String>(values.keySet());
	}

	/**
	 * Get the datasets' values
	 * 
	 * @return Map of the datasets, indexed by name
	 */
	public Map<String, Object> getDatasets() {
		return values;
	}

	public void setDatasets(Map<String, Object> values) {
		this.values = values;
	}

	public void put(String key, Object value) {
		this.values.put(key, value);
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
