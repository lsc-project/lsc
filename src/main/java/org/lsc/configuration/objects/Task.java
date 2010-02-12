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
public class Task {

	/**
	 * 			<name>myTask</name>
			<source>myADAccount</source>
			<destination>myDestination</destination>
			<auditLogs>
				<audit>csv</audit>
				<audit>ldif</audit>
			</auditLogs>
			<conditions>
				<create>1 > 0</create>
				<update>src.getAttr('updateTimeStamp') > dst.getAttr('updateTimeStamp')</update>
				<delete>false</delete>
				<modrdn>false</modrdn>
			</conditions>
			<syncoptions class="XmlBasedSyncOptions">
				<attributes>
					<name>cn</name>
					<createValue>toto</createValue>
					<defaultValue>toto</defaultValue>
					<forceValue>toto</forceValue>
					<policy>(force|keep|merge)</policy>
				</attributes>
			</syncoptions>

	 */
	
	private String name;
	private Service source;
	private Service destination;
	private List<Audit> auditLogs;
	private Conditions conditions;
	private SyncOptions syncOptions;

}
