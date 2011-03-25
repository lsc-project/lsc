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
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.lsc.Configuration;
import org.lsc.LscAttributes;
import org.lsc.beans.IBean;
import org.lsc.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a NIS source service implementation
 * 
 * You only have to specify the following parameters :
 * &lt;ul&gt;
 *  &lt;li&gt;the server name (servername)
 *  &lt;li&gt;the domain name (domain)
 *  &lt;li&gt;the map name (map, map.bymethod)
 * &lt;ul&gt;
 * 
 * NIS requests are exclusively done on .byname ordering method
 * 
 * With the passwd map, the following attributes are exposed :
 * &lt;pre&gt; 
 * uid - test
 * => gidnumber - 1000
 * => nismapentry - test:x:1000:1000:Test account,,,:/home/test:/bin/bash
 * => userpassword - A BYTE ARRAY ENCODED STRING
 * => cn - test
 * => nismapname - passwd.byname
 * => loginshell - /bin/bash
 * => gecos - Test account,,,
 * => homedirectory - /home/test
 * => objectclass - nisObject
 *  - posixAccount
 *  - top
 * => uidnumber - 1000
 * &lt;/pre&gt;
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class NisSrcService implements IService {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(NisSrcService.class);
	/**
	 * Preceding the object feeding, it will be instantiated from this class.
	 */
	private Class<IBean> beanClass;

	/** the nis server name (hostname or ip address) */
	private String url;
	
	private String map;
	
	private InitialContext context;
	
	private Hashtable<String, String> conftable;
	
	private Map<String, Attributes> _cache;
 
	/**
	 * @param serviceProps
	 * @param beanClassName
	 * @throws LscServiceConfigurationException
	 */
	public NisSrcService(final Properties serviceProps, final String beanClassName) {

		try {
			this.beanClass = (Class<IBean>) Class.forName(beanClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		_cache = new HashMap<String, Attributes>();
		
		map = serviceProps.getProperty("map");

		String domain = serviceProps.getProperty("domain");
		String servername = serviceProps.getProperty("servername");
		
		// check that we have all parameters, or abort
		Configuration.assertPropertyNotEmpty("map", map, this.getClass().getName());
		Configuration.assertPropertyNotEmpty("domain", domain, this.getClass().getName());

		url = null;
		if(servername != null) {
			url = "nis://" + servername + "/" + domain;
		} else {
			url = "nis:///" + domain;
		}

		
		conftable = new Hashtable<String, String>();
		conftable.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.nis.NISCtxFactory");
		conftable.put(Context.PROVIDER_URL, url);
		conftable.put(Context.SECURITY_AUTHENTICATION, "simple");
	}

	public IBean getBean(String pivotName, LscAttributes pivotAttributes) 
		throws NamingException {
		IBean srcBean;
		if(_cache.isEmpty()) {
			updateCache();
		}
		try {
			srcBean = this.beanClass.newInstance();
			NamingEnumeration<String> idsEnum = _cache.get(pivotName).getIDs();
			while(idsEnum.hasMore()) {
				srcBean.setAttribute(_cache.get(pivotName).get(idsEnum.next()));
			}
			return srcBean;
		} catch (InstantiationException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error("Bad class name: " + beanClass.getName() + "(" + e + ")");
			LOGGER.debug(e.toString(), e);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Map<String, LscAttributes> getListPivots() throws NamingException {
		Map<String, LscAttributes> ret = new HashMap<String, LscAttributes>();
		if(_cache.isEmpty()) {
			updateCache();
		}
		for( String name: _cache.keySet() ) {
			Map<String, Object> attrsMap = new HashMap<String, Object>();
			NamingEnumeration<? extends Attribute> attrs = _cache.get(name).getAll();
			while(attrs.hasMore()) {
				Attribute attr = attrs.next();
				attrsMap.put(attr.getID(), attr.get());
			}
			ret.put(name, new LscAttributes());
		}
		return ret;
	}

	private synchronized void updateCache() throws NamingException {
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Connecting to the NIS domain ...");
		context = new InitialDirContext(conftable);
		
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Retrieving the information ...");
		DirContext maps = (DirContext) context.lookup("system/" + map);
		NamingEnumeration<SearchResult> results = maps.search("", "objectClass=*", new SearchControls());
		while(results.hasMore()) {
			SearchResult result = results.next();
			_cache.put(result.getName(), result.getAttributes());
		}
		// we've got all needed information, close the context
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Closing context ...");
		context.close();
	}
}
