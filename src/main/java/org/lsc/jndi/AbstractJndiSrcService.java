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

import org.apache.log4j.Logger;

import org.lsc.objects.top;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

/**
 * This class contents all the shared method for Jndi data source access.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractJndiSrcService {
    /** This is the local LOG4J logger. */
    protected static final Logger LOGGER = 
	Logger.getLogger(AbstractJndiSrcService.class);

    /**
     * Map the ldap search result into a top derivated object.
     *
     * @param sr the ldap search result
     * @param objToFill the original object to fill
     *
     * @return the object modified
     *
     * @throws NamingException thrown if a directory exception is encountered
     *         while switching to the Java POJO
     */
    public final top getObjectFromSR(final SearchResult sr, final top objToFill)
                              throws NamingException {
        Method[] methods = objToFill.getClass().getMethods();
        Map<String, Method> localMethods = new HashMap<String, Method>();

        if (sr==null) return null;
        
        for (int i = 0; i < methods.length; i++) {
            localMethods.put(methods[i].getName(), methods[i]);
        }

        NamingEnumeration<?> ne = sr.getAttributes().getAll();

        while (ne.hasMore()) {
            Attribute attr = (Attribute) ne.next();
            String methodName = "set"
                                + attr.getID().substring(0, 1).toUpperCase()
                                + attr.getID().substring(1);

            if (localMethods.containsKey(methodName)) {
                try {
                    Class<?>[] paramsType = localMethods.get(methodName)
                                                     .getParameterTypes();

                    if (List.class.isAssignableFrom(paramsType[0])) {
                        localMethods.get(methodName)
                                    .invoke(objToFill, new Object[] { getValue(attr.getAll()) });
                    } else if (String.class.isAssignableFrom(paramsType[0])) {
                        localMethods.get(methodName)
                                    .invoke(objToFill, new Object[] { getValue(attr.getAll()).get(0) });
                    } else {
                        throw new RuntimeException("Unable to manage data type !");
                    }
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            } else {
                LOGGER.debug("Unable to map search result attribute to "
                            + attr.getID() + " attribute object !");
            }
        }

        return objToFill;
    }

    /**
     * Get a list of object values from the NamingEnumeration.
     *
     * @param ne the naming enumeration
     *
     * @return the object list
     *
     * @throws NamingException thrown if a directory exception is encountered
     *         while switching to the Java POJO
     */
    protected static List<?> getValue(final NamingEnumeration<?> ne)
                            throws NamingException {
        List<Object> l = new ArrayList<Object>();

        while (ne.hasMore()) {
            l.add(ne.next());
        }
        return l;
    }
}
