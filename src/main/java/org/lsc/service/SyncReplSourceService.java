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
 *         Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *         Thomas Chemineau &lt;thomas@lsc-project.org&gt;
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.service;

import java.io.Closeable;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.extras.controls.SynchronizationModeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncRequest.SyncRequestValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncRequest.SyncRequestValueImpl;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateTypeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValue;
import org.apache.directory.api.ldap.extras.intermediate.syncrepl.SyncInfoValue;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.message.controls.AbstractControl;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearchImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.lsc.LscDatasets;
import org.lsc.beans.IBean;
import org.lsc.configuration.*;
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
	
	/** The interval in milliseconds */
	private int interval;
	
	/** The SearchFuture instance that will wait for any update from the LDAP server */
	private SearchFuture searchFuture;
	
	/** A variable used to compute the duration of the refresh phase */
    private long refreshStart = 0L;
    
    /** 
     * A fake entry attribute used when an entry is deleted. It will be
     * stored into the created LscDataSetsfor deleted entries
     **/
    public static final String DELETED_ENTRY = "_deleted-entry";
    
    // A small speedup: no reason  to compile this pattern many times
    private static final Pattern ID_PATTERN = Pattern.compile("\\{id\\}", Pattern.CASE_INSENSITIVE);


	public SyncReplSourceService(final TaskType task)
			throws LscServiceConfigurationException {
		super(task);
		srsc = task.getAsyncLdapSourceService();
		// Default interval
		interval = (srsc.getInterval() != null ? srsc.getInterval().intValue() : 5) * 1000;
		
		ldapConn = (LdapConnectionType) srsc.getConnection().getReference();
		
		connection = getConnection(ldapConn);
	}

	public static LdapAsyncConnection getConnection(LdapConnectionType ldapConn) throws LscServiceConfigurationException {
		LdapUrl url;
		try {
			url = new LdapUrl(ldapConn.getUrl());
			boolean isLdaps = "ldaps://".equalsIgnoreCase(url.getScheme());
			int port = url.getPort();
			if (port == -1) {
				port = isLdaps ? 636 : 389;
			}
			LdapAsyncConnection conn = new LdapNetworkConnection(url.getHost(), port);
			LdapConnectionConfig lcc = conn.getConfig();
			lcc.setUseSsl(isLdaps);
			lcc.setUseTls(ldapConn.isTlsActivated());
			
			/* Use default SUN TrustManager. See https://issues.apache.org/jira/browse/DIRAPI-91 */
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			lcc.setTrustManagers(tmf.getTrustManagers());
			
			if(ldapConn.getBinaryAttributes() != null) {
				DefaultConfigurableBinaryAttributeDetector bad = new DefaultConfigurableBinaryAttributeDetector();
				bad.addBinaryAttribute(ldapConn.getBinaryAttributes().getString().toArray(new String[0]));
				lcc.setBinaryAttributeDetector(bad);
			}
			if(conn.connect()) {
				conn.bind(ldapConn.getUsername(), ldapConn.getPassword());
				return conn;
			} else {
				return null;
			}
//		} catch (org.apache.directory.shared.ldap.model.exception.LdapURLEncodingException e) {
//			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (LdapException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		} catch (KeyStoreException e) {
			throw new LscServiceConfigurationException(e.toString(), e);
		}
	}
	
	@Override
	public void close() throws IOException {
		connection.close();
	}
	
	@Override
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		try {
			if (!connection.isConnected()) {
				connection = getConnection(ldapConn);
			}
			return convertSearchEntries(connection.search(getBaseDn(), getFilterAll(), SearchScope.SUBTREE,
					getAttrsId().toArray(new String[0])));
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

		searchString = ID_PATTERN.matcher(searchString).replaceAll(Matcher.quoteReplacement(id));
		
		if (pivotAttrs != null && pivotAttrs.getDatasets() != null && pivotAttrs.getDatasets().size() > 0) {
			for (String attributeName : pivotAttrs.getAttributesNames()) {
				String valueId = pivotAttrs.getValueForFilter(attributeName.toLowerCase());
				searchString = Pattern.compile("\\{" + attributeName + "\\}", 
				        Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(Matcher.quoteReplacement(valueId));
			}
		} else if (attrsId.size() == 1) {
			searchString = Pattern.compile("\\{" + attrsId.get(0) + "\\}", 
			        Pattern.CASE_INSENSITIVE).matcher(searchString).replaceAll(Matcher.quoteReplacement(id));
		} else {
			// this is kept for backwards compatibility but will be removed
			searchString = filterIdSync.replaceAll("\\{0\\}", id);
		}

		try {
			EntryCursor entryCursor = null;
			
			// When launching getBean in clean phase, the search base should be the Base DN, not the entry ID
			String searchBaseDn = (fromSameService ? id : baseDn);
			SearchScope searchScope = (fromSameService ? SearchScope.OBJECT : SearchScope.SUBTREE);
			
			if (!connection.isConnected()) {
				connection = getConnection(ldapConn);
			}
			
			if(getAttrs() != null) {
				List<String> attrList = new ArrayList<String>(getAttrs());
				attrList.addAll(pivotAttrs.getAttributesNames());
				entryCursor = connection.search(searchBaseDn, searchString, searchScope, 
				        attrList.toArray(new String[0]));
			} else {
				entryCursor = connection.search(searchBaseDn, searchString, searchScope);
			}

			// Fetch the first entry found, and close the cursor
			for ( Entry entry:entryCursor ) {
	            srcBean = beanClass.newInstance();

	            srcBean.setMainIdentifier(entry.getDn().getName());
	            srcBean.setDatasets(convertEntry(entry));
	            
	            break;
			}

			entryCursor.close();
			
			return srcBean;
		} catch (InstantiationException e) {
			LOGGER.error("Bad class name: {}({})", beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bad class name: {}({})", beanClass.getName(), e);
			LOGGER.debug(e.toString(), e);
		} catch (Exception e) {
			LOGGER.error("LDAP error while reading entry {}({})", id, e);
			LOGGER.debug(e.toString(), e);
		}
		return null;
	}
	
	
	/**
	 * Create the searchFuture
	 * 
	 * @throws LscServiceException
	 */
	private void createSearchFuture() throws LscServiceException {
        try {
            // Prepare a search Request with 
            SearchRequest searchRequest = new SearchRequestImpl();
            
            // Create the proper control, depending on the targeted server:
            // OpenLDAP/ApacheDS: SyncRequestValue control
            // Microsoft AD: LDAP_SERVER_NOTIFICATION_OID control
            // Most of the other servers: PersistentSearch control
            Control searchContinuationControl = getSearchContinuationControl(srsc.getServerType());
            
            searchRequest.addControl(searchContinuationControl);
            
            // We will use the configured base DN and filter
            searchRequest.setBase(new Dn(getBaseDn()));
            searchRequest.setFilter(getFilterAll());
            searchRequest.setDerefAliases(getAlias(ldapConn.getDerefAliases()));
            searchRequest.setScope(SearchScope.SUBTREE);
            searchRequest.addAttributes(getAttrsId().toArray(new String[0]));
            
            // Reclaim all attributes so that we don't have to do it later.
            //searchRequest.addAttributes(SchemaConstants.ALL_ATTRIBUTES_ARRAY);
            
            // Now do a search in asynchronous mode
            searchFuture = getConnection(ldapConn).searchAsync(searchRequest);
            refreshStart = System.currentTimeMillis();
        } catch (LdapInvalidDnException e) {
            throw new LscServiceException(e.toString(), e);
        } catch (LdapException e) {
            throw new LscServiceException(e.toString(), e);
        }
	}
	
	@Override
	public Map.Entry<String, LscDatasets> getNextId() throws LscServiceException {
		if ((searchFuture == null) || searchFuture.isCancelled()) {
		    createSearchFuture();
		}
		
		// Ok, we are ready. Get the first entry that waits
		try {
		    
		    Response searchResponse = searchFuture.get();
		    
		    if (searchResponse == null) {
		        // Not a SearchResponse instance...
		        return null;
		    } else if ( searchResponse instanceof SearchResultEntry) {
		        // We've got something that may be an entry, a referral or an SyncInfo Value
		        // Intermediate response (at the end of the refresh phase)
	            SearchResultEntry searchResultEntry = ((SearchResultEntry) searchResponse);
	            Dn entryDn = searchResultEntry.getObjectName();
	                
	            LOGGER.info( "Processing entry {}", entryDn);
            
	            switch (searchResultEntry.getType()) {
	                case SEARCH_RESULT_ENTRY:
	                    // A plain entry. Let's check it's status, it may be for a deleted
	                    // entry
	                    LscDatasets datasets = convertEntry(searchResultEntry.getEntry(), true);
	                    
	                    // Check if it's a deletion
	                    SyncStateValue syncStateCtrl = ( SyncStateValue ) searchResponse.getControl( SyncStateValue.OID );
	                    
	                    if ((syncStateCtrl != null) && (syncStateCtrl.getSyncStateType() == SyncStateTypeEnum.DELETE)) {
	                        // It's a deletion, add a special attribute that is not valid in LDAP
	                        // (because of the starting '_'. That will mark the entry as deleted
	                        datasets.getDatasets().put(DELETED_ENTRY, entryDn);
	                    }

	                    return Map.entry(entryDn.toString(), datasets);
	                    
	                case SEARCH_RESULT_REFERENCE:
	                    // TODO...
	                    return null;
	                    
	                case SEARCH_RESULT_DONE:
	                    // This is the end, beautiful friend,
	                    // This is the end, my only friend
	                    // (The Doors)
	                    LdapResult result = ((SearchResultDone)searchResultEntry).getLdapResult();
	                    
	                    if (result.getResultCode() != ResultCodeEnum.SUCCESS) {
	                        throw new LscServiceCommunicationException(result.getDiagnosticMessage(), null);
	                    }
	                    
	                    searchFuture = null;
	                    
	                    return null;
	                    
	                default:
	                    // Can't be...
	                    return null;
	            }
		    } else if ( searchResponse instanceof SyncInfoValue ){
		        // This is the end of the refresh phase, log the info
		        SyncInfoValue syncInfoValue = (SyncInfoValue)searchResponse;
		        LOGGER.info("Refresh phase done: {} in {} ms", syncInfoValue.isRefreshDone(),
		                System.currentTimeMillis() - refreshStart);
		        
		        // Now we are done with the refresh, we need to cleanup the destination
		        // by removing the deleted entries 
		        cleanupDestination();
		        
		        return null;
		    } else {
		        // Can't be
		        return null;
		    }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted search !");
            
            return null;
        }
	}
	
	
	/**
	 * Check entries from destination to see if they haven't been deleted from source
	 * 
	 */
	private void cleanupDestination() {
	    
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
    			SyncRequestValue syncControl = new SyncRequestValueImpl(true);
    			syncControl.setMode(SynchronizationModeEnum.REFRESH_AND_PERSIST);
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
    			searchControl.setChangeTypes(PersistentSearch.CHANGE_TYPES_MAX);
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
	
	private LscDatasets convertEntry(Entry entry, boolean onlyFirstValue) {
		if(entry == null) {
		    return null;
		}
		
		LscDatasets converted = new LscDatasets();
		Map<String, Object> dataSets = converted.getDatasets();
		
        if (onlyFirstValue) {
            for ( Attribute attribute:entry ) {
                if(attribute.size() > 0){
                    String id = attribute.getId();
                    Value value = attribute.get();
                    
                    if (value.isHumanReadable()) {
                        dataSets.put(id, value.getString());
                    } else {
                        dataSets.put(id, value.getBytes());
                    }
                }
            }
        } else {
            for ( Attribute attribute:entry ) {
                if(attribute.size() > 0){
                    String id = attribute.getId();
                    Set<Object> datasetsValues = new HashSet<Object>();
                    
                    for ( Value value:attribute) {
                        if (value.isHumanReadable()) {
                            datasetsValues.add(value.getString());
                        } else {
                            datasetsValues.add(value.getBytes());
                        }
                    }

                    dataSets.put(id, datasetsValues);
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
	private Map<String, LscDatasets> convertSearchEntries( EntryCursor entryCursor) throws LscServiceException {
		Map<String, LscDatasets> converted = new HashMap<String, LscDatasets>();
		
		try {
		    for (Entry entry:entryCursor) {
				converted.put(entry.getDn().getName(), convertEntry(entry));
			}
		    
		    entryCursor.close();
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
