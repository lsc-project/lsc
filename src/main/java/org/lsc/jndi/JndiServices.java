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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.log4j.Logger;
import org.lsc.Configuration;

import com.sun.jndi.ldap.LdapURL;

/**
 * General LDAP services wrapper.
 * 
 * This class is designed to manage all the needed operations to the directory
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public final class JndiServices {

    /** Default LDAP filter. */
    public static final String DEFAULT_FILTER = "objectClass=*";

    /** the Log4J instance. */
    private static final Logger LOGGER = Logger.getLogger(JndiServices.class);

    /** the ldap ctx. */
    private LdapContext ctx;

    /** The context base dn. */
    private String contextDn;

    /** The instances cache. */
    private static Map<Properties, JndiServices> cache;

    /** Number of results per page (through PagedResults extended control). */
    private int pageSize;

    //	/** Attribute name to sort on. */
    //	private String sortedBy;

    /**
     * Initiate the object and the connection according to the properties
     * files.
     * @param connProps the connection properties to use to instantiate 
     * connection
     * @throws NamingException thrown if a directory error is encountered
     */
    private JndiServices(final Properties connProps) throws NamingException {
        ctx = new InitialLdapContext(connProps, null);
        contextDn = new LdapURL((String) ctx.getEnvironment().get("java.naming.provider.url")).getDN().toString();
        String pageSizeStr = (String) ctx.getEnvironment().get("java.naming.ldap.pageSize");
        if (pageSizeStr != null) {
            pageSize = Integer.parseInt(pageSizeStr);
        } else {
            pageSize = -1;
        }
        //		sortedBy = (String) ctx.getEnvironment().get("java.naming.ldap.sortedBy");
    }

    /**
     * Get the source directory connected service.
     * @return the source directory connected service
     */
    public static JndiServices getSrcInstance() {
	Properties srcProperties = Configuration.getSrcProperties();
	if (srcProperties != null && srcProperties.size() > 0) {
        	return getInstance(Configuration.getSrcProperties());
	}
	return null;
    }

    /**
     * Get the target directory connected service.
     * @return the target directory connected service
     */
    public static JndiServices getDstInstance() {
        return getInstance(Configuration.getDstProperties());
    }

    /**
     * Instance getter. Manage a connections cache and return the good service
     * @param props the connection properties
     * @return the instance
     */
    private static JndiServices getInstance(final Properties props) {
        try {
            if (cache == null) {
                cache = new HashMap<Properties, JndiServices>();
            }
            if (!cache.containsKey(props)) {
                cache.put(props, new JndiServices(props));
            }
            return (JndiServices) cache.get(props);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Search for an entry.
     * 
     * This method is a simple LDAP search operation with SUBTREE search
     * control
     * 
     * @param base
     *                the base of the search operation
     * @param filter
     *                the filter of the search operation
     * @return the entry or null if not found
     * @throws NamingException
     *                 thrown if something goes wrong
     */
    public SearchResult getEntry(final String base, final String filter) throws NamingException {
        SearchControls sc = new SearchControls();
        return getEntry(base, filter, sc);
    }

    /**
     * Search for an entry.
     * 
     * This method is a simple LDAP search operation with SUBTREE search
     * control
     * 
     * @param base the base of the search operation
     * @param filter  the filter of the search operation
     * @param sc the search controls
     * @return the entry or null if not found
     * @throws NamingException
     *                 thrown if something goes wrong
     */
    public SearchResult getEntry(final String base, final String filter, 
            final SearchControls sc) throws NamingException {
        NamingEnumeration<SearchResult> ne = null;
        try {
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String rewrittenBase = null;
            if (base.toLowerCase().endsWith(contextDn.toLowerCase())) {
                if (!base.equalsIgnoreCase(contextDn)) {
                    rewrittenBase = base.substring(0, base.toLowerCase()
                            .lastIndexOf(contextDn.toLowerCase()) - 1);
                } else {
                    rewrittenBase = "";
                }
            } else {
                rewrittenBase = base;
            }
            ne = ctx.search(rewrittenBase, filter, sc);
        } catch (NamingException nex) {
            LOGGER.error("Error while looking for " + filter + " in " + base + ": " + nex, nex);
            throw nex;
        }
        SearchResult sr = null;
        if (ne.hasMore()) {
            sr = (SearchResult) ne.next();
            if (ne.hasMoreElements()) {
                LOGGER.error("Too many entries returned (base: \"" + base
                        + "\", filter: \"" + filter + "\"");
            } else {
                return sr;
            }
        }
        return sr;
    }

    /**
     * Check if the entry with the specified distinguish name exists (or
     * not).
     * 
     * @param dn the entry's distinguish name
     * @param filter look at the dn according this filter
     * @return entry existence (or false if something goes wrong)
     */
    public boolean exists(final String dn, final String filter) {
        try {
            return (readEntry(dn, filter, true) != null);
        } catch (NamingException e) {
            LOGGER.error(e, e);
        }
        return false;
    }

    /**
     * Check if the entry with the specified distinguish name exists (or
     * not).
     * 
     * @param dn the entry's distinguish name
     * @return entry existence (or false if something goes wrong)
     */
    public boolean exists(final String dn) {
        return exists(dn, DEFAULT_FILTER);
    }

    /**
     * Search for an entry.
     * 
     * This method is a simple LDAP search operation with SUBTREE search
     * control
     * 
     * @param base
     *                the base of the search operation
     * @param allowError
     *                log error if not found or not
     * @return the entry or null if not found
     * @throws NamingException
     *                 thrown if something goes wrong
     */
    public SearchResult readEntry(final String base, final boolean allowError) throws NamingException {
        return readEntry(base, DEFAULT_FILTER, allowError);
    }

    public SearchResult readEntry(final String base, final String filter, final boolean allowError)
    throws NamingException {
        SearchControls sc = new SearchControls();
        return readEntry(base, filter, allowError, sc);
    }

    public String rewriteBase(final String base) {
        try {
            String contextDn = new LdapURL((String) ctx.getEnvironment().get(
            "java.naming.provider.url")).getDN().toString();
            String rewrittenBase = null;
            if (base.toLowerCase().endsWith(contextDn.toLowerCase())) {
                if (!base.equalsIgnoreCase(contextDn)) {
                    rewrittenBase = base.substring(0, base.toLowerCase()
                            .lastIndexOf(contextDn.toLowerCase()) - 1);
                } else {
                    rewrittenBase = "";
                }
            } else {
                rewrittenBase = base;
            }
            return rewrittenBase;
        } catch (NamingException nex) {
            LOGGER.error("Error rewriting LDAP DN " + base + " : " + nex, nex);
            return null;
        }
    }

    public SearchResult readEntry(final String base, final String filter,
            final boolean allowError, final SearchControls sc) throws NamingException {
        NamingEnumeration<SearchResult> ne = null;
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);
        try {
            ne = ctx.search(rewriteBase(base), filter, sc);
        } catch (NamingException nex) {
            if (!allowError) {
                LOGGER.error("Error while reading entry " + base + " : " + nex,	nex);
            }
            return null;
        }
        SearchResult sr = null;
        if (ne.hasMore()) {
            sr = (SearchResult) ne.next();
            if (ne.hasMore()) {
                LOGGER.error("To many entries returned (base: \"" + base + "\"");
            } else {
                return sr;
            }
        }
        return sr;
    }

    /**
     * Search for a list of DN.
     * 
     * This method is a simple LDAP search operation which is attended to
     * return a list of the entries DN
     * 
     * @param base
     *                the base of the search operation
     * @param filter
     *                the filter of the search operation
     * @param scope
     *                the scope of the search operation
     * @return the dn of each entry that is returned by the directory
     * @throws NamingException
     *                 thrown if something goes wrong
     */
    public List<String> getDnList(final String base, final String filter, 
            final int scope) throws NamingException {
        NamingEnumeration<SearchResult> ne = null;
        List<String> l = new ArrayList<String>();
        try {
            SearchControls sc = new SearchControls();
            sc.setDerefLinkFlag(false);
            sc.setReturningAttributes(new String[] { "1.1" });
            sc.setSearchScope(scope);
            sc.setReturningObjFlag(true);
            ne = ctx.search(base, filter, sc);
            String completedBaseDn = null;
            if (base.length() > 0) {
                completedBaseDn = "," + base;
            } else {
                completedBaseDn = "";
            }
            while (ne.hasMore()) {	
                l.add(((SearchResult) ne.next()).getName() + completedBaseDn);
            }
        } catch (NamingException nex) {
            LOGGER.error(nex, nex);
        }
        return l;
    }

    /**
     * Apply directory modifications.
     * 
     * If no exception is thrown, modifications were done successfully
     * 
     * @param jm modifications to apply
     * @return operation status
     */
    public boolean apply(final JndiModifications jm) {
        if (jm==null) return true;
        try {
            switch (jm.getOperation()) {
            case ADD_ENTRY:
                ctx.createSubcontext(
                        new LdapName(rewriteBase(jm.getDistinguishName())),
                        getAttributes(jm.getModificationItems(), true)
                );
                break;
            case DELETE_ENTRY:
                ctx.destroySubcontext(new LdapName(rewriteBase(jm.getDistinguishName())));
                break;
            case MODIFY_ENTRY:
                Object[] table = jm.getModificationItems().toArray();
                ModificationItem[] mis = new ModificationItem[table.length];
                for (int i = 0; i < table.length; i++) {
                    mis[i] = (ModificationItem) table[i];
                }
                ctx.modifyAttributes(new LdapName(rewriteBase(jm.getDistinguishName())), mis);
                break;
            case MODRDN_ENTRY:
                ctx.rename(
                        new LdapName(rewriteBase(jm.getDistinguishName())),
                        new LdapName(rewriteBase(jm.getNewDistinguishName()))
                );
                break;
            default:
                LOGGER.error("Unable to identify the right modification type: " + jm.getOperation());
            return false;
            }
            return true;
        } catch (NamingException ne) {
            LOGGER.error("Error while modifying directory on entry "
                    + jm.getDistinguishName() + " / "
                    + jm.getModificationItems(), ne);
            return false;
        }
    }

    /**
     * Return the modificationItems in the javax.naming.directory.Attributes
     * format.
     * 
     * @param modificationItems
     *                the modification items list
     * @param forgetEmpty
     *                if specified, empty attributes will not be converted
     * @return the formatted attributes
     */
    private Attributes getAttributes(final List<ModificationItem> modificationItems, 
            final boolean forgetEmpty) {
        Attributes attrs = new BasicAttributes();
        Iterator<ModificationItem> modificationItemsIter = modificationItems.iterator();
        while (modificationItemsIter.hasNext()) {
            ModificationItem mi = (ModificationItem) modificationItemsIter.next();
            if (!(forgetEmpty && mi.getAttribute().size() == 0)) {
                attrs.put(mi.getAttribute());
            }
        }
        return attrs;
    }

    /**
     * Return the LDAP schema.
     * 
     * @param attrsToReturn
     *                list of attribute names to return (or null for all
     *                'standard' attributes)
     * @return the map of name => attribute
     * @throws NamingException
     *                 thrown if something goes wrong (bad
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getSchema(final String[] attrsToReturn) throws NamingException {
        Map<String, List<String>> attrsResult = new HashMap<String, List<String>>();

        // connect to directory
        Hashtable props = ctx.getEnvironment();
        String baseUrl = (String) props.get("java.naming.provider.url");
        baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
        props.put("java.naming.provider.url", baseUrl);
        DirContext schemaCtx = new InitialLdapContext(props, null);

        // find schema entry
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.OBJECT_SCOPE);
        sc.setReturningAttributes(new String[] {"subschemaSubentry"});

        NamingEnumeration<SearchResult> schemaDnSR = schemaCtx.search("", "(objectclass=*)", sc);

        SearchResult sr = null;
        Attribute subschemaSubentry = null;
        String subschemaSubentryDN = null;

        if (schemaDnSR.hasMore()) sr = schemaDnSR.next();
        if (sr != null) subschemaSubentry = sr.getAttributes().get("subschemaSubentry");
        if (subschemaSubentry != null && subschemaSubentry.size() > 0) {
            subschemaSubentryDN = (String) subschemaSubentry.get();
        }

        if (subschemaSubentryDN != null) {
            // get schema attributes from subschemaSubentryDN
            Attributes schemaAttrs = schemaCtx.getAttributes(
                    subschemaSubentryDN, attrsToReturn != null ? attrsToReturn : new String[] { "*", "+" });

            if (schemaAttrs != null) {
                for (int i = 0; i < attrsToReturn.length; i++) {
                    Attribute schemaAttr = schemaAttrs.get(attrsToReturn[i]);
                    if (schemaAttr != null) {
                        attrsResult.put(schemaAttr.getID(), (List<String>) Collections
                                .list(schemaAttr.getAll()));
                    }
                }
            }
        }

        return attrsResult;
    }

    public List<String> sup(String dn, int level) throws NamingException {
        int ncLevel = (new LdapName(contextDn)).size();

        LdapName lName = new LdapName(dn);
        List<String> cList = new ArrayList<String>();
        if (level > 0) {
            if (lName.size() > level) {
                for (int i = 0; i < level; i++) {
                    lName.remove(lName.size() - 1);
                }
                cList.add(lName.toString());
            }
        } else if (level == 0) {
            cList.add(lName.toString());
            int size = lName.size();
            for (int i = 0; i < size - 1 && i < size - ncLevel; i++) {
                lName.remove(lName.size() - 1);
                cList.add(lName.toString());
            }
        } else {
            return null;
        }
        return cList;
    }

    /**
     * Search for a list of attribute values
     * 
     * This method is a simple LDAP search operation which is attended to
     * return a list of the attribute values in all returned entries
     * 
     * @param base the base of the search operation
     * @param filter the filter of the search operation
     * @param scope the scope of the search operation
     * @param attrName the attribute name to use to
     * @return the dn of each entry that are returned by the directory
     * @throws NamingException thrown if something goes wrong
     */
    public Map<String, String> getAttrList(final String base, 
            final String filter, final int scope, final String attrName) 
            throws NamingException {

        Map<String, Map<String, String>> map = getAttrsList(base, filter, scope, new String[] {attrName});
        Map<String, String> result = null;
        if(map != null) {
            result = new HashMap<String, String>();
            Iterator<Entry<String, Map<String, String>>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Map<String, String>> entry = it.next();
                Iterator<String> iteValues = entry.getValue().values().iterator();
                
                if(iteValues.hasNext()) {
                    result.put(entry.getKey(), iteValues.next());
                }
            }
        }
        
        return result;
    }

    /**
     * Search for a list of attribute values
     * 
     * This method is a simple LDAP search operation which is attended to
     * return a list of the attribute values in all returned entries
     * 
     * @param base the base of the search operation
     * @param filter the filter of the search operation
     * @param scope the scope of the search operation
     * @param attrsNames table of attribute names to get
     * @return the dn of each entry that are returned by the directory and a map of attribute names and values
     * @throws NamingException thrown if something goes wrong
     */
    public Map<String, Map<String, String>> getAttrsList(final String base, 
            final String filter, final int scope, final String[] attrsNames) 
            throws NamingException {

        Map<String, Map<String, String>> res = new HashMap<String, Map<String, String>>();

        SearchControls constraints = new SearchControls();
        constraints.setDerefLinkFlag(false);
        constraints.setReturningAttributes(attrsNames);
        constraints.setSearchScope(scope);
        constraints.setReturningObjFlag(true);

        try {
            boolean requestPagedResults = false;

            List<Control> extControls = new ArrayList<Control>();

            if (pageSize > 0) {
                requestPagedResults = true;
                LOGGER.debug("Using pagedResults control for " + pageSize + " entries at a time");
            }

            if (requestPagedResults) {
                extControls.add(new PagedResultsControl(pageSize, Control.CRITICAL));
            }

            if (extControls.size() > 0) {
                ctx.setRequestControls(extControls.toArray(new Control[extControls.size()]));
            }

            byte[] pagedResultsResponse = null;
            do {
                NamingEnumeration<SearchResult> results = ctx.search(base, filter, constraints);

                if (results != null) {
                    while (results.hasMoreElements()) {
                        Map<String, String> attrsValues = new HashMap<String, String>();

                        SearchResult ldapResult = (SearchResult) results.next();

                        // get the value for each attribute requested
                        String attrName = null;
                        for (int i = 0; i < attrsNames.length; i++) {
                            attrName = attrsNames[i];
                            Attribute attr = ldapResult.getAttributes().get(attrName);
                            if (attr != null && attr.get() != null) {
                                attrsValues.put(attrName, (String) attr.get());
                            }
                        }
                        if (attrsValues.size() == attrsNames.length) {
                            res.put(ldapResult.getName(), attrsValues);
                        }
                    }
                }
                Control[] respCtls = ctx.getResponseControls();
                if (respCtls != null) {
                    for (int i = 0; i < respCtls.length; i++) {
                        Control respCtl = respCtls[i];
                        if (requestPagedResults && respCtl instanceof PagedResultsResponseControl) {
                            pagedResultsResponse = ((PagedResultsResponseControl)respCtls[i]).getCookie();
                        }
                    }
                }

                if (requestPagedResults && pagedResultsResponse != null) {
                    ctx.setRequestControls(new Control[] {
                            new PagedResultsControl(pageSize, pagedResultsResponse, Control.CRITICAL) });
                }

            } while (pagedResultsResponse != null);

            // clear requestControls for future use of the JNDI context
            if (requestPagedResults) {
                ctx.setRequestControls(null);
            }
        } catch (NamingException e) {
            // clear requestControls for future use of the JNDI context
            ctx.setRequestControls(null);
            e.printStackTrace();
        } catch (IOException e) {
            // clear requestControls for future use of the JNDI context
            ctx.setRequestControls(null);
            e.printStackTrace();
        }
        return res;
    }

	/**
	 * @return the contextDn
	 */
	public String getContextDn() {
		return contextDn;
	}
}
