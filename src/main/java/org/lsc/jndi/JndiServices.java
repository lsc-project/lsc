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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.BasicControl;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.SortControl;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.codec.api.ControlFactory;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapURLEncodingException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.lsc.Configuration;
import org.lsc.LscDatasets;
import org.lsc.configuration.LdapAuthenticationType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdapDerefAliasesType;
import org.lsc.configuration.LdapReferralType;
import org.lsc.configuration.LdapVersionType;
import org.lsc.exception.LscConfigurationException;
import org.lsc.exception.LscServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General LDAP services wrapper.
 *
 * This class is designed to manage all the needed operations to the directory
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 * @author Jonathan Clarke &lt;jon@lsc-project.org&gt;
 */
public final class JndiServices {

	protected static final String TLS_CONFIGURATION = "java.naming.tls";

	protected static final String RELAX_RULES_CONTROL_OID = "1.3.6.1.4.1.4203.666.5.12";

	/** Default LDAP filter. */
	public static final String DEFAULT_FILTER = "objectClass=*";

	private static final Logger LOGGER = LoggerFactory.getLogger(JndiServices.class);

	/** the ldap ctx. */
	private LdapContext ctx;

	/** TLSResponse in case we use StartTLS */
	private StartTlsResponse tlsResponse;

	/** The context base dn. */
	private Dn contextDn;

	/** The instances cache. */
	private static Map<Properties, JndiServices> cache = new HashMap<Properties, JndiServices>();

	/** Number of results per page (through PagedResults extended control). */
	private int pageSize;

	private LdapUrl namingContext;

	/** Support for recursive deletion (default to false) */
	private boolean recursiveDelete;

	/** Send relax rules control when writing in directory (default to false) */
	private boolean relaxRules;

	/** Attribute name to sort on. */
	private String sortedBy;
	
	/** Remember connection properties to reconnect */
	private Properties connProps;
	
	/**
	 * Initiate the object and the connection according to the properties.
	 *
	 * @param connProps the connection properties to use to instantiate
	 * connection
	 * @throws NamingException thrown if a directory error is encountered
	 * @throws IOException thrown if an error occurs negotiating StartTLS operation
	 */
	private JndiServices(final Properties connProps) throws NamingException, IOException {
		this.connProps = connProps;
		initConnection();
	}

