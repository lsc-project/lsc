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
package org.lsc.connectors.xmlrpc;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attribute;

import org.apache.xmlrpc.XmlRpcException;
import org.lsc.LscDatasetModification;
import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IWritableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample customer class to demonstrate how to call xmlrpc methods
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class XmlRpcUserProvisioning extends AbstractLscXmlRpcClient implements IWritableService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlRpcUserProvisioning.class);

	private String objectType = "Users"; 
	private String domain = "lsc-project.org";
	private String idToSync;
	
	public XmlRpcUserProvisioning() {
		super();
		options.addOption("i", "id", true, "Specify an identifier to synchronize");
		options.addOption("o", "object", true, "Specify the object name type on XmlRpc");
	}
	
	public static void main(String[] args) throws MalformedURLException, XmlRpcException {
		XmlRpcUserProvisioning oupsUser = new XmlRpcUserProvisioning();
		int retCode = oupsUser.parseOptions(args);
		if(retCode != 0) {
			System.exit(retCode);
		}
		oupsUser.run();
	}
	
	public void run() throws XmlRpcException {
		bind();
		if(!ping()) {
			LOGGER.error("LSC XmlRpc : Failed to ping service !");
			System.exit(1);
		}
		List<String> methodNames = info();
		if(!methodNames.contains("list")
				|| !methodNames.contains("read")
				|| !methodNames.contains("create")
				|| !methodNames.contains("update")
				|| !methodNames.contains("delete") ){
			LOGGER.error("LSC XmlRpc : Service does not provide required methods !");
			System.exit(1);
		}
		if(idToSync != null) {
			LOGGER.info("LSC XmlRpc : Force sync of id=" + idToSync);
			get(idToSync);			
		} else {
			LOGGER.info("LSC XmlRpc : Listing ids ...");
			for(String id : listIds()) {
				LOGGER.info("LSC XmlRpc : Getting data for user id=" + id);
				get(id);
			}
		}
	}

	public boolean ping() {
		try {
			return decodeStringListResult((Object[])client.execute(objectType + ".ping", encodeRequest())).size() == 0;
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<String> listIds() throws XmlRpcException {
		return decodeStringListResult((Object[])client.execute(objectType + ".list", encodeRequest(domain)));
	}

	public List<String> get(String id) throws XmlRpcException {
		return decodeStringListResult((Object[])client.execute(objectType + ".read", encodeRequest(id)));
	}

	public List<String> create(String id, List<Attribute> attributes) throws XmlRpcException {
		return decodeStringListResult((Object[])client.execute(objectType + ".create", encodeRequest(id, encodeMap(attributes))));
	}

	public List<String> update(String id, List<Attribute> attributes) throws XmlRpcException {
		return decodeStringListResult((Object[])client.execute(objectType + ".update", encodeRequest(id, encodeMap(attributes))));
	}

	public List<String> delete(String id) throws XmlRpcException {
		return decodeStringListResult((Object[])client.execute(objectType + ".delete", encodeRequest(id)));
	}

	/**
	 * Return available methods list
	 * @param id
	 * @return
	 * @throws XmlRpcException
	 */
	public List<String> info() throws XmlRpcException {
		List<Object> result = decodeObjectListResult((Object[])client.execute(objectType + ".info", encodeRequest()));
		
		LOGGER.info("LSC XmlRpc Server version: " + result.get(0));
		LOGGER.info("LSC XmlRpc Protocol version : " + result.get(1));
		
		List<String> methodNames = new ArrayList<String>(); 
		for(Object value : (Object[])result.get(2)) {
			methodNames.add((String) value);
		}
		return methodNames;
	}

	protected int parseOptions(final String[] args) throws MalformedURLException {
		int retCode = super.parseOptions(args);
		if(retCode != 0) {
			return retCode;
		}
		if ( cmdLine.hasOption("i") ) {
			idToSync = cmdLine.getOptionValue("i");
		}
		if ( cmdLine.hasOption("o") ) {
			objectType = cmdLine.getOptionValue("o");
		} else {
			printHelp(options);
			return 3;
		}
		return 0;
	}

	public boolean apply(LscModifications lm) throws LscServiceException {
		try {
			switch(lm.getOperation()) {
				case CREATE_OBJECT:
					this.create(lm.getMainIdentifier(), attributeModificationsToAttributes(lm.getLscAttributeModifications()));
					break;
				case UPDATE_OBJECT:
					this.update(lm.getMainIdentifier(), attributeModificationsToAttributes(lm.getLscAttributeModifications()));
					break;
				case DELETE_OBJECT:
					this.delete(lm.getMainIdentifier());
					break;
				case CHANGE_ID:
					throw new UnsupportedOperationException();
			}
		} catch(XmlRpcException e) {
			throw new LscServiceException(e);
		}
		return false;
	}

	private List<Attribute> attributeModificationsToAttributes(
			List<LscDatasetModification> lscAttributeModifications) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBean getBean(String pivotName, LscDatasets pivotAttributes, boolean fromSameService)
			throws LscServiceException {
		try {
			get(pivotName);
		} catch (XmlRpcException e) {
			throw new LscServiceException(e);
		}
		return null;
	}

	@Override
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		try {
			Map<String, LscDatasets> ids = new HashMap<String, LscDatasets>();
			for(String id : listIds()) {
				ids.put(id, new LscDatasets());
			}
			return ids;
		} catch(XmlRpcException e) {
			throw new LscServiceException(e);
		}
		
	}

	@Override
	public List<String> getWriteDatasetIds() {
		throw new UnsupportedOperationException("TODO");
	}

    /**
     * @see org.lsc.service.IService.getSupportedConnectionType()
     */
    public Collection<Class<? extends ConnectionType>> getSupportedConnectionType() {
        Collection<Class<? extends ConnectionType>> list = new ArrayList<Class<? extends ConnectionType>>();
        return list;
    }
}
