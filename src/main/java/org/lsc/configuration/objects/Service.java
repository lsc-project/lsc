/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration.objects;

/**
 *
 * @author rschermesser
 */
//public abstract class Service {
public class Service {

	/**
	 * <service type="SimpleJndiSrcService">
			<name>myADAccount</name>
			<connection>myAdServer</connection>
			<baseDn>ou=people</baseDn>
			<pivotAttributes>dn, cn</pivotAttributes>
			<fetchAttributes>uid, mail, cn</fetchAttributes>
			<dn>'cn=' + srcBean.getAttributeValueById('uid')</dn>
			<getAllFilter>(&(objectClass=inetOrgPerson)(uid=*))</getAllFilter>
			<getOneFilter>(&(objectClass=inetOrgPerson)(uid={uid}))</getOneFilter>
		</service>
		<service type="SimpleJndiDstService">
			<name>myDestination</name>
			<connection>myAdServer</connection>
		</service>
	 */
	
	private String name;
	private Connection connection;

}
