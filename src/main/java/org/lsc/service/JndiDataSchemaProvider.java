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
package org.lsc.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.AbstractGenerator;
import org.lsc.jndi.JndiServices;
import org.lsc.jndi.parser.LdapAttributeType;
import org.lsc.jndi.parser.LdapObjectClass;

/**
 * This class provides the data schema by reading the directory schema
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiDataSchemaProvider implements DataSchemaProvider {

    /** This is the local logger. */
    public static final Logger LOGGER = 
        LoggerFactory.getLogger(AbstractGenerator.class);

    /** This is the list of object classes available in the directory. */
    private List<String> objectClasses;

    /** This is the list of attribute types available in the directory. */
    private List<String> attributeTypes;
    
    private LdapObjectClass ldapObjectClass;
    private Map<String, LdapAttributeType> ldapAttributeTypes; 


    /**
     * Initialize the data schema provider by reading the directory schema and 
     * looking for the specified objectclass name
     * @param js
     * @param className
     * @throws NamingException 
     */
    public JndiDataSchemaProvider(JndiServices js, String className) throws NamingException {

        Map<String, List<String>> ocsTemp = js.getSchema(new String[] {
                "objectclasses"
        });
        Map<String, List<String>> atsTemp = js.getSchema(new String[] {
                "attributetypes"
        });

        if ((ocsTemp == null) || (ocsTemp.keySet().size() == 0)
                || (atsTemp == null) || (atsTemp.keySet().size() == 0)) {
            LOGGER.error("Unable to read objectclasses or attributetypes in ldap schema! Exiting...");
            return;
        }

        objectClasses = filterNames(ocsTemp.values().iterator().next());
        attributeTypes = filterNames(atsTemp.values().iterator().next());

        ldapAttributeTypes = new HashMap<String, LdapAttributeType>();
        Iterator<String> atIter = attributeTypes.iterator();
        while (atIter.hasNext()) {
            String atStr = atIter.next();
            LdapAttributeType lat = LdapAttributeType.parse(atStr);
            if (lat != null) {
            	ldapAttributeTypes.put(lat.getName(), lat);
            }
        }

        
        Iterator<String> ocIter = objectClasses.iterator();
        while (ocIter.hasNext() && ldapObjectClass == null) {
            String ocStr = ocIter.next();
            LdapObjectClass loc = LdapObjectClass.parse(ocStr, ldapAttributeTypes);
            if (loc != null
                    && loc.getName().compareToIgnoreCase(className) == 0) {
                ldapObjectClass = loc;
            }
        }

	}
	
    /**
     * Filter the attribute and object classes names
     * @param names List of names
     * @return list of filtered names
     */
    private List<String> filterNames(List<String> names) {
    	List<String> filteredNames = new ArrayList<String>();
    	Iterator<String> namesIter = names.iterator();
    	while(namesIter.hasNext()) {
    		String name = namesIter.next();
    		String filteredName = filterName(name);
    		if(filteredName != null) {
    			filteredNames.add(filteredName);
    		} else {
    			LOGGER.error("Name invalid: {}. Attributes or object class not generated !!!", name);
    		}
    	}
    	return filteredNames;
    	
	}

    /**
     * Filter name according to attribute or object class 
     * @param name the originale name
     * @return the filtered name or null if not matching
     */
	public String filterName(String name) {
		String REGEX = "^\\p{Alpha}[\\w]*$";
		Pattern p = Pattern.compile(REGEX);
		Matcher m = p.matcher(name);
		if(m.matches()) {
			return null;
		} else {
			return name;
		}
	}

	/**
	 * List of attributes authorized for this object
	 * @return the attributes name collection
	 */
	public Collection<String> getElementsName() {
		return ldapAttributeTypes.keySet();
	}

	/**
	 * TODO Refactor to return the good type
	 */
	public Class<?> getElementSingleType(String elementName) {
		return String.class;
	}

	/**
	 * Unsupported at this time !
	 */
	public boolean isElementMandatory(String elementName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return true if the attribute is not marked as SINGLE-VALUE
	 * @return the attribute multi-valued status
	 * @throws MissingFormatArgumentException is the attribute is unknown
	 */
	public boolean isElementMultivalued(String elementName) {
		if(!ldapObjectClass.getMonoAttrs().contains(elementName)
			&& !ldapObjectClass.getMultiAttrs().contains(elementName)) {
			throw new MissingFormatArgumentException("Unknown attribute: " + elementName);
		}
		return ldapObjectClass.getMultiAttrs().contains(elementName);
	}
}
