/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration.objects;

/**
 *
 * @author rschermesser
 */
//public abstract class Audit {
public class Audit {

	/**
	 * 		<audit type="csv">
			<name>csv</name>
			<operations>create, delete</operations>
			<attributes>cn, dn</attributes>
			<separator>;</separator>
			<append>true</append>
			<file>/tmp/log.csv</file>
		</audit>
		<audit type="ldif">
			<name>ldif</name>
			<operations>create, delete</operations>
			<append>false</append>
			<file>/tmp/log.ldif</file>
		</audit>
	 */
	
	private String name;
	private String file;
	

}
