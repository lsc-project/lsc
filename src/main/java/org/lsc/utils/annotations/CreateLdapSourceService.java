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
package org.lsc.utils.annotations;

/**
 * An annotation for the various LdapSourceService.
 */
public @interface CreateLdapSourceService {
    //-----------------------------------------------------------
    // From ServiceType
    //-----------------------------------------------------------
    /**
     * Service name - uniquely identifies this service within a task.
     * 
     * @return the name of the LDAP source service
     */
    String name();
    
    /**
     * Connection reference - refers to an ldapConnection defined in the connections section.
     * 
     * @return the name of the connection to use
     */
    String connectionRef();
    
    /**
     * The Ldap Source Service ID. 
     * 
     * @return The LdapSourceService ID 
     **/
    String id() default "";
    
    //-----------------------------------------------------------
    // From LdapServiceType
    //-----------------------------------------------------------
    /**
     * Base DN for LDAP searches.
     * Example: "ou=People,dc=lsc-project,dc=org"
     * 
     * @return the base distinguished name
     */
    String baseDn();
    
    /**
     * Pivot attributes used to identify entries.
     * These attributes and their values are used to match entries between source and destination.
     * 
     * @return array of pivot attribute names
     */
    String[] pivotAttributes();
    
    /**
     * Fetched attributes - attributes to retrieve from LDAP entries.
     * Only these attributes will be synchronized to the destination.
     * 
     * @return array of attribute names to fetch
     */
    String[] fetchedAttributes();
    
    /**
     * Filter to get all entries from the source.
     * Example: "(objectClass=inetOrgPerson)"
     * 
     * @return LDAP filter string for listing all objects
     */
    String allFilter();
    
    /**
     * Filter to get a single entry based on pivot attributes.
     * Can use placeholders like {mail} which will be replaced with pivot values.
     * Example: "(&(objectClass=inetOrgPerson)(mail={mail}))"
     * 
     * @return LDAP filter string for getting one object
     */
    String oneFilter();
    
    //-----------------------------------------------------------
    // From LdapSourceServiceType
    //-----------------------------------------------------------
    /**
     * Filter used during the clean phase to identify entries to delete.
     * Typically mirrors getOneFilter.
     * Example: "(&(objectClass=inetOrgPerson)(mail={mail}))"
     * 
     * @return LDAP filter string for clean operations
     * @default empty string (optional)
     */
    String cleanFilter() default "";
    
    /**
     * Filter for detecting changes in asynchronous mode.
     * Example: "(&(objectClass=inetOrgPerson)(modifyTimestamp>={lastRunTimestamp}))"
     * @return LDAP filter for changed entries
     * @default empty string
     */
    String filterAsync() default "";
    
    /**
     * Interval for asynchronous LDAP source service (in milliseconds).
     * Only used with asyncLdapSourceService implementation.
     * 
     * @return interval in milliseconds, -1 to disable
     * @default -1 (disabled)
     */
    int interval() default -1;
    
    /**
     * The date format
     * 
     * @return The date format
     * @default "" (disabled)
     */
    String dateFormat() default "";
}