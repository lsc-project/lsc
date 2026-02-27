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
 * An annotation for LSC tasks. The following elements can be configured:
 * 
 * <ul>
 *   <li>id: The rule's ID</li>
 *   <li>bean: The Java class used to instanciate elements</li>
 *   <li>cleanHook:The clean hook </li>
 *   <li>syncHook: The synchronization hook</li>
 *   <li>sourceService: The source service</li>
 *   <li>destinationService: The destination service</li>
 *   <li>errorIfEmptySource: Tell if an error is generated if the source is empty</li>
 *   <li>errorIfEmptyDestination: Tell if an error is generated if the source is empty</li>
 *   <li>customLibrary: An optional custom library to use </li>
 *   <li>scriptInclude: An option script</li>
 * </ul>
 */
public @interface CreateTask {
    /** @return The task's ID */
    String id() default "";
    
    /** @return The task's name */
    String name() default "";
    
    /** @return The Bean class to use to store elements */
    String bean() default "org.lsc.beans.SimpleBean";
    
    /** @return The hook to call when cleaning elements */
    String cleanHook() default "";
    
    /** @return The hook to call when syncing elements */
    String syncHook() default "";
    
    /** @return The LDAP Source service */
    CreateLdapSourceService[] ldapSourceService() default {};
    
    /** @return The Destination service. */
    CreateLdapDestinationService[] ldapDestinationService() default {};
    
    /** Tell if an error is generated when the source is empty */
    boolean errorIfEmptySource() default true;

    /** Tell if an error is generated when the destination is empty */
    boolean errorIfEmptyDestination() default true;
    
    /** 
     * The synchronization options. 
     * One of :
     * <ul>
     *   <li>propertiesBasedSyncOptions</li>
     *   <li>forceSyncOptions</li>
     *   <li>PluginSyncOptions</li>
     * </ul>
     **/
    CreatePropertiesBasedSyncOptions[] propertiesBasedSyncOptions() default {};
    
    CreateForceSyncOptions[] forceSyncOptions() default {};
    
    //CreatePluginSyncOptions[] pluginSyncOptions() default {}
    
    /** A custom library that is going to be used */
    CreateValuesType[] customLibrary() default {};
    
    /** An included script */
    CreateValuesType[] scriptInclude() default {};
}
