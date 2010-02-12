/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lsc.configuration.objects;

import com.unboundid.ldap.sdk.LDAPURL;
import java.net.URI;

/**
 *
 * @author rschermesser
 */
//public abstract class Connection {
public class Connection {

	/**
	 * <connection type="ldap">
			<name>myAdServer</name>
			<url>ldap://localhost:1390/dc=AD,dc=net</url>
			<username>cn=manager,dc=AD,dc=net</username>
			<password>secret</password>
			<referral>ignore</referral>
			<derefAliases>never</derefAliases>
			<factory>com.sun.jndi.ldap.LdapCtxFactory</factory>
			<version>3</version>
			<authentification>simple</authentification>
			<pageSize>10</pageSize>
			<tls>true</tls>
		</connection>
		<connection type="database">
			<name>myHSQLDBServer</name>
			<url>jdbc:hsqldb:file:hsqldb/lsc</url>
			<username>elilly</username>
			<password></password>
			<driver>org.hsqldb.blabla</driver>
		</connection>
	 */
	
	private String name;
	private LDAPURL url;
	private String username;
	private String password;
}
