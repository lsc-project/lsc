/*
 ****************************************************************************
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.lsc.LscDatasets;
import org.lsc.LscModifications;
import org.lsc.beans.IBean;
import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscServiceException;
import org.lsc.utils.output.LdifLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/**
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class XALdifDstService implements
		IXAWritableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(XALdifDstService.class);

	/** the service unique identifier */
	private String id;
	
    /** the XA object */
    private XAFileSystem xafs;
    
    private TransactionManager tm;
    
    /** The path where file are stored */
    private String outputDirectory;
    
    private Map<String, XASession> xaSessions;
	
	public XALdifDstService(TaskType task) throws LscServiceException {
		xaSessions = new HashMap<String, XASession>();
		this.id = LscConfiguration.getDestinationService(task).getName();
		this.outputDirectory = task.getXaFileDestinationService().getOutputDirectory();
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Botting an XADisk instance...");
        StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(outputDirectory, id);
        xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
        try {
			xafs.waitForBootup(-1);
		} catch (InterruptedException e) {
			throw new LscServiceException(e);
		}
        if(LOGGER.isDebugEnabled()) LOGGER.debug("Successfully booted the XADisk instance.\n");
        tm = new bitronix.tm.BitronixTransactionManager();
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String start() throws LscServiceException {

		if(xaSessions.containsKey("" + Thread.currentThread().getId())) {
			LOGGER.debug("XA transaction already started in this thread !");
			return null;
		}

		if(LOGGER.isDebugEnabled()) LOGGER.debug("Starting an XA transaction...");
        try {
			tm.begin();
	        Transaction tx1 = tm.getTransaction();
	        XASession xaSession = xafs.createSessionForXATransaction();
	        if(LOGGER.isDebugEnabled()) LOGGER.debug ("Enlisting XADisk in the XA transaction.");
	        XAResource xaResource = xaSession.getXAResource();
	        tx1.enlistResource(xaResource);
	        xaSessions.put("" + Thread.currentThread().getId(), xaSession);
		} catch (NotSupportedException e) {
			throw new LscServiceException(e);
		} catch (SystemException e) {
			throw new LscServiceException(e);
		} catch (IllegalStateException e) {
			throw new LscServiceException(e);
		} catch (RollbackException e) {
			throw new LscServiceException(e);
		}
		return null;
	}

	@Override
	public void submit(String xid, LscModifications lm)
			throws LscServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void commit(String xid) throws LscServiceException {
        try {
			tm.commit();
		} catch (SecurityException e) {
			throw new LscServiceException(e);
		} catch (IllegalStateException e) {
			throw new LscServiceException(e);
		} catch (RollbackException e) {
			throw new LscServiceException(e);
		} catch (HeuristicMixedException e) {
			throw new LscServiceException(e);
		} catch (HeuristicRollbackException e) {
			throw new LscServiceException(e);
		} catch (SystemException e) {
			throw new LscServiceException(e);
		}
	}

	@Override
	public void end(String xid) throws LscServiceException {
		// Do nothing
	}

	@Override
	public int prepare(String xid) throws LscServiceException {
		// Do nothing
		return XAResource.XA_OK;
	}

	
	
	@Override
	public void rollback(String xid) throws LscServiceException {
		try {
			tm.rollback();
		} catch (IllegalStateException e) {
			throw new LscServiceException(e);
		} catch (SecurityException e) {
			throw new LscServiceException(e);
		} catch (SystemException e) {
			throw new LscServiceException(e);
		}
	}

	@Override
	public boolean apply(LscModifications lm) throws LscServiceException {

		XASession xaSession = xaSessions.get("" + Thread.currentThread().getId());

		if(xaSession != null) {
	        if(LOGGER.isDebugEnabled()) LOGGER.debug("Performing transactional work over XADisk and other involved resources (e.g. Oracle, MQ)\n");
	        try {
	        	File ldifFile = new File(outputDirectory, lm.getMainIdentifier());
				xaSession.createFile(ldifFile, false);
				FileOutputStream fos = new FileOutputStream(ldifFile);
				fos.write(LdifLayout.format(lm).getBytes());
				fos.close();
			} catch (FileAlreadyExistsException e) {
				throw new LscServiceException(e);
			} catch (FileNotExistsException e) {
				throw new LscServiceException(e);
			} catch (InsufficientPermissionOnFileException e) {
				throw new LscServiceException(e);
			} catch (LockingFailedException e) {
				throw new LscServiceException(e);
			} catch (NoTransactionAssociatedException e) {
				throw new LscServiceException(e);
			} catch (InterruptedException e) {
				throw new LscServiceException(e);
			} catch (FileNotFoundException e) {
				throw new LscServiceException(e);
			} catch (IOException e) {
				throw new LscServiceException(e);
			}
		}
        return false;
	}

	@Override
	public IBean getBean(String pivotName, LscDatasets pivotAttributes,
			boolean fromSameService) throws LscServiceException {
		return null;
	}

	@Override
	public Map<String, LscDatasets> getListPivots() throws LscServiceException {
		return null;
	}

	@Override
	public void setTransactionManager(TransactionManager xaTM) {
		this.tm = xaTM;
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
