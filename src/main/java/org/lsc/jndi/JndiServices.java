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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
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
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ietf.ldap.LDAPUrl;
import org.lsc.Configuration;
import org.lsc.LscAttributes;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(JndiServices.class);

	/** the ldap ctx. */
	private LdapContext ctx;

	/** TLSResponse in case we use StartTLS */
	private StartTlsResponse tlsResponse;

	/** The context base dn. */
	private String contextDn;

	/** The instances cache. */
	private static Map<Properties, JndiServices> cache = new HashMap<Properties, JndiServices>();

	/** Number of results per page (through PagedResults extended control). */
	private int pageSize;

	private LDAPUrl namingContext;

	/** Support for recursive deletion (default to false) */
	private boolean recursiveDelete;

	//	/** Attribute name to sort on. */
	//	private String sortedBy;
	/**
	 * Initiate the object and the connection according to the properties.
	 *
	 * @param connProps the connection properties to use to instantiate
	 * connection
	 * @throws NamingException thrown if a directory error is encountered
	 * @throws IOException thrown if an error occurs negotiating StartTLS operation
	 */
	private JndiServices(final Properties connProps) throws NamingException, IOException {

		// log new connection with it's details
		logConnectingTo(connProps);

		/* should we negotiate TLS? */
		if (Boolean.parseBoolean((String) connProps.get("java.naming.tls"))) {
			/* if we're going to do TLS, we mustn't BIND before the STARTTLS operation
			 * so we remove credentials from the properties to stop JNDI from binding */
			/* duplicate properties to avoid changing them (they are used as a cache key in getInstance() */
			Properties localConnProps = new Properties();
			localConnProps.putAll(connProps);
			String jndiContextAuthentication = localConnProps.getProperty(Context.SECURITY_AUTHENTICATION);
			String jndiContextPrincipal = localConnProps.getProperty(Context.SECURITY_PRINCIPAL);
			String jndiContextCredentials = localConnProps.getProperty(Context.SECURITY_CREDENTIALS);
			localConnProps.remove(Context.SECURITY_AUTHENTICATION);
			localConnProps.remove(Context.SECURITY_PRINCIPAL);
			localConnProps.remove(Context.SECURITY_CREDENTIALS);

			/* open the connection */
			ctx = new InitialLdapContext(localConnProps, null);

			/* initiate the STARTTLS extended operation */
			try {
				tlsResponse = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
				tlsResponse.negotiate();
			} catch (IOException e) {
				LOGGER.error("Error starting TLS encryption on connection to {}", localConnProps.getProperty(Context.PROVIDER_URL));
				LOGGER.debug(e.toString(), e);
				throw e;
			} catch (NamingException e) {
				LOGGER.error("Error starting TLS encryption on connection to {}", localConnProps.getProperty(Context.PROVIDER_URL));
				LOGGER.debug(e.toString(), e);
				throw e;
			}

			/* now we add the credentials back to the context, to BIND once TLS is started */
			ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, jndiContextAuthentication);
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, jndiContextPrincipal);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, jndiContextCredentials);

		} else {
			/* don't start TLS, just connect normally (this can be on ldap:// or ldaps://) */
			ctx = new InitialLdapContext(connProps, null);
		}

		/* get LDAP naming context */
		try {
			namingContext = new LDAPUrl((String) ctx.getEnvironment().get(Context.PROVIDER_URL));
		} catch (MalformedURLException e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw new NamingException(e.getMessage());
		}

		/* handle options */
		contextDn = namingContext.getDN() != null ? namingContext.getDN().toString() : null;

		String pageSizeStr = (String) ctx.getEnvironment().get("java.naming.ldap.pageSize");
		if (pageSizeStr != null) {
			pageSize = Integer.parseInt(pageSizeStr);
		} else {
			pageSize = -1;
		}

		String recursiveDeleteStr = (String) ctx.getEnvironment().get("java.naming.recursivedelete");
		if (recursiveDeleteStr != null) {
			recursiveDelete = Boolean.parseBoolean(recursiveDeleteStr);
		} else {
			recursiveDelete = false;
		}
	}

	private void logConnectingTo(Properties connProps) {
		if (LOGGER.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Connecting to LDAP server ");
			sb.append(connProps.getProperty(Context.PROVIDER_URL));

			// log identity used to connect
			if (connProps.getProperty(Context.SECURITY_AUTHENTICATION) == null || connProps.getProperty(Context.SECURITY_AUTHENTICATION).equals("none")) {
				sb.append(" anonymously");
			} else {
				sb.append(" as ");
				sb.append(connProps.getProperty(Context.SECURITY_PRINCIPAL));
			}

			// using TLS ?
			if (Boolean.parseBoolean((String) connProps.get("java.naming.tls"))) {
				sb.append(" with STARTTLS extended operation");
			}

			LOGGER.info(sb.toString());
		}
	}

	/**
	 * Get the source directory connected service.
	 * @return the source directory connected service
	 */
	public static JndiServices getSrcInstance() {
		try {
			Properties srcProperties = Configuration.getSrcProperties();
			if (srcProperties != null && srcProperties.size() > 0) {
				return getInstance(srcProperties);
			}
			return null;
		} catch (Exception e) {
			LOGGER.error("Error opening the LDAP connection to the source!");
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Get the target directory connected service.
	 * @return the target directory connected service
	 */
	public static JndiServices getDstInstance() {
		try {
			return getInstance(Configuration.getDstProperties());
		} catch (Exception e) {
			LOGGER.error("Error opening the LDAP connection to the destination!");
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Instance getter. Manage a connections cache and return the good service
	 * @param props the connection properties
	 * @return the instance
	 * @throws IOException
	 * @throws NamingException
	 */
	public static JndiServices getInstance(final Properties props) throws NamingException, IOException {
		if (!cache.containsKey(props)) {
			cache.put(props, new JndiServices(props));
		}
		return (JndiServices) cache.get(props);
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
		return getEntry(base, filter, sc, SearchControls.SUBTREE_SCOPE);
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
	 * @param scope the search scope to use
	 * @return the entry or null if not found
	 * @throws SizeLimitExceededException
	 * 					thrown if more than one entry is returned by the search
	 * @throws NamingException
	 *                 thrown if something goes wrong
	 */
	public SearchResult getEntry(final String base, final String filter,
					final SearchControls sc, final int scope) throws NamingException {
		//sanity checks
		String searchBase = base == null ? "" : base;
		String searchFilter = filter == null ? DEFAULT_FILTER : filter;

		NamingEnumeration<SearchResult> ne = null;
		try {
			sc.setSearchScope(scope);
			String rewrittenBase = null;
			if (contextDn != null && searchBase.toLowerCase().endsWith(contextDn.toLowerCase())) {
				if (!searchBase.equalsIgnoreCase(contextDn)) {
					rewrittenBase = searchBase.substring(0, searchBase.toLowerCase().lastIndexOf(contextDn.toLowerCase()) - 1);
				} else {
					rewrittenBase = "";
				}
			} else {
				rewrittenBase = searchBase;
			}
			ne = ctx.search(rewrittenBase, searchFilter, sc);
		} catch (NamingException nex) {
			LOGGER.error("Error while looking for {} in {}: {}",
							new Object[] { searchFilter, searchBase, nex });
			throw nex;
		}
		SearchResult sr = null;
		if (ne.hasMoreElements()) {
			sr = (SearchResult) ne.nextElement();
			if (ne.hasMoreElements()) {
				LOGGER.error("Too many entries returned (base: \"{}\", filter: \"{}\"",
								searchBase, searchFilter);
				throw new SizeLimitExceededException("Too many entries returned (base: \"" + searchBase + "\", filter: \"" + searchFilter + "\"");
			} else {
				return sr;
			}
		} else {
			// try hasMore method to throw exceptions if there are any and we didn't get our entry
			ne.hasMore();
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
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
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
	 * This method is a simple LDAP search operation with BASE search scope
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
		String contextDn = namingContext.getDN().toString();
		String rewrittenBase = null;
		if (base.toLowerCase().endsWith(contextDn.toLowerCase())) {
			if (!base.equalsIgnoreCase(contextDn)) {
				rewrittenBase = base.substring(0, base.toLowerCase().lastIndexOf(contextDn.toLowerCase()) - 1);
			} else {
				rewrittenBase = "";
			}
		} else {
			rewrittenBase = base;
		}
		return rewrittenBase;
	}

	public SearchResult readEntry(final String base, final String filter,
					final boolean allowError, final SearchControls sc) throws NamingException {
		NamingEnumeration<SearchResult> ne = null;
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		try {
			ne = ctx.search(rewriteBase(base), filter, sc);
		} catch (NamingException nex) {
			if (!allowError) {
				LOGGER.error("Error while reading entry {}: {}", base, nex);
				LOGGER.debug(nex.toString(), nex);
			}
			return null;
		}
		SearchResult sr = null;
		if (ne.hasMore()) {
			sr = (SearchResult) ne.next();
			if (ne.hasMore()) {
				LOGGER.error("To many entries returned (base: \"{}\"", base);
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
			sc.setReturningAttributes(new String[]{"1.1"});
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
		} catch (NamingException e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw e;
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
	 * @throws CommunicationException If the connection to the directory is lost
	 */
	public boolean apply(final JndiModifications jm) throws CommunicationException {
		if (jm == null) {
			return true;
		}
		try {
			switch (jm.getOperation()) {
				case ADD_ENTRY:
					ctx.createSubcontext(
									new LdapName(rewriteBase(jm.getDistinguishName())),
									getAttributes(jm.getModificationItems(), true));
					break;
				case DELETE_ENTRY:
					if (recursiveDelete) {
						deleteChildrenRecursively(rewriteBase(jm.getDistinguishName()));
					} else {
						ctx.destroySubcontext(new LdapName(rewriteBase(jm.getDistinguishName())));
					}
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
					//We do not display this warning if we do not apply the modification with the option modrdn = false
					LOGGER.warn("WARNING: updating the RDN of the entry will cancel other modifications! Relaunch synchronization to complete update.");
					ctx.rename(
									new LdapName(rewriteBase(jm.getDistinguishName())),
									new LdapName(rewriteBase(jm.getNewDistinguishName())));
					break;
				default:
					LOGGER.error("Unable to identify the right modification type: {}", jm.getOperation());
					return false;
			}
			return true;
		} catch (ContextNotEmptyException e) {
			LOGGER.error("Object {} not deleted because it has children (LDAP error code 66 received). To delete this entry and it's subtree, set the dst.java.naming.recursivedelete property to true",
							jm.getDistinguishName());
			return false;
		} catch (CommunicationException e) {
			// we lost the connection to the source or destination, stop everything!
			throw e;
		} catch (NamingException ne) {
			if (LOGGER.isErrorEnabled()) {
				StringBuilder errorMessage = new StringBuilder("Error while ");
				switch (jm.getOperation()) {
					case ADD_ENTRY:
						errorMessage.append("adding");
						break;
					case MODIFY_ENTRY:
						errorMessage.append("modifying");
						break;
					case MODRDN_ENTRY:
						errorMessage.append("renaming");
						break;
					case DELETE_ENTRY:
						if (recursiveDelete) {
							errorMessage.append("recursively deleting");
						} else {
							errorMessage.append("deleting");
						}
						break;
				}
				errorMessage.append(" entry ").append(jm.getDistinguishName());
				errorMessage.append(" in directory :").append(ne.toString());

				LOGGER.error(errorMessage.toString());
			}
			return false;
		}
	}

	/**
	 * Delete children recursively
	 * @param distinguishName the tree head to delete
	 * @throws NamingException thrown if an error is encountered
	 */
	private void deleteChildrenRecursively(String distinguishName) throws NamingException {
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		NamingEnumeration<SearchResult> ne = ctx.search(distinguishName, DEFAULT_FILTER, sc);
		while (ne.hasMore()) {
			SearchResult sr = (SearchResult) ne.next();
			String childrenDn = rewriteBase(sr.getName() + "," + distinguishName);
			deleteChildrenRecursively(childrenDn);
		}
		ctx.destroySubcontext(new LdapName(distinguishName));
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
		String baseUrl = (String) props.get(Context.PROVIDER_URL);
		baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
		props.put(Context.PROVIDER_URL, baseUrl);
		DirContext schemaCtx = new InitialLdapContext(props, null);

		// find schema entry
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		sc.setReturningAttributes(new String[]{"subschemaSubentry"});

		NamingEnumeration<SearchResult> schemaDnSR = schemaCtx.search("", "(objectclass=*)", sc);

		SearchResult sr = null;
		Attribute subschemaSubentry = null;
		String subschemaSubentryDN = null;

		if (schemaDnSR.hasMore()) {
			sr = schemaDnSR.next();
		}
		if (sr != null) {
			subschemaSubentry = sr.getAttributes().get("subschemaSubentry");
		}
		if (subschemaSubentry != null && subschemaSubentry.size() > 0) {
			subschemaSubentryDN = (String) subschemaSubentry.get();
		}

		if (subschemaSubentryDN != null) {
			// get schema attributes from subschemaSubentryDN
			Attributes schemaAttrs = schemaCtx.getAttributes(
							subschemaSubentryDN, attrsToReturn != null ? attrsToReturn : new String[]{"*", "+"});

			if (schemaAttrs != null) {
				for (int i = 0; i < attrsToReturn.length; i++) {
					Attribute schemaAttr = schemaAttrs.get(attrsToReturn[i]);
					if (schemaAttr != null) {
						attrsResult.put(schemaAttr.getID(), (List<String>) Collections.list(schemaAttr.getAll()));
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
	 * @param attrsNames table of attribute names to get
	 * @return Map of DNs of all entries that are returned by the directory with an associated map of attribute names and values (never null)
	 * @throws NamingException thrown if something goes wrong
	 */
	public Map<String, LscAttributes> getAttrsList(final String base,
					final String filter, final int scope, final List<String> attrsNames)
					throws NamingException {

		// sanity checks
		String searchBase = base == null ? "" : base;
		String searchFilter = filter == null ? DEFAULT_FILTER : filter;

		Map<String, LscAttributes> res = new HashMap<String, LscAttributes>();

		if (attrsNames == null || attrsNames.size() == 0) {
			LOGGER.error("No attribute names to read! Check configuration.");
			return res;
		}

		String[] attributes = new String[attrsNames.size()];
		attributes = attrsNames.toArray(attributes);

		SearchControls constraints = new SearchControls();
		constraints.setDerefLinkFlag(false);
		constraints.setReturningAttributes(attributes);
		constraints.setSearchScope(scope);
		constraints.setReturningObjFlag(true);

		try {
			boolean requestPagedResults = false;

			List<Control> extControls = new ArrayList<Control>();

			if (pageSize > 0) {
				requestPagedResults = true;
				LOGGER.debug("Using pagedResults control for {} entries at a time", pageSize);
			}

			if (requestPagedResults) {
				extControls.add(new PagedResultsControl(pageSize, Control.CRITICAL));
			}

			if (extControls.size() > 0) {
				ctx.setRequestControls(extControls.toArray(new Control[extControls.size()]));
			}

			byte[] pagedResultsResponse = null;
			do {
				NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, constraints);

				if (results != null) {
					Map<String, String> attrsValues = null;
					while (results.hasMoreElements()) {
						attrsValues = new HashMap<String, String>();

						SearchResult ldapResult = (SearchResult) results.next();

						// get the value for each attribute requested
						Iterator<String> ite = attrsNames.iterator();
						String attribute = null;
						while (ite.hasNext()) {
							attribute = ite.next();
							Attribute attr = ldapResult.getAttributes().get(attribute);
							if (attr != null && attr.get() != null) {
								attrsValues.put(attribute, (String) attr.get());
							}
						}

						res.put(ldapResult.getNameInNamespace(), new LscAttributes(attrsValues));
					}
				}
				Control[] respCtls = ctx.getResponseControls();
				if (respCtls != null) {
					for (int i = 0; i < respCtls.length; i++) {
						Control respCtl = respCtls[i];
						if (requestPagedResults && respCtl instanceof PagedResultsResponseControl) {
							pagedResultsResponse = ((PagedResultsResponseControl) respCtls[i]).getCookie();
						}
					}
				}

				if (requestPagedResults && pagedResultsResponse != null) {
					ctx.setRequestControls(new Control[]{
										new PagedResultsControl(pageSize, pagedResultsResponse, Control.CRITICAL)});
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

	/**
	 * Close connection before this object is deleted by the garbage collector.
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		// Close the TLS connection (revert back to the underlying LDAP association)
		if (tlsResponse != null) {
			tlsResponse.close();
		}

		// Close the connection to the LDAP server
		ctx.close();

		super.finalize();
	}

	/**
	 * Get the JNDI context.
	 * @return The LDAP context object in use by this class.
	 */
	public LdapContext getContext() {
		return ctx;
	}
}
