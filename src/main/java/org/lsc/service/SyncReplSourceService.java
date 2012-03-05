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

 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *    * Neither the name of the LSC Project nor the names of its
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
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.SearchCursor;
import org.apache.directory.ldap.client.api.exception.LdapException;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.ldap.client.api.message.SearchRequest;
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.shared.ldap.codec.controls.ControlImpl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControl;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.persistentSearch.PersistentSearchControl;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.AsyncLdapSourceServiceType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdapDerefAliasesType;
import org.lsc.configuration.LdapServerType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;
import org.lsc.jndi.SimpleJndiSrcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * You will find there the SyncRepl source service that can attach
 * to a compatible directory to get updates on the fly.
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class SyncReplSourceService extends SimpleJndiSrcService implements IAsynchronousService {

	protected static final Logger LOGGER = LoggerFactory.getLogger(SyncReplSourceService.class);

	private LdapConnection connection;
	
	private LdapConnectionType ldapConn;
	
	private AsyncLdapSourceServiceType srsc;
	
	/** The interval in seconds */
	private int interval;
	
	private SearchFuture sf;

	public SyncReplSourceService(final TaskType task)
			throws LscServiceConfigurationException {
		super(task);
		srsc = task.getAsyncLdapSourceService();
 		// Default interval
		interval = (srsc.getInterval() != null ? srsc.getInterval().intValue() : 5);
		
		ldapConn = (LdapConnectionType) srsc.getConnection().getReference();
		
		connection = getConnection(ldapConn);
	}

	public static LdapConnection getConnection(LdapConnectionType ldapConn) throws LscServiceConfigurationException {
		LdapConnectionConfig lcc = new LdapConnectionConfig();
		LdapURL url;
		try {
			url = new LdapURL(ldapConn.getUrl());
			lcc.setLdapHost(url.getHost());
			lcc.setLdapPort((url.getPort() != -1 ? url.getPort() : 389));
			lcc.setSslProtocol(url.getScheme());
			lcc.setUseSsl("ldaps".equalsIgnoreCase(url.getScheme()));
			lcc.setName(lcc.getName());
//			lco.setFollowReferrals(ldapConn.getReferralHandling() == ReferralHandling.THROUGH);
			LdapConnection conn = new LdapConnection(lcc);
			if(conn.connect()) {
				conn.bind(ldapConn.getUsername(), ldapConn.getPassword());
				return conn;
			} else {
				return null;
			}
		} catch (LdapURLEncodingException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (LdapException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (IOException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		}
	}
	
	@Override
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		try {
			return convertSearchEntries(connection.search(getBaseDn(), getFilterAll(), SearchScope.SUBTREE,
					getAttrsId().toArray(new String[getAttrsId().size()])));
		} catch (RuntimeException e) {
			throw new LscServiceException(e.toString(), e);
		} catch (LdapException e) {
			throw new LscServiceException(e.toString(), e);
		}
	}

	/**
	 * The simple object getter according to its identifier.
	 * 
	 * @param pivotName Name of the entry to be returned, which is the name returned by
	 *            {@link #getListPivots()} (used for display only)
	 * @param pivotAttributes Map of attribute names and values, which is the data identifier in the
	 *            source such as returned by {@link #getListPivots()}. It must identify a unique
	 *            entry in the source.
	 * @param fromSameService are the pivot attributes provided by the same service
	 * @return The bean, or null if not found
	 * @throws LscServiceException May throw a {@link NamingException} if the object is not found in the
	 *             directory, or if more than one object would be returned.
	 */
	@Override
	public IBean getBean(final String id, final LscDatasets pivotAttrs, boolean fromSameService) throws LscServiceException {
		IBean srcBean = null;
		String searchString = null;
		if(fromSameService || filterIdClean == null) {
			searchString = filterIdSync;
		} else {
			searchString = filterIdClean; 
		}

        searchString = Pattern.compile("\\{id\\}", Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(id);
		if (pivotAttrs != null && pivotAttrs.getDatasets() != null && pivotAttrs.getDatasets().size() > 0) {
			for (String attributeName : pivotAttrs.getAttributesNames()) {
				String valueId = pivotAttrs.getStringValueAttribute(attributeName.toLowerCase());
				searchString = Pattern.compile("\\{" + attributeName + "\\}", Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(valueId);
			}
		} else if (attrsId.size() == 1) {
			searchString = Pattern.compile("\\{" + attrsId.get(0) + "\\}", Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(id);
		} else {
			// this is kept for backwards compatibility but will be removed
			searchString = filterIdSync.replaceAll("\\{0\\}", id);
		}

		try {
//			SearchRequest searchRequest = new SearchRequest( id, SearchScope.BASE, searchString );
			String[] attrs = null;
			if(getAttrs() != null) {
				attrs = getAttrs().toArray(new String[getAttrs().size()]);
			}
			SearchCursor searchResponses = (SearchCursor) connection.search(id, searchString, SearchScope.OBJECT, attrs);

			srcBean = this.beanClass.newInstance();

			if(searchResponses.next() && searchResponses.get() instanceof SearchResultEntry) {
				SearchResultEntry sre = (SearchResultEntry) searchResponses.get();
				// get dn
				srcBean.setDistinguishName(sre.getObjectName().toString());
				srcBean.setDatasets(convertSearchEntry(sre));
				return srcBean;
			}
		} catch (InstantiationException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (LdapException e) {
			LOGGER.error("LDAP error while reading entry " + id + " (" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (Exception e) {
			LOGGER.error("LDAP error while reading entry " + id + " (" + e + ")");
			LOGGER.debug(e.toString(), e);
		}
		return null;
	}

	@Override
	public Entry<String, LscDatasets> getNextId() throws LscServiceException {
		Map<String, LscDatasets> temporaryMap = new HashMap<String, LscDatasets>(1);
		if(sf == null || sf.isCancelled()) {
			try {
				SearchRequest searchRequest = new SearchRequest();
				searchRequest.add(getSearchContinuationControl(srsc.getServerType()));
				searchRequest.setBaseDn(getBaseDn());
				searchRequest.setFilter(getFilterAll());
				searchRequest.setDerefAliases(getAlias(ldapConn.getDerefAliases()));
				searchRequest.setScope(SearchScope.SUBTREE);
				searchRequest.addAttributes(getAttrsId().toArray(new String[getAttrsId().size()]));
				sf = getConnection(ldapConn).searchAsync(searchRequest);
			} catch (LdapException e) {
				throw new LscServiceException(e.toString(), e);
			}
		}
		SearchResponse searchResponse = null;
		try {
			searchResponse = sf.get(1, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			LOGGER.warn("Interrupted search !");
		} catch (ExecutionException e) {
			LOGGER.warn("Execution exception while searching !");
		} catch (TimeoutException e) {
			LOGGER.warn("Timeout during search !");
		}
		if(searchResponse != null && searchResponse instanceof SearchResultEntry) {
			SearchResultEntry sre = ((SearchResultEntry) searchResponse);
			temporaryMap.put(sre.getObjectName().toString(), convertSearchEntry(sre, true));
			return temporaryMap.entrySet().iterator().next();
		} else {
			return null;
		}
	}

	private AliasDerefMode getAlias(LdapDerefAliasesType aliasesHandling) {
		switch(aliasesHandling) {
		case ALWAYS:
			return AliasDerefMode.DEREF_ALWAYS;
		case FIND:
			return AliasDerefMode.DEREF_FINDING_BASE_OBJ;
		case SEARCH:
			return AliasDerefMode.DEREF_IN_SEARCHING;
		case NEVER:
		default:
			return AliasDerefMode.NEVER_DEREF_ALIASES;
		}
	}

	public static Control getSearchContinuationControl(LdapServerType serverType) throws LscServiceConfigurationException {
		switch(serverType) {
		case OPEN_LDAP:
		case APACHE_DS:
			SyncRequestValueControl syncControl = new SyncRequestValueControl();
			syncControl.setMode(SynchronizationModeEnum.REFRESH_AND_PERSIST);
			return syncControl;
		case OPEN_DS:
		case OPEN_DJ:
		case ORACLE_DS:
		case SUN_DS:
		case NETSCAPE_DS:
		case NOVELL_E_DIRECTORY:
			PersistentSearchControl searchControl = new PersistentSearchControl();
			searchControl.setChangesOnly(true);
			searchControl.setChangeTypes(ChangeType.ADD_VALUE + ChangeType.DELETE_VALUE + ChangeType.MODDN_VALUE + ChangeType.MODIFY_VALUE);
			return searchControl;
		case ACTIVE_DIRECTORY:
			Control notificationControl = new ControlImpl("1.2.840.113556.1.4.528");
			notificationControl.setCritical(true);
			return notificationControl;
		default:
			throw new LscServiceConfigurationException("Unknown or unsupported server type !");
		}
	}

	/**
	 * 
	 * @param entry
	 * @return
	 */
	private LscDatasets convertSearchEntry(SearchResultEntry entry) {
		return convertSearchEntry(entry, false);
	}
	private LscDatasets convertSearchEntry(SearchResultEntry entry, boolean onlyFirstValue) {
		if(entry == null) return null;
		LscDatasets converted = new LscDatasets();
		Iterator<EntryAttribute> entryAttributes = entry.getEntry().iterator();
		while(entryAttributes.hasNext()) {
			EntryAttribute attr = entryAttributes.next();
			if(attr.getAll() != null)  {
				Iterator<Value<?>> values = attr.getAll();
				if(!onlyFirstValue) {
					Set<Object> datasetsValues = new HashSet<Object>();
					while(values.hasNext()) {
						Value<?> value = values.next();
						datasetsValues.add(value.getString());
					}
					converted.getDatasets().put(attr.getId(), datasetsValues);
				} else {
					Value<?> value = values.next();
					converted.getDatasets().put(attr.getId(), value.getString());
				}
			}
		}
		return converted;
	}

	/**
	 * Convert a search result entries list to a LSC ready to use map
	 * @param cursor Unbounded ID LDAP SDK objects 
	 * @return LSC compatible map
	 */
	private Map<String, LscDatasets> convertSearchEntries(
			Cursor<SearchResponse> cursor) {
		Map<String, LscDatasets> converted = new HashMap<String, LscDatasets>();
		for(SearchResponse sr : cursor) {
			if(sr instanceof SearchResultEntry) {
				SearchResultEntry sre = (SearchResultEntry) sr;
				converted.put(sre.getObjectName().toString(), convertSearchEntry(sre));
			}
		}
		return converted;
	}

	@Override
	public long getInterval() {
		return interval;
	}
}
