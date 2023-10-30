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
 * Always return a Force status.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ForceSyncOptions implements ISyncOptions {

	/**
	 * The name of the task
	 */
	private TaskType task;

	public final PolicyType getStatus(final String id, final String attributeName) {
		return PolicyType.FORCE;
	}

	public final String getDefaultValue(final String id, final String attributeName) {
		return null;
	}

	public final String getCreateValue(final String id, final String attributeName) {
		return null;
	}

	@Override
	public final void initialize(TaskType task) {
		 this.task = task;
	}

	public final Set<String> getCreateAttributeNames() {
		return null;
	}

	public final Set<String> getDefaultValuedAttributeNames() {
		return null;
	}

	public Set<String> getForceValuedAttributeNames() {
		return null;
	}

	public List<String> getCreateValues(String id, String attributeName) {
		return null;
	}

	public List<String> getDefaultValues(String id, String attributeName) {
		return null;
	}

	public List<String> getWriteAttributes() {
		return task.getLdapDestinationService().getFetchedAttributes().getString();
	}

	public String getTaskName() {
		return task.getName();
	}

	public String getCreateCondition() {
		return DEFAULT_CONDITION;
	}

	public String getDeleteCondition() {
		return DEFAULT_CONDITION;
	}

	public String getUpdateCondition() {
		return DEFAULT_CONDITION;
	}

	public String getChangeIdCondition() {
		return DEFAULT_CONDITION;
	}

	public String getCondition(LscModificationType operation) {
		return DEFAULT_CONDITION;
	}

	public OutputFormat getPostHookOutputFormat() {
		return OutputFormat.LDIF;
	}

	public Optional<String> getCreatePostHook() {
		return Optional.empty();
	}

	public Optional<String> getDeletePostHook() {
		return Optional.empty();
	}

	public Optional<String> getUpdatePostHook() {
		return Optional.empty();
	}

	public Optional<String> getChangeIdPostHook() {
		return Optional.empty();
	}

	public Optional<String> getPostHook(LscModificationType operation) {
		return Optional.empty();
	}

	public String getDn() {
		return null;//((Ldap)task.getDestinationService()).getDn();
	}

	public List<String> getForceValues(String id, String attributeName) {
		return null;
	}

    @Override
    public String getDelimiter(String attributeName) {
        return null;
    }
}
