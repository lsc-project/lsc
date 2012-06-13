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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.shared.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.shared.ldap.codec.osgi.DefaultLdapCodecService;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueDecorator;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.AbstractControl;
import org.apache.directory.shared.ldap.model.message.controls.PersistentSearchImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.AsyncLdapSourceServiceType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdapDerefAliasesType;
import org.lsc.configuration.LdapServerType;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceCommunicationException;
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
public class SyncReplSourceService extends SimpleJndiSrcService implements IAsynchronousService, Closeable {

	protected static final Logger LOGGER = LoggerFactory.getLogger(SyncReplSourceService.class);

	private LdapAsyncConnection connection;
	
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

	public static LdapAsyncConnection getConnection(LdapConnectionType ldapConn) throws LscServiceConfigurationException {
		LdapURL url;
		try {
            url = new LdapURL(ldapConn.getUrl());
            LdapAsyncConnection conn = LdapConnectionFactory.getNetworkConnection(url.getHost(), (url.getPort() != -1 ? url.getPort() : 389));
            LdapConnectionConfig lcc = conn.getConfig();
			lcc.setSslProtocol(url.getScheme());
			lcc.setUseSsl("ldaps".equalsIgnoreCase(url.getScheme()));
			lcc.setName(lcc.getName());
//			lco.setFollowReferrals(ldapConn.getReferralHandling() == ReferralHandling.THROUGH);
			if(conn.connect()) {
				conn.bind(ldapConn.getUsername(), ldapConn.getPassword());
				return conn;
			} else {
				return null;
			}
		} catch (LdapURLEncodingException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (IOException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (LdapException e) {
            throw new LscServiceConfigurationException(e.toString(), e);
        }
	}
	
	public void close() throws IOException {
		if (! connection.close()) {
			throw new IOException("Can't close service");
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
	 * @param id Name of the entry to be returned, which is the name returned by
	 *            {@link #getListPivots()} (used for display only)
	 * @param pivotAttrs Map of attribute names and values, which is the data identifier in the
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
		    EntryCursor entryCursor = null;
            // When launching getBean in clean phase, the search base should be the Base DN, not the entry ID
            String searchBaseDn = (fromSameService ? id : baseDn);
            SearchScope searchScope = (fromSameService ? SearchScope.OBJECT : SearchScope.SUBTREE);
			if(getAttrs() != null) {
				entryCursor = connection.search(searchBaseDn, searchString, searchScope, getAttrs().toArray(new String[getAttrs().size()]));
			} else {
			    entryCursor = connection.search(searchBaseDn, searchString, searchScope);
			}

			srcBean = this.beanClass.newInstance();

			entryCursor.next();
			if(! entryCursor.available()) {
			    return null;
			}
			Entry entry =  entryCursor.get();
			// get dn
			srcBean.setMainIdentifier(entry.getDn().getName());
			srcBean.setDatasets(convertEntry(entry));
			entryCursor.getSearchResultDone();
			entryCursor.close();
			return srcBean;
		} catch (InstantiationException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (Exception e) {
			LOGGER.error("LDAP error while reading entry " + id + " (" + e + ")");
			LOGGER.debug(e.toString(), e);
		}
		return null;
	}

	@Override
	public java.util.Map.Entry<String, LscDatasets> getNextId() throws LscServiceException {
		Map<String, LscDatasets> temporaryMap = new HashMap<String, LscDatasets>(1);
		if(sf == null || sf.isCancelled()) {
			try {
				SearchRequest searchRequest = new SearchRequestImpl();
				searchRequest.addControl(getSearchContinuationControl(srsc.getServerType()));
				searchRequest.setBase(new Dn(getBaseDn()));
				searchRequest.setFilter(getFilterAll());
				searchRequest.setDerefAliases(getAlias(ldapConn.getDerefAliases()));
				searchRequest.setScope(org.apache.directory.shared.ldap.model.message.SearchScope.SUBTREE);
				searchRequest.addAttributes(getAttrsId().toArray(new String[getAttrsId().size()]));
				sf = getConnection(ldapConn).searchAsync(searchRequest);
			} catch (LdapInvalidDnException e) {
                throw new LscServiceException(e.toString(), e);
            } catch (LdapException e) {
                throw new LscServiceException(e.toString(), e);
            }
		}
		Response searchResponse = null;
		try {
			searchResponse = sf.get(1, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			LOGGER.warn("Interrupted search !");
		} catch (ExecutionException e) {
			LOGGER.warn("Execution exception while searching !");
		} catch (TimeoutException e) {
			LOGGER.warn("Timeout during search !");
		}
		if(searchResponse != null && searchResponse.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY) {
			SearchResultEntryDecorator sre = ((SearchResultEntryDecorator) searchResponse);
			temporaryMap.put(sre.getObjectName().toString(), convertEntry(sre.getEntry(), true));
			return temporaryMap.entrySet().iterator().next();
		} else if(searchResponse != null && searchResponse.getType() == MessageTypeEnum.SEARCH_RESULT_DONE){
		    LdapResult result = ((SearchResultDone)searchResponse).getLdapResult();
		    if(result.getResultCode() != ResultCodeEnum.SUCCESS) {
		        throw new LscServiceCommunicationException(result.getDiagnosticMessage(), null);
		    }
		    sf = null;
		}
        return null;
	}

    private org.apache.directory.shared.ldap.model.message.AliasDerefMode getAlias(LdapDerefAliasesType aliasesHandling) {
		switch(aliasesHandling) {
		case ALWAYS:
			return org.apache.directory.shared.ldap.model.message.AliasDerefMode.DEREF_ALWAYS;
		case FIND:
            return org.apache.directory.shared.ldap.model.message.AliasDerefMode.DEREF_FINDING_BASE_OBJ;
		case SEARCH:
            return org.apache.directory.shared.ldap.model.message.AliasDerefMode.DEREF_IN_SEARCHING;
		case NEVER:
		default:
            return org.apache.directory.shared.ldap.model.message.AliasDerefMode.NEVER_DEREF_ALIASES;
		}
	}

	public static org.apache.directory.shared.ldap.model.message.Control getSearchContinuationControl(LdapServerType serverType) throws LscServiceConfigurationException {
		switch(serverType) {
		case OPEN_LDAP:
		case APACHE_DS:
		    DefaultLdapCodecService codec = new DefaultLdapCodecService();
		    SyncRequestValueDecorator syncControl = new SyncRequestValueDecorator(codec);
		    syncControl.setMode(org.apache.directory.shared.ldap.extras.controls.SynchronizationModeEnum.REFRESH_AND_PERSIST);
		    return syncControl;
		case OPEN_DS:
		case OPEN_DJ:
		case ORACLE_DS:
		case SUN_DS:
		case NETSCAPE_DS:
		case NOVELL_E_DIRECTORY:
		    PersistentSearchImpl searchControl = new PersistentSearchImpl();
		    searchControl.setCritical(true);
			searchControl.setChangesOnly(true);
			searchControl.setReturnECs(false);
			searchControl.setChangeTypes(ChangeType.ADD_VALUE + ChangeType.DELETE_VALUE + ChangeType.MODDN_VALUE + ChangeType.MODIFY_VALUE);
			return searchControl;
		case ACTIVE_DIRECTORY:
			return new AbstractControl("1.2.840.113556.1.4.528", true) {};
		default:
			throw new LscServiceConfigurationException("Unknown or unsupported server type !");
		}
	}

	/**
	 * 
	 * @param entry
	 * @return
	 */
	private LscDatasets convertEntry(Entry entry) {
		return convertEntry(entry, false);
	}
	
    private LscDatasets convertEntry(org.apache.directory.shared.ldap.entry.Entry entry, boolean onlyFirstValue) {
        if(entry == null) return null;
        LscDatasets converted = new LscDatasets();
        Iterator<EntryAttribute> entryAttributes = entry.iterator();
        while(entryAttributes.hasNext()) {
            EntryAttribute attr = entryAttributes.next();
            if(attr != null && attr.size() > 0)  {
                Iterator<org.apache.directory.shared.ldap.entry.Value<?>> values = attr.iterator();
                if(!onlyFirstValue) {
                    Set<Object> datasetsValues = new HashSet<Object>();
                    while(values.hasNext()) {
                        org.apache.directory.shared.ldap.entry.Value<?> value = values.next();
                        datasetsValues.add(value.getString());
                    }
                    converted.getDatasets().put(attr.getId(), datasetsValues);
                } else {
                    org.apache.directory.shared.ldap.entry.Value<?> value = values.next();
                    converted.getDatasets().put(attr.getId(), value.getString());
                }
            }
        }
        return converted;
    }

	private LscDatasets convertEntry(Entry entry, boolean onlyFirstValue) {
		if(entry == null) return null;
		LscDatasets converted = new LscDatasets();
		Iterator<Attribute> entryAttributes = entry.iterator();
		while(entryAttributes.hasNext()) {
			Attribute attr = entryAttributes.next();
			if(attr != null && attr.size() > 0)  {
				Iterator<Value<?>> values = attr.iterator();
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
	 * @param entryCursor Unbounded ID LDAP SDK objects 
	 * @return LSC compatible map
	 * @throws LscServiceException 
	 */
	private Map<String, LscDatasets> convertSearchEntries(
			EntryCursor entryCursor) throws LscServiceException {
		Map<String, LscDatasets> converted = new HashMap<String, LscDatasets>();
		try {
			while (entryCursor.next()) {
				Entry entry = entryCursor.get();
				converted.put(entry.getDn().getName(), convertEntry(entry));
			}
			entryCursor.getSearchResultDone();
		} catch (Exception e) {
			throw new LscServiceException("Error while performing search. Results may be incomplete." + e, e);
		}
		return converted;
	}

	@Override
	public long getInterval() {
		return interval;
	}
}
