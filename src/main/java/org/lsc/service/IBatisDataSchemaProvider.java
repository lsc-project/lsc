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
package org.lsc.service;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the data schema by reading the request schema via iBatis
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class IBatisDataSchemaProvider implements DataSchemaProvider {

//	private ResultSetMetaData metadata;

	private Map<String, String> metadataCache;

	/** This is the local logger. */
	public static final Logger LOGGER = LoggerFactory.getLogger(IBatisDataSchemaProvider.class);

	public IBatisDataSchemaProvider(ResultSetMetaData metadata) {
//    	this.metadata = metadata;
		metadataCache = new HashMap<String, String>();
		try {
			for (int i = 1; i <= metadata.getColumnCount(); i++) {
				metadataCache.put(metadata.getColumnLabel(i), metadata.getColumnTypeName(i));
			}
		} catch (SQLException e) {

		}
	}

	public Collection<String> getElementsName() {
		return metadataCache.keySet();
	}

	public Class<?> getElementSingleType(String elementName) {
		return String.class;
	}

	public boolean isElementMandatory(String elementName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Never return true, or maybe in a 3D database implementation :)
	 * 
	 * @param elementName Name of the element
	 * @return false
	 */
	public boolean isElementMultivalued(String elementName) {
		return false;
	}
}
