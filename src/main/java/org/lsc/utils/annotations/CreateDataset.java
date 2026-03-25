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
 * An annotation to create a Dataset.
 *
 * The structure is:
 * <ul>
 *   <li>id: The dataset ID. Optional</li>
 *   <li>name: The attribute ID</li>
 *   <li>policy: The policy to apply: FORCE, KEEP or MERGE. Optional</li>
 *   <li>defaultValues: The default value. Optional</li>
 *   <li>forceValues: The forced values. Optional</li>
 *   <li>createValues: The values to create. Optional</li>
 *   <li>delimiter: The values delimiter. Optional</li>
 * </ul>
 */
public @interface CreateDataset {
    /**
     * The Dataset ID.
     *
     * @return The Dataset ID
     **/
    String id() default "";

    /**
     * The Dataset attribute name.
     *
     * @return The Dataset attribute name
     **/
    String name();

    /**
     * The Dataset policy, one of FORCE, MERGE or KEEP. If the value is INHERITED,
     * the task's default Policy is used.
     *
     * @return The Dataset policy
     **/
    PolicyType[] policy() default {};

    /**
     * The Dataset default values.
     *
     * @return The Dataset default values
     **/
    CreateValuesType[] defaultValues() default {};

    /**
     * The Dataset create values.
     *
     * @return The Dataset create values
     **/
    CreateValuesType[] createValues() default {};

    /**
     * The Dataset force values.
     *
     * @return The Dataset force values
     **/
    CreateValuesType[] forceValues() default {};
}
