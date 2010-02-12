/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration.objects.audit;

import org.lsc.configuration.objects.Audit;

/**
 *
 * @author rschermesser
 */
public class Csv extends Audit {

	/**
	 * 			<operations>create, delete</operations>
			<attributes>cn, dn</attributes>
			<separator>;</separator>
			<append>true</append>

	 */
	 private String operations;
	 private String attributes;
	 private String separator;
	 private Boolean append;

}
