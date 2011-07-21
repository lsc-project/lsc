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
package org.lsc.connectors.jdbc;

import java.sql.SQLException;
import java.util.Properties;

import org.lsc.IWritableService;
import org.lsc.LscModifications;
import org.lsc.configuration.objects.Task;
import org.lsc.exception.LscServiceException;
import org.lsc.service.SimpleJdbcSrcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a JDBC compliant service
 * 
 * At this time, no time out is managed. So please consider handling provisioned
 * referential availibility and/or time limit handling directly in the executable.
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class JdbcService extends SimpleJdbcSrcService implements IWritableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcService.class);

	private final String requestNameForAdd;
	private final String requestNameForUpdate;
	private final String requestNameForRename;
	private final String requestNameForDelete;
	
	@Deprecated
	public JdbcService(Properties props, String beanClassName) throws LscServiceException {
		super(props, beanClassName);
		requestNameForAdd    = (String) props.get("requestNameForAdd");
		requestNameForUpdate = (String) props.get("requestNameForUpdate");
		requestNameForRename = (String) props.get("requestNameForRename");
		requestNameForDelete = (String) props.get("requestNameForDelete");
		throw new UnsupportedOperationException();
	}

	public JdbcService(Task task) throws LscServiceException {
		super(task);
		throw new UnsupportedOperationException();
	}

	public boolean apply(LscModifications lm) throws LscServiceException {
		try {
			sqlMapper.startTransaction();
			switch(lm.getOperation()) {
				case CREATE_OBJECT:
					sqlMapper.insert(requestNameForAdd);
					break;
				case UPDATE_OBJECT:
					sqlMapper.update(requestNameForUpdate);
					break;
				case CHANGE_ID:
					sqlMapper.update(requestNameForRename);
					break;
				case DELETE_OBJECT:
					sqlMapper.delete(requestNameForDelete);
					break;
				default:
			}
			sqlMapper.commitTransaction();
			return true;
		} catch (SQLException e) {
			LOGGER.error(e.toString(), e);
//			throw new LscServiceException(e);
		}
		return false;
	}
}
