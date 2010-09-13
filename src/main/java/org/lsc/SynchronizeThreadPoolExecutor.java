/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2010, LSC Project 
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
 *               (c) 2008 - 2010 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object is managing LSC tasks. It is now a wrapper for JDK 
 * ThreadPoolExecutor but may rely on a different implementation  
 * @author Sebastien Bahloul <seb@lsc-project.org>
 *
 */
public class SynchronizeThreadPoolExecutor extends ThreadPoolExecutor {

	private static final long KEEP_ALIVE_TIME = 60;

	private BlockingQueue<Runnable> queue;

	/** Default logger */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SynchronizeThreadPoolExecutor.class);

	protected SynchronizeThreadPoolExecutor(int threads) {
		super(threads, threads, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		queue = getQueue(); 
	}

	/**
	 * Run a task
	 * In the threadpoolexecutor implementation, store it in the queue and let
	 * the pool consume it as soon as it can, in a FIFO way without any priority
	 * @param task the runnable object
	 */
	protected void runTask(SynchronizeTask task) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Task count.." + getTaskCount());
			LOGGER.debug("Queue Size before assigning the task.."
					+ queue.size());
		}
		execute(task);
		this.beforeExecute(new Thread(task.getSyncName() + "-" + task.getId().getKey()), task);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Queue Size after assigning the task: {}", queue.size());
			LOGGER.debug("Pool Size after assigning the task: {}", getActiveCount());
			LOGGER.debug("Task count: {}", getTaskCount());
		}

	}
	
	/**
	 * This method just rename the thread with the taskname as prefix 
	 * suffixed by a threads counter
	 * @param t the thread
	 * @param r the runnable task
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if(r instanceof SynchronizeTask) {
			SynchronizeTask task = (SynchronizeTask) r;
			t.setName(task.getSyncName() + "-" + t.getId());
		}
	}
}
