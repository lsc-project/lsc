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
package org.lsc;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All objects in LSC inherit from this class, both pure-LDAP style objects
 * (including top), and flat, database-style objects.
 * 
 * @author Rémy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 */
public abstract class LscObject
{
	/* The local logger */
	protected static Logger LOGGER = LoggerFactory.getLogger(LscObject.class);

	protected String distinguishName;

	protected List<String> objectClass = null;

	/**
	 * Returns the full distinguished name of this object.
	 * 
	 * @return distinguished name of this object
	 */
	public final String getDistinguishName()
	{
		return distinguishName;
	}

	/**
	 * Set the full distinguished name for this object.
	 * 
	 * @param dn
	 *            This object's distinguished name
	 */
	public final void setDistinguishName(String dn)
	{
		this.distinguishName = dn;
	}

	/**
	 * Return the list of values for the objectClass of this object
	 * 
	 * @return list of values for the objectClass of this object
	 */
	public final List<String> getObjectClass()
	{
		return objectClass;
	}

	/**
	 * Set the list of values for the objectClass of this object.
	 * <p>
	 * Note that this will replace any existing list. To add to the existing
	 * list instead, see {@link #setObjectClass(String)}.
	 * </P>
	 * 
	 * @param objectClass
	 *            The objectClass list to set
	 */
	public final void setObjectClass(List<String> objectClass)
	{
		this.objectClass = objectClass;
	}

	/**
	 * Add an objectClass value to this object.
	 * <p>
	 * Note that this will add a new value to the existing list, it will not
	 * replace it. To replace the full list see {@link #setObjectClass(List)}.
	 * </p>
	 * <p>
	 * If no list has been set yet, this method will create one.
	 * </p>
	 * 
	 * @param objectClass
	 *            The objectClass name to add
	 */
	public final void setObjectClass(String objectClass)
	{
		if (this.objectClass == null)
		{
			this.objectClass = new ArrayList<String>();
		}
		this.objectClass.add(objectClass);
	}
}
