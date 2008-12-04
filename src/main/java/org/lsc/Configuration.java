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
package org.lsc;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Ldap Synchronization Connector Configuration.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class Configuration extends org.lsc.utils.Configuration {

    /**
     * Default constructor.
     */
    protected Configuration() {
        super();
    }

    /**
     * Get data source connection properties.
     * @return the data source connection properties
     */
    public static Properties getSrcProperties() {
        return getAsProperties("src");
    }

    /**
     * Get data destination connection properties.
     * @return the data destination connection properties
     */
    public static Properties getDstProperties() {
        Properties dst = getAsProperties("dst");
        if (dst == null || dst.size() == 0) {
            dst = getAsProperties("ldap");
        }
        return dst;
    }
    
    /**
     * Get a list of value from a space separated string
     * @param propertyValue the value of a property
     * @return A list of the property divided
     */
    public static List<String> getListFromString(String propertyValue, String separator) {
        List<String> result = new ArrayList<String>();
        if(propertyValue != null) {
            StringTokenizer st = new StringTokenizer(propertyValue, separator);
            for (int i = 0 ; st.hasMoreTokens() ; i++) {
                result.add(st.nextToken().toLowerCase());
            }
        }
        return result;
    }
    
    public static List<String> getListFromString(String propertyValue) {
        return Configuration.getListFromString(propertyValue, " ");
    }

    /** People DN. */
    public static final String DN_PEOPLE = 
        Configuration.getString("dn.people", "ou=People");

    /** LDAP schema DN. */
    public static final String DN_LDAP_SCHEMA = 
        Configuration.getString("dn.ldap_schema", "cn=Subschema");

    /** Enhanced schema DN. */
    public static final String DN_ENHANCED_SCHEMA = 
        Configuration.getString("dn.ldap_schema", "ou=Schema,ou=System");

    /** Structures DN. */
    public static final String DN_STRUCTURES = 
        Configuration.getString("dn.structures", "ou=Structures");

    /** Accounts DN. */
    public static final String DN_ACCOUNTS = 
        Configuration.getString("dn.accounts", "ou=Accounts");

    /** objectClass for a person. */
    public static final String OBJECTCLASS_PERSON = 
        Configuration.getString("objectclass.person", "inetOrgPerson");

    /** objectClass for an employee. */
    public static final String OBJECTCLASS_EMPLOYEE = 
        Configuration.getString("objectclass.employee", "inetOrgPerson");

    /**
     * Numbers of days between an entry is set to be deleted and its actual
     * deletion.
     */
    public static final int DAYS_BEFORE_SUPPRESSION = 
        Configuration.getInt("suppression.MARQUAGE_NOMBRE_DE_JOURS", 90);

    /** The real LDAP base DN. */
    public static final String DN_REAL_ROOT = 
        Configuration.getString("dn.real_root", "dc=lsc-project,dc=org");

    /** The maximum user identifier length. */
    public static final int UID_MAX_LENGTH = 
        Configuration.getInt("uid.maxlength", 8);
}
