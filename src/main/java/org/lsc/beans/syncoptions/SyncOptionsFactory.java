/*
 ****************************************************************************
 * Ldap Synchronization Connector provides tools to synchronize
 * electronic identities from a list of data sources including
 * any database with a JDBC connector, another LDAP directory,
 * flat files...
 *
 *                  ==LICENSE NOTICE==
 * 
 * Copyright (c) 2008, LSC Project 
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
 *               (c) 2008 - 2009 LSC Project
 *         Sebastien Bahloul <seb@lsc-project.org>
 *         Thomas Chemineau <thomas@lsc-project.org>
 *         Jonathan Clarke <jon@lsc-project.org>
 *         Remy-Christophe Schermesser <rcs@lsc-project.org>
 ****************************************************************************
 */
package org.lsc.beans.syncoptions;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lsc.Configuration;

public final class SyncOptionsFactory {

	private static SyncOptionsFactory INSTANCE = new SyncOptionsFactory();
	private Map<String, ISyncOptions> cache;
	private static final Logger LOGGER = LoggerFactory.getLogger(SyncOptionsFactory.class);

	private SyncOptionsFactory() {
		cache = new HashMap<String, ISyncOptions>();
		this.loadOptions();
	}

	private void loadOptions() {
		String tasks = Configuration.getString("lsc.tasks", "default");
		StringTokenizer stok = new StringTokenizer(tasks, ",");
		while (stok.hasMoreTokens()) {
			String taskname = stok.nextToken();
			String className = Configuration.getString("lsc.syncoptions." + taskname, "org.lsc.beans.syncoptions.ForceSyncOptions");
			try {
				Class<?> cSyncOptions = Class.forName(className);
				ISyncOptions iso = (ISyncOptions) cSyncOptions.newInstance();
				iso.initialize(taskname);
				cache.put(taskname, iso);
			} catch (ClassNotFoundException e) {
				LOGGER.error("Unable to found '{}' name. Please respecify lsc.syncoptions.{} value.",
								className, taskname);
			} catch (InstantiationException e) {
				LOGGER.error("Internal error while instanciating '{}' name. Choose another implementation or fix it !", className);
			} catch (IllegalAccessException e) {
				LOGGER.error("Internal error while instanciating '{}' name. Choose another implementation or fix it !", className);
			}
		}
	}

	public static ISyncOptions getInstance(String syncName) {
		return INSTANCE.get(syncName);
	}

	private ISyncOptions get(String syncName) {
		return cache.get(syncName);
	}
}
