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
package org.lsc.jndi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.ModificationItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Single object used to store all modifications on one directory entry.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JndiModifications {

    /** The entry distinguish name. */
    private String distinguishName;

    /** If the entry is "modrdn", the new value. */
    private String newDistinguishName;

    /** Operation must be DirContext. */
    private JndiModificationType operation;

    /** This list contains modificationsItems. */
    private List<ModificationItem> modificationItems;
    
    /** The task that these modifications concern */
    private String taskName;

    /* Logger */
    private static Logger LOGGER = LoggerFactory.getLogger(JndiModifications.class);
    
    /**
     * Standard constructor.
     * @param operation the main modification type defining this object 
     */
    public JndiModifications(final JndiModificationType operation) {
        this.operation = operation;
    }

    /**
     * Constructor.
     * 
     * @param operation the main modification type defining this object 
     * @param taskName name of the task we're building modifications for
     */
    public JndiModifications(final JndiModificationType operation, String taskName) {
        this.operation = operation;
        this.taskName = taskName;
    }
    
    /**
     * Default modifications items getter.
     * @return the modifications items list
     */
    public final List<ModificationItem> getModificationItems() {
        return modificationItems;
    }

    /**
     * Modification items list setter.
     * @param mi a list of ModificationItem objects
     */
    public final void setModificationItems(final List<ModificationItem> mi) {
        modificationItems = mi;
    }

    /**
     * Default operation getter.
     * @return the operation type
     */
    public final JndiModificationType getOperation() {
        return operation;
    }

    /**
     * Default operation setter.
     * @param operation the operation type
     */
    public final void setOperation(final JndiModificationType operation) {
        this.operation = operation;
    }

    /**
     * Default distinguish name getter.
     * @return the primary distinguish name
     */
    public final String getDistinguishName() {
        return distinguishName;
    }

    /**
     * Default distinguish name setter.
     * @param ldistinguishName the primary distinguish name
     */
    public final void setDistinguishName(final String ldistinguishName) {
        distinguishName = ldistinguishName;
    }

    /**
     * Default new distinguish name getter.
     * @return the new distinguish name
     */
    public final String getNewDistinguishName() {
        return newDistinguishName;
    }

    /**
     * Default new distinguish name setter.
     * @param lnewDistinguishName the new distinguish name
     */
    public final void setNewDistinguishName(final String lnewDistinguishName) {
        this.newDistinguishName = lnewDistinguishName;
    }
    
    /**
     * Return all the modification in a hash indexed by the name of the attribute
     * 
     * @return the hash
     * @throws NamingException
     */
    public HashMap<String, List<String>> getModificationsItemsByHash() {
        HashMap<String,  List<String>> result = new HashMap<String,  List<String>>();
        List<ModificationItem> mi = this.getModificationItems();
        
        if(mi != null) {
            Iterator<ModificationItem> iterator = this.getModificationItems().iterator();
            while (iterator.hasNext()) {
                ModificationItem modificationItem = iterator.next();
                Attribute attr = modificationItem.getAttribute();
                String id = attr.getID().toLowerCase();
                
                List<String> values = new ArrayList<String>(attr.size());
    
                try {
                    NamingEnumeration<?> ne = attr.getAll();
                    while(ne.hasMoreElements()) {
                        values.add(ne.next().toString());
                    }
                } catch (NamingException e) {
                    LOGGER.error("Error in getting the value(s) of the attribute {}", id);
                }
                
                result.put(id, values);
            }
        }
        return result;
    }

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
}
