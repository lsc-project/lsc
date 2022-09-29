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
 *         Jonathan Clarke &lt;jon@lsc-project.org&gt;
 *         Remy-Christophe Schermesser &lt;rcs@lsc-project.org&gt;
 ****************************************************************************
 */
package org.lsc.service;

import java.util.Hashtable;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.lsc.LscModifications;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.exception.LscServiceInitializationException;

/**
 * Relies upon the Bitronix JTA transaction manager
 * Supported databases list is available there: http://docs.codehaus.org/display/BTM/JdbcXaSupportEvaluation
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class XAJdbcDstService extends SimpleJdbcDstService implements
		IXAWritableService {

	static {
		Hashtable<String, String> btmEnvs = new Hashtable<String, String>();
		btmEnvs.put("bitronix.tm.jndi.userTransactionName", "btmTransactionManager");
		btmEnvs.put("java.naming.factory.initial", "bitronix.tm.jndi.BitronixInitialContextFactory");
//		try {
//			Context ctx = new InitialContext(btmEnvs);
//			ctx.bind(name, obj)
//		} catch (NamingException ne) {
//			
//		}
	}
	
	private String id;
	
	public XAJdbcDstService(TaskType task) throws LscServiceException {
		super(task);

		this.id = LscConfiguration.getDestinationService(task).getName();
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String start() throws LscServiceInitializationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void submit(String xid, LscModifications lm)
			throws LscServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void commit(String xid) throws LscServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(String xid) throws LscServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public int prepare(String xid) throws LscServiceException {
		return XAResource.XA_OK;
	}

	@Override
	public void rollback(String xid) throws LscServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTransactionManager(TransactionManager xaTM) {
		// TODO Auto-generated method stub
		
	}
}
