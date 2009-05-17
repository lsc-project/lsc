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
package org.lsc.beans;

import java.io.Serializable;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

/**
 * General interface of a bean.
 * 
 * This bean has been designed to allow attribute modifications
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public interface IBean extends Cloneable, Serializable {

    /**
         * Get an attribute from its name.
         * 
         * @param id
         *                the name
         * @return the attribute
         */
    Attribute getAttributeById(String id);

    /**
         * Get the attributes name.
         * 
         * @return a set containing all the attributes name
         */
    Set<String> getAttributesNames();

    /**
         * Set a attribute.
         * 
         * @param attr
         *                the attribute to set
         */
    void setAttribute(Attribute attr);

    /**
         * Get the distinguish name.
         * 
         * @return the distinguish name
         */
    String getDistinguishName();

    /**
         * Set the distinguish name.
         * 
         * @param dn
         *                the distinguish name
         */
    void setDistinguishName(String dn);

    /**
         * Generate the distinguish name according to the information on the
         * bean.
         * 
         * @throws NamingException
         *                 thrown is a directory exception is encountered while
         *                 generating the new distinguish name
         */
    void generateDn() throws NamingException;

	/**
	 * Clone this object.
	 * @return Object
	 */
	IBean clone() throws CloneNotSupportedException;

}
