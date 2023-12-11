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
package org.lsc.beans.syncoptions;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.lsc.LscModificationType;
import org.lsc.configuration.PolicyType;
import org.lsc.configuration.TaskType;

/**
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public interface ISyncOptions {

	/** default condition if none is given */
	public static final String DEFAULT_CONDITION = "true";

	/** list of output formats */
	public enum OutputFormat { LDIF, JSON };
	
	/**
	 * Initialize the synchronization options policy.
	 * @param task the task configuration object on which applying syncopts
	 */
	void initialize(TaskType task);

	/**
	 * Analyze the context to get the right synchronization status to apply.
	 * @param id the object identifier according to the datasource
	 * @param attributeName the attribute name
	 * @return the default or create value
	 */
	PolicyType getStatus(String id, String attributeName);

	/**
	 * Return the default values for a given attribute name.
	 * The default values replace a missing value in the source datasource
	 * @param id the object identifier according to the datasource
	 * @param attributeName the attribute name
	 * @return the default value
	 */
	List<String> getDefaultValues(String id, String attributeName);

	/**
	 * Return the create value for a given attribute name.
	 * The create value replace a missing value in the source datasource
	 * only while creating a new entry. For coherence, implementation
	 * classes must guarantee that a default value override a create value
	 * @param id the object identifier according to the datasource
	 * @param attributeName the attribute name
	 * @return the create value
	 */
	List<String> getCreateValues(String id, String attributeName);

	/**
	 * Return the names of attributes to be created.
	 * @return the created attributes names
	 */
	Set<String> getCreateAttributeNames();

	/**
	 * Return the default valued attributes
	 * @return the default valued attributes names
	 */
	Set<String> getDefaultValuedAttributeNames();

	/**
	 * Return the force value for a given attribute name.
	 * The force value forces a value in the desination directory
	 * @param id the object identifier according to the datasource
	 * @param attributeName the attribute name
	 * @return the force value
	 */
	List<String> getForceValues(String id, String attributeName);

	/**
	 * Return the force valued attributes
	 * @return the force valued attributes names
	 */
	Set<String> getForceValuedAttributeNames();

	/**
	 * Returns the condition for a creation
	 *
	 * @return the condition or "true" if none is specified (default)
	 */
	String getCreateCondition();

	/**
	 * Returns the condition for an update
	 *
	 * @return the condition or "true" if none is specified (default)
	 */
	String getUpdateCondition();

	/**
	 * Returns the condition for a delete
	 *
	 * @return the condition or "true" if none is specified (default)
	 */
	String getDeleteCondition();

	/**
	 * Returns the condition for a id change
	 *
	 * @return the condition or "true" if none is specified (default)
	 */
	String getChangeIdCondition();
	

	String getCondition(LscModificationType operation);

	/**
	 * Returns the posthook output format
	 *
	 * @return the posthook output format (default = OutputFormat.LDIF)
	 */
	OutputFormat getPostHookOutputFormat();

	/**
	 * Returns the posthook for a creation
	 *
	 * @return the posthook or "" if none is specified (default)
	 */
	Optional<String> getCreatePostHook();

	/**
	 * Returns the posthook for an update
	 *
	 * @return the posthook or "" if none is specified (default)
	 */
	Optional<String> getUpdatePostHook();

	/**
	 * Returns the posthook for a delete
	 *
	 * @return the posthook or "" if none is specified (default)
	 */
	Optional<String> getDeletePostHook();

	/**
	 * Returns the posthook for a id change
	 *
	 * @return the posthook or "" if none is specified (default)
	 */
	Optional<String> getChangeIdPostHook();


	Optional<String> getPostHook(LscModificationType operation);

	/**
	 * Return the expression used to infer the new object DN
	 * @return the new object dn pattern
	 */
	String getDn();

	/**
	 * Return the delimiter name for the corresponding dataset
	 * @param name dataset name
     * @return the delimiter for this dataset or the default delimiter
	 */
	String getDelimiter(String name);
}
