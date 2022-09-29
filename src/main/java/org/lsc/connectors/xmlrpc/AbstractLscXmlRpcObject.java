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
package org.lsc.connectors.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

/**
 * This methods contains all the required method to handle encoding
 * and decoding of values inside the XmlRpc protocol
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractLscXmlRpcObject {

	protected String message;
	
	public static final String LSC_XMLRPC_PROTOCOL_VERSION = "1.0"; 
	public static final String LSC_XMLRPC_SERVICE_NAME = "LSC-XMLRPC"; 

	/* Error codes */
	public static int RESULT_SUCCESS 					= 200;
	public static int AUTHENTICATION_ERROR 				= 401;
	public static int UNHANDLED_ERROR	 				= 500;
	public static int BAD_SERVICE_NAME	 				= 510;
	public static int BAD_PROTOCOL_VERSION 				= 511;
	
	/**
	 * Encode a list of parameters starting with the result code, followed by the
	 * protocol version and ending by the parameters as an embedded array
	 * @param code the result code
	 * @param parameters the parameters to embed
	 * @return the object list to return through XmlRpc
	 */
	public List<Object> encodeResult(int code, Object... parameters) {
		List<Object> result = new ArrayList<Object>();
		result.add("" + code);
		result.add("" + LSC_XMLRPC_PROTOCOL_VERSION);
		List<Object> data = new ArrayList<Object>(); 
		if(parameters.length > 0) {
			for(Object parameter: parameters) {
				data.add(parameter);
			}
		}
		result.add(data);
		return result;
	}
	

	/**
	 * Check in the request that the service and the version are correct
	 * @param service the service name to check
	 * @param version the protocol version to check
	 * @return an error code, RESULT_SUCCESS if correct, something else otherwise 
	 */
	protected int checkRequestService(String service, String version) {
		if(!service.equals(LSC_XMLRPC_SERVICE_NAME)) {
			return BAD_SERVICE_NAME;
		} else if(!version.equals(LSC_XMLRPC_PROTOCOL_VERSION)) {
			return BAD_PROTOCOL_VERSION;
		} else {
			return RESULT_SUCCESS;
		}
	}
	
	/**
	 * Check in the response that the result code and the protocol version are correct
	 * @param service the result code to check
	 * @param version the protocol version to check
	 * @return an error code, RESULT_SUCCESS if correct, something else otherwise 
	 */
	protected int checkResultService(String code, String version) {
		if(!code.equals("" + RESULT_SUCCESS)) {
			return Integer.parseInt(code);
		} else if(!version.equals(LSC_XMLRPC_PROTOCOL_VERSION)) {
			return BAD_PROTOCOL_VERSION;
		} else {
			return RESULT_SUCCESS;
		}
	}

	/**
	 * Error message accessor
	 * @return the error message
	 */
	public String getErrorMessage() {
		return message;
	}
	
	/**
	 * Parse the response to get the parameters as a String list
	 * @param result the response
	 * @return the string list or null if something goes wrong !
	 */
	public List<String> decodeStringListResult(Object[] result) {
		List<String> ret = new ArrayList<String>();
		if(result.length != 3) {
			return null;
		} else {
			int resultCode = checkResultService((String)result[0], (String)result[1]); 
			if(resultCode == RESULT_SUCCESS && Object[].class.isInstance(result[2])) {
				for(Object data: (Object[])result[2]) {
					if(String.class.isInstance(data)) {
						ret.add((String) data);
					} else {
						return null;
					}
				}
				return ret;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Parse the response to get the parameters as an objects list
	 * @param result the response
	 * @return the objects list or null if something goes wrong !
	 */
	public List<Object> decodeObjectListResult(Object[] result) {
		List<Object> ret = new ArrayList<Object>();
		if(result.length != 3) {
			return null;
		} else {
			int resultCode = checkResultService((String)result[0], (String)result[1]); 
			if(resultCode == RESULT_SUCCESS && Object[].class.isInstance(result[2])) {
				for(Object data: (Object[])result[2]) {
					ret.add(data);
				}
				return ret;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Parse the response to get a boolean result 
	 * @param result the request
	 * @return the boolean result
	 */
	public boolean decodeBooleanResult(Object[] result) {
		if(result.length != 3) {
			return false;
		} else {
			int resultCode = checkResultService((String)result[0], (String)result[1]); 
			if(resultCode == RESULT_SUCCESS && Boolean.class.isInstance(result[2])) {
				return (Boolean) result[2];
			} else {
				return false;
			}
		}
	}
	
	public Object[] encodeRequest(Object... parameters) {
		Object[] params = new Object[parameters.length + 2];
		int index = 0;
		params[index++] = LSC_XMLRPC_PROTOCOL_VERSION;
		params[index++] = LSC_XMLRPC_SERVICE_NAME;
		for(Object parameter : parameters) {
			params[index++] = parameter; 
		}
		return params;
	}
	
	public Object encodeMap(List<Attribute> attributes) {
		Map<String, Object> values = new HashMap<String, Object>();
		for(Attribute attribute: attributes) {
			values.put(attribute.getID(), encodeAttributeValue(attribute));
		}
		return values;
	}

	public Object encodeAttributeValue(Attribute attribute) {
		List<Object> values = new ArrayList<Object>();
		try {
			NamingEnumeration<?> enumeration = attribute.getAll();
			while(enumeration.hasMore()) {
				values.add(enumeration.nextElement());
			}
		} catch (NamingException ne) {
			System.err.println(ne);
			ne.printStackTrace();
		}
		return values;
	}


}
