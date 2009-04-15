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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
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

    private static Logger LOGGER = Logger.getLogger(PropertiesBasedSyncOptions.class);
    
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
        status = new HashMap<String, STATUS_TYPE>();
        defaultValues = new HashMap<String, List<String>>();
        createValues = new HashMap<String, List<String>>();
        forceValues = new HashMap<String, List<String>>();
        defaultStatus = STATUS_TYPE.FORCE;
        this.syncName = syncName; 

        /* temporary cache to store delimiters for each attribute to read {default,create}_value */
        Map<String, String> delimiters = new HashMap<String, String>();

        Properties props = Configuration.getAsProperties("lsc.syncoptions." + syncName);
        Enumeration<Object> en = props.keys();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if("".equals(key)) continue;
            String value = props.getProperty(key);
            StringTokenizer stok = new StringTokenizer(key, ".");
            if (stok.countTokens() != 2) {
                LOGGER.error("Unable to use invalid name : lsc."
                        + syncName + "." + key + " ! Bypassing ...");
                continue;
            }
            String attributeName = stok.nextToken();
            String typeName = stok.nextToken();
            if (typeName.equalsIgnoreCase("action")) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Adding '" + value + "' sync type for attribute name " + attributeName + ".");
                STATUS_TYPE st = parseSyncType(value);
                if(st == STATUS_TYPE.UNKNOWN) {
                    LOGGER.error("Unable to analyze action type \"" + value + "\" for the following attribute : lsc."
                            + syncName + "." + key + " ! Bypassing ...");
                    continue;
                }
                if(attributeName.equalsIgnoreCase("default")) {
                    defaultStatus = st;
                } else {
                    status.put(attributeName.toLowerCase(), st);
                }
            } else if (typeName.equalsIgnoreCase("default_value")) {
                String delimiter = delimiters.get(attributeName.toLowerCase()) != null ? delimiters.get(attributeName.toLowerCase()) : ";";
                StringTokenizer st = new StringTokenizer(value, delimiter);
                List<String> values = new ArrayList<String>();
                for (int i = 0; st.hasMoreTokens(); i++) {
                    values.add(st.nextToken());
                }
                defaultValues.put(attributeName.toLowerCase(), values);
                /* TODO: don't wipe out existing createValue */
                createValues.put(attributeName.toLowerCase(), values);
            } else if (typeName.equalsIgnoreCase("create_value")) {
                String delimiter = delimiters.get(attributeName.toLowerCase()) != null ? delimiters.get(attributeName.toLowerCase()) : ";";
                StringTokenizer st = new StringTokenizer(value, delimiter);
                List<String> values = new ArrayList<String>();
                for (int i = 0; st.hasMoreTokens(); i++) {
                    values.add(st.nextToken());
                }
                createValues.put(attributeName.toLowerCase(), values);
            } else if (typeName.equalsIgnoreCase("force_value")) {
                String delimiter = delimiters.get(attributeName.toLowerCase()) != null ? delimiters.get(attributeName.toLowerCase()) : ";";
                StringTokenizer st = new StringTokenizer(value, delimiter);
                List<String> values = new ArrayList<String>();
                for (int i = 0; st.hasMoreTokens(); i++) {
                    values.add(st.nextToken());
                }
                forceValues.put(attributeName.toLowerCase(), values);
            } else if (typeName.equalsIgnoreCase("delimiter")) {
                delimiters.put(attributeName.toLowerCase(), value);
            } else {
                LOGGER.error("Unable to identify attribute option \"" + typeName + "\" in this name : lsc." + syncName
                        + "." + key + " ! Bypassing ...");
                continue;
            }
        }
    }

    protected final STATUS_TYPE parseSyncType(String value) {
        if(value.equalsIgnoreCase("K")) {
            return STATUS_TYPE.KEEP;
        } else if(value.equalsIgnoreCase("F")) {
            return STATUS_TYPE.FORCE;
        } else if(value.equalsIgnoreCase("M")) {
            return STATUS_TYPE.MERGE;
        } else {
            return STATUS_TYPE.UNKNOWN;
        }
    }

    public final STATUS_TYPE getStatus(final String id, final String attributeName) {
        if(!status.containsKey(attributeName.toLowerCase())
                || status.get(attributeName.toLowerCase()) == STATUS_TYPE.UNKNOWN) {
            return defaultStatus;
        } else {
            return status.get(attributeName.toLowerCase());
        }
    }

    public final List<String> getDefaultValues(final String id, final String attributeName) {
        List<String> values = defaultValues.get(attributeName.toLowerCase());
        ArrayList<String> copy = null;
        if(values != null) {
            copy = new ArrayList<String>(values);
        }
        return copy;
    }

    public final List<String> getCreateValues(final String id, final String attributeName) {
        List<String> values = createValues.get(attributeName.toLowerCase());
        ArrayList<String> copy = null;
        if(values != null) {
            copy = new ArrayList<String>(values);
        }
        return copy;
    }

    public final List<String> getForceValues(final String id, final String attributeName) {
        List<String> values = forceValues.get(attributeName.toLowerCase());
        ArrayList<String> copy = null;
        if(values != null) {
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
        String property = Configuration.getString("lsc.tasks." + syncName + ".dstService.attrs");
        if(property == null) {
            return null;
        }
        
        List<String> writeAttributes = Configuration.getListFromString(property);
        if (writeAttributes.size() == 0) {
        	LOGGER.warn("No attributes set to write in the destination. This means that LSC will not change anything! Update lsc.tasks." + syncName + ".dstService.attrs to change this.");
        }
        return writeAttributes;
    }

    public String getCreateCondition() {
        String property = Configuration.getString("lsc.tasks." + syncName + ".condition.create");
        if(property == null) {
            return "true";
        }
        return property;
    }

    public String getDeleteCondition() {
        String property = Configuration.getString("lsc.tasks." + syncName + ".condition.delete");
        if(property == null) {
            return "true";
        }
        return property;
    }

    public String getUpdateCondition() {
        String property = Configuration.getString("lsc.tasks." + syncName + ".condition.update");
        if(property == null) {
            return "true";
        }
        return property;
    }
    
    public String getModrdnCondition() {
        String property = Configuration.getString("lsc.tasks." + syncName + ".condition.modrdn");
        if(property == null) {
            return "true";
        }
        return property;
    }

    public String getCondition(JndiModificationType operation) {
        String result = "true";
        switch(operation) {
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
        return Configuration.getString("lsc.tasks." + syncName + ".dn");
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
