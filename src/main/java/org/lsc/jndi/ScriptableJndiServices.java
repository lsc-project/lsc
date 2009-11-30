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
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * Based on rhino, this class is able to understand your LQL requests.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class ScriptableJndiServices extends ScriptableObject {
    
    /** Local jndi instance. Used to connect to the right directory. */
    private JndiServices jndiServices;

    /**
     * Default constructor.
     * 
     * Default directory properties are based on destination.
     */
    public ScriptableJndiServices() {
//        jndiServices = JndiServices.getDstInstance();
    }

    /**
     * Default jndiServices setter.
     * @param jndiServices the new value
     */
    public final void setJndiServices(JndiServices jndiServices) {
        this.jndiServices = jndiServices;
    }

    public final List<String> search(final Object a, final Object b) throws NamingException {
        return wrapString("_search", a, b);
    }

    protected List<String> _search(final String base, final String filter)
                          throws NamingException {
        return jndiServices.getDnList(base, filter,
                                      SearchControls.SUBTREE_SCOPE);
    }

    public final List<String> list(final Object a, final Object b) throws NamingException {
        return wrapString("_list", a, b);
    }

    protected final List<String> _list(final String base, final String filter)
                        throws NamingException {
        return jndiServices.getDnList(base, filter,
                                      SearchControls.ONELEVEL_SCOPE);
    }

    public final List<String> read(final Object a, final Object b) throws NamingException {
        return wrapString("_read", a, b);
    }

    protected List<String> _read(final String base, final String filter)
                        throws NamingException {
        return jndiServices.getDnList(base, filter, SearchControls.OBJECT_SCOPE);
    }

    public final List<String> exists(final Object a, final Object b)
                              throws NamingException {
        return wrapString("_exists", a, b);
    }

    protected List<String> _exists(final String dn, final String filter)
                          throws NamingException {
        if (jndiServices.exists(dn)) {
            List<String> c = new ArrayList<String>();
            c.add(dn);

            return c;
        } 
        return null;
    }

    public final List<String> or(final Object a, final Object b) throws NamingException {
        return wrapList("_or", a, b);
    }

    protected final  List<String> _or(final List<String> a, final List<String> b)
                      throws NamingException {
        List<String> c = new ArrayList<String>();
        c.addAll(a);
        c.addAll(b);

        return c;
    }

    public final List<String> attribute(final Object a, final Object b)
                                 throws NamingException {
        return wrapString("_attr", a, b);
    }

    @SuppressWarnings({"unchecked"})
    protected List<String> _attr(final String base, final String attrName)
                        throws NamingException {
        SearchResult sr = jndiServices.readEntry( base, "objectClass=*", false);

        if ((sr != null) && (sr.getAttributes() != null)
                && (sr.getAttributes().get(attrName) != null)) {
            return (List<String>) Collections.list(sr.getAttributes()
                                                     .get(attrName).getAll());
        }
        return null;
    }

    public final List<String> and(final Object a, final Object b) throws NamingException {
        return wrapList("_and", a, b);
    }

    protected List<String> _and(final List<String> aList, final List<String> bList)
                       throws NamingException {
        List<String> cList = new ArrayList<String>();

        if (aList.size() < bList.size()) {
        	for (String tmp : aList) {
                if (bList.contains(tmp)) {
                    cList.add(tmp);
                }
            }
        } else {
        	for (String tmp : bList) {
                if (aList.contains(tmp)) {
                    cList.add(tmp);
                }
            }
        }

        return cList;
    }

    public final List<String> retain(final Object a, final Object b) throws NamingException {
        return wrapList("_retain", a, b);
    }

    protected List<String> _retain(final List<String> aList, final List<String> bList)
                          throws NamingException {
        List<String> cList = new ArrayList<String>();
        for (String aValue : aList) {
            if (!bList.contains(aValue)) {
                cList.add(aValue);
            }
        }

        return cList;
    }

    public final List<String> sup(final Object a, final Object b) throws NamingException {
        return wrapString("_sup", a, b);
    }

    protected List<String> _sup(final String dn, final String levelStr)
                       throws NamingException {
        int levelValue = Integer.parseInt(levelStr);

        return jndiServices.sup(dn, levelValue);
    }

    public final List<String> fsup(final Object a, final Object b)
                            throws NamingException {
        return wrapString("_fsup", a, b);
    }

    protected List<String> _fsup(final String base, final String filter)
                              throws NamingException {
        List<String> cList = new ArrayList<String>();
        List<String> dns = jndiServices.sup(base, 0);

        if (dns == null) {
            return null;
        }

        for (String dn : dns) {
            if (jndiServices.exists(dn, filter)) {
                cList.add(dn);

                return cList;
            }
        }

        return cList;
    }
}
