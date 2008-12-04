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

import java.util.List;
import java.util.Set;

import org.lsc.Configuration;
import org.lsc.jndi.JndiModificationType;

/**
 * Always return a Force status.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */

public class ForceSyncOptions implements ISyncOptions {

    /**
     * The name of the task
     */
    private String taskname;

    public final STATUS_TYPE getStatus(final String id, final String attributeName) {
        return STATUS_TYPE.FORCE;
    }

    public final String getDefaultValue(final String id, final String attributeName) {
        return null;
    }

    public final String getCreateValue(final String id, final String attributeName) {
        return null;
    }

    public final void initialize(final String taskname) {
        this.taskname = taskname;
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
        String property = Configuration.getString("lsc.tasks." + taskname + ".dstService.attrs");
        if(property == null) {
            return null;
        }
        return Configuration.getListFromString(property);
    }


    private String TRUE_CONDITION = "true";

    public String getCreateCondition() {
        return TRUE_CONDITION;
    }

    public String getDeleteCondition() {
        return TRUE_CONDITION;
    }

    public String getUpdateCondition() {
        return TRUE_CONDITION;
    }

    public String getCondition(JndiModificationType operation) {
        return TRUE_CONDITION;
    }

    public String getDn() {
        return Configuration.getString("lsc.tasks." + taskname + ".dn");
    }

	public List<String> getForceValues(String id, String attributeName) {
		return null;
	}

	public String getTaskName() {
		return taskname;
	}
}
