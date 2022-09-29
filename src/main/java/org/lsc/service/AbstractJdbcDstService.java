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
package org.lsc.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsc.LscModifications;
import org.lsc.exception.LscServiceConfigurationException;
import org.lsc.exception.LscServiceException;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;


/**
 * This class is a Database abstraction layour for a destination service
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public abstract class AbstractJdbcDstService extends AbstractJdbcService implements IWritableService{

    private String serviceName;
    
    public AbstractJdbcDstService(String serviceName, SqlMapClient sqlMapClient, String bean) throws LscServiceConfigurationException {
        super(sqlMapClient, bean);
        this.serviceName = serviceName;
    }

    @Override
    public boolean apply(LscModifications lm) throws LscServiceException {
        Map<String, Object> attributeMap = getAttributesMap(lm.getLscAttributeModifications());
        try {
            sqlMapper.startTransaction();
            switch(lm.getOperation()) {
            case CHANGE_ID:
                // Silently return without doing anything
                break;
            case CREATE_OBJECT:
                for(String request: getRequestsNameForInsert()) {
              	    LOGGER.debug("Executing " + request + "(" + attributeMap + ")");
                    sqlMapper.insert(request, attributeMap);
                }
                break;
            case DELETE_OBJECT:
                for(String request: getRequestsNameForDelete()) {
                	  LOGGER.debug("Executing " + request + "(" + attributeMap + ")");
                    sqlMapper.delete(request, attributeMap);
                }
                break;
            case UPDATE_OBJECT:
                // Push the destination value
                attributeMap = fillAttributesMap(attributeMap, lm.getDestinationBean());
                for(String request: getRequestsNameForUpdate()) {
              	    LOGGER.debug("Executing " + request + "(" + attributeMap + ")");
                    sqlMapper.update(request, attributeMap);
                }
            }
            sqlMapper.commitTransaction();
        } catch (SQLException e) {
            LOGGER.error(e.toString(), e);
            LOGGER.error("Error caused by operation " + lm.getOperation().getDescription() + ", attributes " + attributeMap.toString());
            return false;
        } finally {
            try {
                sqlMapper.endTransaction();
            } catch (SQLException e) {
                LOGGER.error(e.toString(), e);
                return false;
            }
        }
        return true;
    }
    
    /** Fetched attributes name cache */
    private static Map<String, List<String>> attributesNameCache = new HashMap<String, List<String>>();

    @Override
    public List<String> getWriteDatasetIds() {
        List<String> writeDatasetIds = attributesNameCache.get(serviceName);
        if(writeDatasetIds != null) {
            return writeDatasetIds;
        }
        return buildWriteDatasetIds();
    }

	private List<String> buildWriteDatasetIds() {
        List<String> writeDatasetIds = new ArrayList<String>();
        if(sqlMapper instanceof SqlMapClientImpl) {
            for(String request: getRequestsNameForInsert()) {
                for(ParameterMapping pm : ((SqlMapClientImpl)sqlMapper).getDelegate().getMappedStatement(request).getParameterMap().getParameterMappings()) {
                    writeDatasetIds.add(pm.getPropertyName());
                }
            }
            attributesNameCache.put(serviceName, writeDatasetIds);
        } else {
            LOGGER.error("Unable to handle an unknown SQLMap Client type : " + sqlMapper.getClass().getName());
        }
        return writeDatasetIds;
	}

    public abstract List<String> getRequestsNameForInsert();

    public abstract List<String> getRequestsNameForUpdate();
    
    public abstract List<String> getRequestsNameForDelete();

}
