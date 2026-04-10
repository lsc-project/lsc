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

import org.lsc.configuration.PolicyType;

/**
 * An annotation for the PropertiesBasedOptions, which inherits from
 * SyncOptions:
 * 
 * <ul>
 *   <li>id: The SyncOptions ID</li>
 *   <li>mainIdentifier: The main identifier construction rule</li>
 *   <li>pivotTransformation: The pivot transformation (optional)</li>
 *   <li>dataset: Tells how to handle each attribute</li>
 *   <li>defaultDelimiter: The value used to split multiple values </li>
 *   <li>defaultPolicy: The policy, default to FORCE</li>
 *   <li>conditions: The conditions to test before the four operations (optional)</li>
 *   <li>hooks: Hook for each specific operations</li>
 * </ul>
 */
public @interface CreatePropertiesBasedSyncOptions {
    //-----------------------------------------------------------
    // From SyncOptions
    //-----------------------------------------------------------
    /**
     * The SyncOption ID. 
     * 
     * @return The SyncOption ID 
     **/
    String id() default "";
    
    /**
     * The SyncOption main identifier
     * @return The main identifier
     */
    String mainIdentifier() default "";
    
    //-----------------------------------------------------------
    // From PropertiesBasedSyncOptions
    //-----------------------------------------------------------
    // The pivot transformation
    String pivotTransformation() default "";
    
    // The datasets 
    CreateDataset[] dataset();
    
    // The policyType
    PolicyType defaultPolicy() default PolicyType.FORCE;
    
    // The default delimiter
    String defaultDelimiter() default "";
    
    // The conditions
    CreateConditions[] conditions() default {};
    
    // The hooks
    CreateHooks[] hooks() default {};
}
