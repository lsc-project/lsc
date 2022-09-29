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
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.lsc.LscDatasets;

/**
 * General interface of a bean. A bean in LSC describes an object in a
 * source or destination. It has nothing to do with Java beans (in fact,
 * LSC beans do not have any get- or set- methods).
 *
 * <p>
 * An object is described in this bean as datasets (set of named values)
 * and main identifier.
 * </p>
 * 
 * <p>
 * Most methods are convenience methods to get the values of each dataset.
 * </p>
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jonathan@phillipoux.net&gt;
 */
public interface IBean extends Cloneable, Serializable {

	/**
	 * Get an attribute from its name.
	 * 
	 * @param id
	 *            the name
	 * @return the LDAP attribute
	 * @deprecated
	 */
	@Deprecated
	Attribute getAttributeById(final String id);

    /**
     * Get an dataset from its name.
     * 
     * @param id the name
     * @return the values dataset or null if non existent
     */
	Set<Object> getDatasetById(final String id);

	/**
	 * Get an attribute from its name as a Set.
	 *
	 * @param id the name
	 * @return the LDAP attribute
	 * @deprecated
	 */
	@Deprecated
	Set<Object> getAttributeAsSetById(final String id);

	/**
	 * Get the <b>first</b> value of an attribute from its name
	 * 
	 * @param id
	 *            The attribute name (case insensitive)
	 * @return String The first value of the attribute, or the empty string ("")
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 * @deprecated
	 */
	@Deprecated
	public String getAttributeFirstValueById(final String id)
					throws NamingException;

	/**
	 * Get the <b>first</b> value of an attribute from its name
	 * 
	 * @param id
	 *            The attribute name (case insensitive)
	 * @return String The first value of the attribute, or the empty string ("")
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 */
	public String getDatasetFirstValueById(final String id)
					throws NamingException;

	/**
	 * Get the <b>first</b> binary value of an attribute from its name
	 * 
	 * @param id The attribute name (case insensitive)
	 * @return byte[] The first value of the attribute, or null.
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 */
	public byte[] getDatasetFirstBinaryValueById(final String id) throws NamingException;

	/**
	 * Get all values of an attribute from its name
	 * 
	 * @param id
	 *            The attribute name (case insensitive)
	 * @return List<String> List of attribute values, or an empty list
	 * @throws NamingException attribute definition is missing or has a wrong syntax
	 * @deprecated See getDatasetById(String id)
	 */
	@Deprecated
	public List<String> getAttributeValuesById(final String id)
					throws NamingException;

	/**
	 * Get the attributes name.
     * @deprecated Since LSC 2.0
	 * @return a set containing all the attributes name
	 */
	@Deprecated
	Set<String> getAttributesNames();

	/**
	 * Set an attribute.
     * @deprecated Since LSC 2.0
	 * @param attr
	 *            the attribute to set
	 */
	@Deprecated
	void setAttribute(Attribute attr);

	/**
	 * Set an attribute.
     * @deprecated Since LSC 2.0
	 * @param attrName The attribute name.
	 * @param attrValues A set of values for the attribute.
	 */
	@Deprecated
	void setAttribute(String attrName, Set<Object> attrValues);

    /**
     * Set an dataset.
     * 
     * @param name The dataset name.
     * @param values A set of values for this dataset.
     */
    public void setDataset(String name, Set<Object> values);
    
	/**
	 * Get the distinguished name.
     * @deprecated Since LSC 2.0 - switch to getMainIdentifier()
	 * @return the distinguished name
	 */
	@Deprecated
	String getDistinguishedName();

	/**
	 * Set the distinguished name.
     * @deprecated Since LSC 2.0 - switch to setMainIdentifier(String mainIdentifier)
	 * @param dn The distinguishedName to set
	 */
	@Deprecated
	void setDistinguishName(String dn);

	void setDatasets(LscDatasets datasets);
	
	LscDatasets datasets();
	
	String getMainIdentifier();

	void setMainIdentifier(String mainIdentifier);

	/**
	 * Clone this object.
	 * 
	 * @return Object
	 * @throws CloneNotSupportedException 
	 */
	IBean clone() throws CloneNotSupportedException;
}
