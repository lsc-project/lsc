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
package org.lsc.beans.syncoptions;

import java.util.HashMap;
import java.util.Map;

import org.lsc.configuration.LscConfiguration;
import org.lsc.configuration.TaskType;
import org.lsc.exception.LscConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SyncOptionsFactory {

	private static SyncOptionsFactory INSTANCE = new SyncOptionsFactory();
	private Map<String, ISyncOptions> cache;
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SyncOptionsFactory.class);

	private SyncOptionsFactory() {
		cache = new HashMap<String, ISyncOptions>();
	}

	private void convertFromTask(TaskType task) throws LscConfigurationException {
		try {
			ISyncOptions iso = (ISyncOptions) LscConfiguration.getSyncOptionsImplementation(LscConfiguration.getSyncOptions(task)).newInstance();
			iso.initialize(task);
			cache.put(task.getName(), iso);
		} catch (InstantiationException e) {
			LOGGER.error(
					"Internal error while instanciating '{}' name. Choose another implementation or fix it !",
					LscConfiguration.getSyncOptionsImplementation(LscConfiguration.getSyncOptions(task)).getClass().getName());
		} catch (IllegalAccessException e) {
			LOGGER.error(
					"Internal error while instanciating '{}' name. Choose another implementation or fix it !",
					LscConfiguration.getSyncOptionsImplementation(LscConfiguration.getSyncOptions(task)).getClass().getName());
		}
	}

	public static ISyncOptions convert(TaskType task) throws LscConfigurationException {
		return INSTANCE.get(task);
	}

	private ISyncOptions get(TaskType task) throws LscConfigurationException {
		if (!cache.containsKey(task.getName())) {
			convertFromTask(task);
		}
		return cache.get(task.getName());
	}
}
