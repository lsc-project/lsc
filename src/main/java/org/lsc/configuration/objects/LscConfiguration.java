/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration.objects;

import java.util.List;

/**
 *
 * @author rschermesser
 */
public class LscConfiguration {
	List<Connection> connections;
	List<Audit> audits;
	List<Service> services;
	List<Task> tasks;

	public List<Audit> getAudits() {
		return audits;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public List<Service> getServices() {
		return services;
	}

	public List<Task> getTasks() {
		return tasks;
	}
}