	private void initConnection()
			throws NamingException, IOException {
		// log new connection with it's details
		logConnectingTo(connProps);

		/* should we negotiate TLS? */
		if (connProps.get(TLS_CONFIGURATION) != null && (Boolean) connProps.get(TLS_CONFIGURATION)) {
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
			namingContext = new LdapUrl((String) ctx.getEnvironment().get(Context.PROVIDER_URL));
		} catch (LdapURLEncodingException e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw new NamingException(e.getMessage());
		}

		/* handle options */
		try {
			contextDn = namingContext.getDn() != null ?  namingContext.getDn() : new Dn("");
		} catch (LdapInvalidDnException e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw new NamingException(e.getMessage());
		}

		String pageSizeStr = (String) ctx.getEnvironment().get("java.naming.ldap.pageSize");
		if (pageSizeStr != null) {
			pageSize = Integer.parseInt(pageSizeStr);
		} else {
			pageSize = -1;
		}

		sortedBy = (String) ctx.getEnvironment().get("java.naming.ldap.sortedBy");
		
		String recursiveDeleteStr = (String) ctx.getEnvironment().get("java.naming.recursivedelete");
		if (recursiveDeleteStr != null) {
			recursiveDelete = Boolean.parseBoolean(recursiveDeleteStr);
		} else {
			recursiveDelete = false;
		}
		
		String relaxRulesStr = (String) ctx.getEnvironment().get("java.naming.relaxRules");
		if (relaxRulesStr != null) {
			relaxRules = Boolean.parseBoolean(relaxRulesStr);
		} else {
			relaxRules = false;
		}

		/* Load SyncRepl response control */
		LdapApiService ldapApiService = LdapApiServiceFactory.getSingleton();
		ControlFactory<?> factory = new SyncStateValueFactory( ldapApiService );
		ldapApiService.registerResponseControl( factory );
		/* Load Persistent Search response control */
		factory = new PersistentSearchFactory(ldapApiService);
		ldapApiService.registerResponseControl(factory);
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
			if (connProps.get(TLS_CONFIGURATION) != null && (Boolean) connProps.get(TLS_CONFIGURATION)) {
				sb.append(" with STARTTLS extended operation");
			}

			LOGGER.info(sb.toString());
		}
	}

	public static JndiServices getInstance(final Properties props) throws NamingException, IOException {
		return getInstance(props, false);
	}

	/**
	 * Instance getter. Manage a connections cache and return the good service
	 * @param props the connection properties
	 * @return the instance
	 * @throws IOException
	 * @throws NamingException
	 */
	public static JndiServices getInstance(final Properties props, boolean forceNewConnection) throws NamingException, IOException {
        if(forceNewConnection) {
            return new JndiServices(props);
        } else {
        	if (!cache.containsKey(props)) {
        		cache.put(props, new JndiServices(props));
        	}
        	JndiServices instance = cache.get(props);
        	if (instance.ctx == null) {
        		instance.initConnection();
        	}
			return instance;
        }
	}

	public static Properties getLdapProperties(LdapConnectionType connection) throws LscConfigurationException {
		Properties props = new Properties();
		props.setProperty(DirContext.INITIAL_CONTEXT_FACTORY, (connection.getFactory() != null ? connection.getFactory() : "com.sun.jndi.ldap.LdapCtxFactory"));
		props.put(TLS_CONFIGURATION, connection.isTlsActivated());
		if(connection.getUsername() != null) {
			props.setProperty(DirContext.SECURITY_AUTHENTICATION, connection.getAuthentication().value());
			props.setProperty(DirContext.SECURITY_PRINCIPAL, connection.getUsername());
			if(connection.getAuthentication().equals(LdapAuthenticationType.GSSAPI)) {
				if(System.getProperty("java.security.krb5.conf") != null) {
					throw new RuntimeException("Multiple Kerberos connections not supported (existing value: " 
							+ System.getProperty("java.security.krb5.conf") + "). Need to set another LSC instance or unset system property !");
				} else {
					System.setProperty("java.security.krb5.conf", new File(Configuration.getConfigurationDirectory(), "krb5.ini").getAbsolutePath());
				}
				if(System.getProperty("java.security.auth.login.config") != null) {
					throw new RuntimeException("Multiple JAAS not supported (existing value: " 
							+ System.getProperty("java.security.auth.login.config") + "). Need to set another LSC instance or unset system property !");
				} else {
					System.setProperty("java.security.auth.login.config" , new File(Configuration.getConfigurationDirectory(), "gsseg_jaas.conf").getAbsolutePath());
				}
				props.setProperty("javax.security.sasl.server.authentication", ""+connection.isSaslMutualAuthentication());
//				props.put("java.naming.security.sasl.authorizationId", "dn:" + connection.getUsername());
				props.put("javax.security.auth.useSubjectCredsOnly", "true");
				props.setProperty("javax.security.sasl.qop", connection.getSaslQop().value());
				try {
					LoginContext lc = new LoginContext(JndiServices.class.getName(), new KerberosCallbackHandler(connection.getUsername(), connection.getPassword()));
					lc.login();
				} catch (LoginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				props.setProperty(DirContext.SECURITY_CREDENTIALS, connection.getPassword());
			}
		} else {
			props.setProperty(DirContext.SECURITY_AUTHENTICATION, "none");
		}
		try {
		    LdapUrl connectionUrl = new LdapUrl(connection.getUrl());
            if(connectionUrl.getHost() == null) {
                if(LOGGER.isDebugEnabled()) LOGGER.debug("Hostname is empty in LDAP URL, will try to lookup through the naming context ...");
                String domainExt = convertToDomainExtension(connectionUrl.getDn());
                if(domainExt != null) {
                    String hostname = lookupLdapSrvThroughDNS("_ldap._tcp." + domainExt);
                    if(hostname != null) {
                        connectionUrl.setHost(hostname.substring(0, hostname.indexOf(":")));
                        connectionUrl.setPort(Integer.parseInt(hostname.substring(hostname.indexOf(":")+1)));
                        connection.setUrl(connectionUrl.toString());
                    }
                }
            }
        }
        catch (LdapURLEncodingException e) {
            throw new LscConfigurationException(e);
        }
		props.setProperty(DirContext.PROVIDER_URL, connection.getUrl());
		if(connection.getReferral() != null) {
			props.setProperty(DirContext.REFERRAL, connection.getReferral().value().toLowerCase());
		} else {
			props.setProperty(DirContext.REFERRAL, LdapReferralType.IGNORE.value().toLowerCase());
		}
		if(connection.getDerefAliases() != null) {
			props.setProperty("java.naming.ldap.derefAliases", getDerefJndiValue(connection.getDerefAliases()));
		} else {
			props.setProperty("java.naming.ldap.derefAliases", getDerefJndiValue(LdapDerefAliasesType.NEVER));
		}
		if(connection.getBinaryAttributes() != null) {
			props.setProperty("java.naming.ldap.attributes.binary", StringUtils.join(connection.getBinaryAttributes().getString(), " "));
		}
		if(connection.getPageSize() != null) {
			props.setProperty("java.naming.ldap.pageSize", "" + connection.getPageSize());
		}
		if(connection.getSortedBy() != null) {
			props.setProperty("java.naming.ldap.sortedBy", connection.getSortedBy());
		}
		props.setProperty("java.naming.ldap.version", (connection.getVersion() == LdapVersionType.VERSION_2 ? "2" : "3" ));
		if(connection.isRecursiveDelete() != null) {
			props.setProperty("java.naming.recursivedelete", Boolean.toString(connection.isRecursiveDelete()));
		}
		if(connection.isRelaxRules() != null) {
			props.setProperty("java.naming.relaxRules", Boolean.toString(connection.isRelaxRules()));
		}
		return props;
	}
	
	private static String lookupLdapSrvThroughDNS(String hostname) {
	    Properties env = new Properties();
	    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
	    env.put("java.naming.provider.url", "dns:");
	    DirContext ctx;
        try {
            ctx = new InitialDirContext(env);
            if(ctx != null) {
                Attributes attrs = ctx.getAttributes(hostname, new String[] { "SRV" });
                String[] attributes = ((String)attrs.getAll().next().get()).split(" ");
                return attributes[3] + ":" + attributes[2];
            }
        }
        catch (NamingException e) {
        }
        return hostname + ":389";
    }

    private static String convertToDomainExtension(Dn dn) {
	    String fqdn = "";
	    List<Rdn> rdns = dn.getRdns();
	    for(Rdn rdn: rdns) {
	        if(!rdn.getAva().getType().equalsIgnoreCase("dc")) {
	            return null;
	        }
	        if(fqdn.length() > 0) {
	            fqdn = rdn.getValue() + "." + fqdn;
	        } else {
	            fqdn = rdn.getValue();
	        }
	    }
	    return fqdn;
    }

    private static String getDerefJndiValue(LdapDerefAliasesType derefAliases) {
		switch(derefAliases) {
		case ALWAYS:
			return "always";
		case FIND:
			return "finding";
		case SEARCH:
			return "searching";
		case NEVER:
			return "never";
		}
		return "";
	}

	public static JndiServices getInstance(LdapConnectionType connection) {
		try {
			return getInstance(getLdapProperties(connection));
		} catch (Exception e) {
			LOGGER.error("Error opening LDAP connection \"" + connection.getName() + "\" to " + connection.getUrl() + " (" + e.toString() + ")");
			throw new RuntimeException(e);
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
		try {
			return doGetEntry(base, filter, sc, scope);
		} catch (NamingException nex) {
			if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
				LOGGER.warn("Communication error, retrying: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				try {
					initConnection();
				} catch (IOException ioex) {
					LOGGER.error("I/O error: " + ioex.getMessage());
					LOGGER.debug(ioex.getMessage(), ioex);
					// throw the initial communication exception
					throw nex;
				}
				return doGetEntry(base, filter, sc, scope);
			} else {
				throw nex;
			}
		}
	}

	private SearchResult doGetEntry(final String base, final String filter,
			final SearchControls sc, final int scope) throws NamingException {
		//sanity checks
		String searchBase = base == null ? "" : base;
		String searchFilter = filter == null ? DEFAULT_FILTER : filter;

		NamingEnumeration<SearchResult> ne = null;
		try {
			sc.setSearchScope(scope);
			String rewrittenBase = null;
			if (!getContextDn().isEmpty() && searchBase.toLowerCase().endsWith(contextDn.toString().toLowerCase())) {
				if (!searchBase.equalsIgnoreCase(contextDn.toString())) {
					rewrittenBase = searchBase.substring(0, searchBase.toLowerCase().lastIndexOf(contextDn.toString().toLowerCase()) - 1);
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
				LOGGER.error("Too many entries returned (base: \"{}\", filter: \"{}\")",
								searchBase, searchFilter);
				throw new SizeLimitExceededException("Too many entries returned (base: \"" + searchBase + "\", filter: \"" + searchFilter + "\")");
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
		try {
			Dn lowerCasedContextDn = (getContextDn().isEmpty()) ? null : new Dn(contextDn.toString().toLowerCase());
			Dn lowerCasedBaseDn = new Dn(base.toLowerCase());
			if (!lowerCasedBaseDn.isDescendantOf(lowerCasedContextDn)) {
				return base;
			}
			
			if (lowerCasedBaseDn.equals(lowerCasedContextDn)) {
				return "";
			}
			
			Dn lowerCasedRelativeDn = lowerCasedBaseDn.getDescendantOf(lowerCasedContextDn);
			return base.substring(0, lowerCasedRelativeDn.toString().length());
		} catch (LdapInvalidDnException e) {
			throw new RuntimeException(e);
		}
	}

	public SearchResult readEntry(final String base, final String filter,
					final boolean allowError, final SearchControls sc) throws NamingException {
		try {
			return doReadEntry(base, filter, allowError, sc);
		} catch (NamingException nex) {
			if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
				LOGGER.info("Communication error, retrying: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				try {
					initConnection();
				} catch (IOException ioex) {
					LOGGER.error("I/O error: " + ioex.getMessage());
					LOGGER.debug(ioex.getMessage(), ioex);
					// throw the initial communication exception
					throw nex;
				}
				return doReadEntry(base, filter, allowError, sc);
			} else {
				throw nex;
			}
		}
	}
	
	private SearchResult doReadEntry(final String base, final String filter,
			final boolean allowError, final SearchControls sc) throws NamingException {
		NamingEnumeration<SearchResult> ne = null;
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		try {
			ne = ctx.search(rewriteBase(base), filter, sc);
		} catch (NamingException nex) {
            if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
                throw nex;
            }
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
				LOGGER.error("Too many entries returned (base: \"{}\")", base);
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
		try {
			return doGetDnList(base, filter, scope);
		} catch (NamingException nex) {
			if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
				LOGGER.warn("Communication error, retrying: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				try {
					initConnection();
				} catch (IOException ioex) {
					LOGGER.error("I/O error: " + ioex.getMessage());
					LOGGER.debug(ioex.getMessage(), ioex);
					// throw the initial communication exception
					throw nex;
				}
				return doGetDnList(base, filter, scope);
			} else {
				throw nex;
			}
		}
	}
	
	private List<String> doGetDnList(final String base, final String filter,
			final int scope) throws NamingException {
		NamingEnumeration<SearchResult> ne = null;
		List<String> iist = new ArrayList<String>();
		try {
			SearchControls sc = new SearchControls();
			sc.setDerefLinkFlag(false);
			sc.setReturningAttributes(new String[]{"1.1"});
			sc.setSearchScope(scope);
			sc.setReturningObjFlag(true);
			ne = ctx.search(base, filter, sc);
			
			String completedBaseDn = "";
			if (base.length() > 0) {
				completedBaseDn = "," + base;
			}
			while (ne.hasMoreElements()) {
				iist.add(((SearchResult) ne.next()).getName() + completedBaseDn);
			}
		} catch (NamingException e) {
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			throw e;
		}
		return iist;
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
		try {
			return doApply(jm);
		} catch (CommunicationException cex) {
			LOGGER.warn("Communication error, retrying: " + cex.getMessage());
			LOGGER.debug(cex.getMessage(), cex);
			try {
				initConnection();
			} catch (IOException ioex) {
				LOGGER.error("I/O error: " + ioex.getMessage());
				LOGGER.debug(ioex.getMessage(), ioex);
				// throw the initial communication exception
				throw cex;
			} catch (NamingException nex) {
				LOGGER.error("Naming error: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				// throw the initial communication exception
				throw cex;
			}
			return doApply(jm);
		}
	}

	private boolean doApply(final JndiModifications jm) throws CommunicationException {
		if (jm == null) {
			return true;
		}
		
		try {
			// Get a derived context to be able to use controls without impacting/being impacted by other thread sharing this context
			LdapContext updateCtx = getContext(true);

			switch (jm.getOperation()) {

				case ADD_ENTRY:
					updateCtx.createSubcontext(
									new LdapName(rewriteBase(jm.getDistinguishName())),
									getAttributes(jm.getModificationItems(), true));
					break;

				case DELETE_ENTRY:
					if (recursiveDelete) {
						deleteChildrenRecursively(updateCtx, rewriteBase(jm.getDistinguishName()));
					} else {
						updateCtx.destroySubcontext(new LdapName(rewriteBase(jm.getDistinguishName())));
					}
					break;

				case MODIFY_ENTRY:
					Object[] table = jm.getModificationItems().toArray();
					ModificationItem[] mis = new ModificationItem[table.length];
					System.arraycopy(table, 0, mis, 0, table.length);
					updateCtx.modifyAttributes(new LdapName(rewriteBase(jm.getDistinguishName())), mis);
					break;

				case MODRDN_ENTRY:
					//We do not display this warning if we do not apply the modification with the option modrdn = false
					LOGGER.warn("WARNING: updating the RDN of the entry will cancel other modifications! Relaunch synchronization to complete update.");
					updateCtx.rename(
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
							errorMessage.append("recursively ");
						}
						errorMessage.append("deleting");
						break;
				}
				errorMessage.append(" entry ").append(jm.getDistinguishName());
				errorMessage.append(" in directory :").append(ne.toString());

				LOGGER.error(errorMessage.toString());
			}
			
			if (ne instanceof CommunicationException) {
				// we lost the connection to the source or destination, stop everything!
				throw (CommunicationException) ne;
			}
			if (ne instanceof ServiceUnavailableException) {
				// we lost the connection to the source or destination, stop everything!
				CommunicationException ce = new CommunicationException(ne.getExplanation());
				ce.setRootCause(ne);
				throw ce;
			}
			
			return false;
		}
	}

	/**
	 * Delete children recursively
	 * @param distinguishName the tree head to delete
	 * @throws NamingException thrown if an error is encountered
	 */
	private void deleteChildrenRecursively(LdapContext updateCtx, String distinguishName) throws NamingException {
		try {
			doDeleteChildrenRecursively(updateCtx, distinguishName);
			return;
		} catch (NamingException nex) {
			if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
				LOGGER.warn("Communication error, retrying: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				try {
					initConnection();
				} catch (IOException ioex) {
					LOGGER.error("I/O error: " + ioex.getMessage());
					LOGGER.debug(ioex.getMessage(), ioex);
					// throw the initial communication exception
					throw nex;
				}
				doDeleteChildrenRecursively(getContext(true), distinguishName);
				return;
			} else {
				throw nex;
			}
		}
	}

	private void doDeleteChildrenRecursively(LdapContext updateCtx, String distinguishName) throws NamingException {
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		NamingEnumeration<SearchResult> ne = ctx.search(distinguishName, DEFAULT_FILTER, sc);
		while (ne.hasMore()) {
			SearchResult sr = (SearchResult) ne.next();
			String childrenDn = rewriteBase(sr.getName() + "," + distinguishName);
			deleteChildrenRecursively(updateCtx, childrenDn);
		}
		updateCtx.destroySubcontext(new LdapName(distinguishName));
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
		for (ModificationItem mi : modificationItems) {
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
	 * @return the map of name =&gt; attribute
	 * @throws NamingException
	 *                 thrown if something goes wrong (bad
	 */
	@SuppressWarnings("unchecked")
	public Map<String, List<String>> getSchema(final String[] attrsToReturn) throws NamingException {
		Map<String, List<String>> attrsResult = new HashMap<String, List<String>>();

		// connect to directory
		Hashtable<String, String> props = (Hashtable<String, String>) ctx.getEnvironment();
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
				for(String attr: attrsToReturn) {
					Attribute schemaAttr = schemaAttrs.get(attr);
					if (schemaAttr != null) {
						attrsResult.put(schemaAttr.getID(), (List<String>) Collections.list(schemaAttr.getAll()));
					}
				}
			}
		}

		return attrsResult;
	}

	public List<String> sup(String dn, int level) throws NamingException {
		int ncLevel = (new LdapName(contextDn.toString())).size();

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
	public Map<String, LscDatasets> getAttrsList(final String base,
					final String filter, final int scope, final List<String> attrsNames)
					throws NamingException {
		try {
			return doGetAttrsList(base, filter, scope, attrsNames);
		} catch (NamingException nex) {
			if (nex instanceof CommunicationException || nex instanceof ServiceUnavailableException) {
				LOGGER.warn("Communication error, retrying: " + nex.getMessage());
				LOGGER.debug(nex.getMessage(), nex);
				try {
					initConnection();
				} catch (IOException ioex) {
					LOGGER.error("I/O error: " + ioex.getMessage());
					LOGGER.debug(ioex.getMessage(), ioex);
					// throw the initial communication exception
					throw nex;
				}
				return doGetAttrsList(base, filter, scope, attrsNames);
			} else {
				throw nex;
			}
		}
	}

    /**
     * Retrieve a specific attribute from an object
     * 
     * @param objectDn
     * @param attribute
     * @return
     * @throws LscServiceException
     */
    public List<String> getAttributeValues(String objectDn, String attribute) throws LscServiceException {
        List<String> values = null;
        try {
            // Setup search
            SearchControls sc = new SearchControls();
            sc.setDerefLinkFlag(false);
            sc.setReturningAttributes(new String[] { attribute });
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);
            sc.setReturningObjFlag(true);

            // Retrieve attribute values
            SearchResult res = getEntry(objectDn, "objectClass=*", sc, SearchControls.OBJECT_SCOPE);
            Attribute attr = res.getAttributes().get(attribute);
            if (attr != null) {
                values = new ArrayList<String>();
                NamingEnumeration<?> enu = attr.getAll();
                while (enu.hasMoreElements()) {
                    Object val = enu.next();
                    values.add(val.toString());
                }
            }
        } catch (NamingException e) {
            throw new LscServiceException(e);
        }
        return values;
    }

	public Map<String, LscDatasets> doGetAttrsList(final String base,
			final String filter, final int scope, final List<String> attrsNames)
			throws NamingException {

		// sanity checks
		String searchBase = base == null ? "" : rewriteBase(base);
		String searchFilter = filter == null ? DEFAULT_FILTER : filter;

		Map<String, LscDatasets> res = new LinkedHashMap<String, LscDatasets>();

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
			
			if(sortedBy != null) {
			    extControls.add(new SortControl(sortedBy, Control.CRITICAL));
			}

			if (extControls.size() > 0) {
				ctx.setRequestControls(extControls.toArray(new Control[extControls.size()]));
			}

			byte[] pagedResultsResponse = null;
			do {
				NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, constraints);

				if (results != null) {
					Map<String, Object> attrsValues = null;
					while (results.hasMoreElements()) {
						attrsValues = new HashMap<String, Object>();

						SearchResult ldapResult = (SearchResult) results.next();

						// get the value for each attribute requested
						for (String attributeName : attrsNames) {
							Attribute attr = ldapResult.getAttributes().get(attributeName);
							if (attr != null && attr.get() != null) {
								attrsValues.put(attributeName, attr.get());
							}
						}

						res.put(ldapResult.getNameInNamespace(), new LscDatasets(attrsValues));
					}
				}
				
				Control[] respCtls = ctx.getResponseControls();
				if (respCtls != null) {
					for(Control respCtl : respCtls) {
						if (requestPagedResults && respCtl instanceof PagedResultsResponseControl) {
							pagedResultsResponse = ((PagedResultsResponseControl) respCtl).getCookie();
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
        } catch (CommunicationException e) {
            // Avoid handling the communication exception as a generic one
            throw e;
        } catch (ServiceUnavailableException e) {
            // Avoid handling the service unavailable exception as a generic one
            throw e;
		} catch (NamingException e) {
			// clear requestControls for future use of the JNDI context
			ctx.setRequestControls(null);
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
			
		} catch (IOException e) {
			// clear requestControls for future use of the JNDI context
			ctx.setRequestControls(null);
			LOGGER.error(e.toString());
			LOGGER.debug(e.toString(), e);
		}
		return res;
	}

	/**
	 * @return the contextDn
	 */
	public String getContextDn() {
		return contextDn.toString();
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
		if (ctx != null) {
			ctx.close();
			ctx = null;
		}

		super.finalize();
	}

	/**
	 * Get the JNDI context.
	 * @return The LDAP context object in use by this class.
	 * @throws NamingException 
	 */
	public LdapContext getContext() throws NamingException {
		return getContext(false);
	}
	
	/**
	 * Get the initial JNDI context or get a derived context to be able to use controls without 
	 * impacting or being impacted by other threads sharing a same context
	 * @param forUpdates if this derived context is for updates
	 * @return
	 * @throws NamingException
	 */
	public LdapContext getContext(boolean forUpdates) throws NamingException {
		if (forUpdates && relaxRules) {
			LOGGER.debug("Using relax rules control to apply modifications");
			LdapContext newCtx = ctx.newInstance(null);
			Control[] controls = newCtx.getRequestControls();
			if (controls == null) {
				controls = new Control[0];
			}
			int length = newCtx.getRequestControls().length;
			controls = Arrays.copyOf(controls, length + 1);
			controls[length] = new BasicControl(RELAX_RULES_CONTROL_OID, Control.CRITICAL, null);
			newCtx.setRequestControls(controls);
			return newCtx;
		}
		// No need to create a derived context
		return ctx;
	}

	public String completeDn(String dn) {
		if(!dn.toLowerCase().endsWith(contextDn.toString().toLowerCase())) {
			if(dn.length() > 0) {
				return dn + ","  + contextDn.toString();
			} else {
				return contextDn.toString();
			}
		}
		return dn;
	}
	
	public static CallbackHandler getCallbackHandler(String user, String pass) {
		return new KerberosCallbackHandler(user, pass);
	}
}

class KerberosCallbackHandler implements CallbackHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KerberosCallbackHandler.class);
	private String user;
	private String pass;
	
	public KerberosCallbackHandler(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	public void handle(Callback[] cbs) throws IOException,
			UnsupportedCallbackException {
		for(Callback cb: cbs) {
			if(cb instanceof NameCallback) {
				((NameCallback)cb).setName(user);
			} else if(cb instanceof PasswordCallback) {
				((PasswordCallback)cb).setPassword(pass.toCharArray());
			} else {
				LOGGER.error("Unknown callback: " + cb.toString());
			}
		}
	}
}
