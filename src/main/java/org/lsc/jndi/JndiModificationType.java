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
package org.lsc.jndi;

import org.lsc.LscModificationType;

/**
 * Enumeration for the modifications type of an directory
 * 
 * @author Rémy-Christophe Schermesser &lt;remy-christophe@schermesser.com&gt;
 *
 */
public enum JndiModificationType {

	/** Operation identifier to add entry. */
	ADD_ENTRY("create"),

	/** Operation identifier to remove entry. */
	DELETE_ENTRY("delete"),

	/** Operation identifier to modify entry. */
	MODIFY_ENTRY("update"),

	/** Operation identifier to modify the dn. */
	MODRDN_ENTRY("modrdn");

	private final String description;

	private JndiModificationType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	/**
	 * Returns the JndiModificationType from a String
	 *
	 * @param desc the string
	 * @return The JndiModificationType
	 */
	public static JndiModificationType getFromDescription(String desc) {
		JndiModificationType result = null;

		for(JndiModificationType type: JndiModificationType.values()) {
			if (type.getDescription().matches(desc)) {
				result = type;
			}
		}
		return result;
	}
	
	/**
	 * Get the JndiModificationType from LscModificationType
	 * @param lmt the original modification type
	 * @return the JNDI modification type
	 */
	public static JndiModificationType getFromLscModificationType(LscModificationType lmt) {
		switch (lmt) {
			case CREATE_OBJECT:
				return JndiModificationType.ADD_ENTRY;
			case DELETE_OBJECT:
				return JndiModificationType.DELETE_ENTRY;
			case CHANGE_ID:
				return JndiModificationType.MODRDN_ENTRY;
			case UPDATE_OBJECT:
			default:
				return JndiModificationType.MODIFY_ENTRY;
		}
	}
}
