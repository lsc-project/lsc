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
package org.lsc.utils.annotations;

import org.lsc.configuration.ConnectionType;
import org.lsc.configuration.LdapAuthenticationType;
import org.lsc.configuration.LdapConnectionType;
import org.lsc.configuration.LdapDerefAliasesType;
import org.lsc.configuration.LdapReferralType;
import org.lsc.configuration.LdapVersionType;
import org.lsc.configuration.SaslQopType;

/**
 * An annotation for connections. As we can't create a inheritance scheme for
 * annotations, this annotation will contain everything needed to create the three
 * flavors of connection:
 * <ul>
 *   <li>Common elements:
 *     <ul>
 *       <li>id: The connection's ID</li>
 *       <li>name: The Connection name
 *       <li>url: The connection url
 *       <li>username: The connection credentials' name
 *       <li>password: The connection credentials' password
 *     </ul>
 *   </li>
 *   <li>
 *     LdapConnection:
 *     <ul>
 *       <li>authentication: The authentication type, one of NONE, SIMPLE, SASL, DIGEST-MD5 or GSSAPI</li>
 *       <li>referral: The referral handling, one of IGNORE, ERROR, FOLLOW or THROW</li>
 *       <li>derefAliases: The alias handling, one of NEVER, SEARCH, FIND or ALWAYS</li>
 *       <li>version: The LDAP version, one of Version_3 or VERSION_2</li>
 *       <li>pageSize: The page size when using a Paged Search</li>
 *       <li>factory: The Java class used to create a LDAP context </li>
 *       <li>tlsActivated: Tell if the connection is secured or not</li>
 *       <li>saslMutualAuthentication: Tell if we have a mutual SASL authentication </li>
 *       <li>sortedBy: The attributes used to sort the result on the server</li>
 *       <li>binaryAttributes>: The list of binary attributes</li>
 *       <li>recursiveDelete: Tell if the entries are recursively deleted</li>
 *       <li>relaxRules: Tell if we allow modifications of Operation Attributes</li>
 *       <li>saslQop: The Quality Of Protection, one of AUTH, AUTH_CONF or AUTH_INT</li>
 *     </ul>
 *   </li>
 *   <li>
 *     DatabaseConnection:
 *     <ul>
 *       <li>driver: The Java driver class to use</li>
 *     </ul>
 *   </li>
 *   <li>
 *     pluginConnection:
 *     <ul>
 *       <li>configurationClass: The Java class that implements the configuration</li>
 *       <li>implementationClass: The Java class that implements the plugin</li>
 *       <li>(there is an extension mechanism that needs to be implemented)</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public @interface CreateConnection {
    // From connectionType
    /** @return The Connection ID */
    String id() default "";

    /** @return The connection type, one of LdapConnection, DatabaseConnection or PluginConnection */
    Class<? extends ConnectionType> type() default LdapConnectionType.class;

    // The common parameters whatever the type of connection
    /** @return the connection name */
    String name();

    /** @return the connection URL */
    String url();

    /** @return the user name */
    String username();

    /** @return the user password */
    String password();

    // LDAP Connection specific parameters
    /** @return The authentication type */
    LdapAuthenticationType authenticationType() default LdapAuthenticationType.SIMPLE;

    LdapReferralType referral() default LdapReferralType.IGNORE;

    LdapDerefAliasesType derefAliases() default LdapDerefAliasesType.NEVER;

    LdapVersionType version() default LdapVersionType.VERSION_3;

    int pageSize() default -1;

    String factory() default "com.sun.jndi.ldap.LdapCtxFactory";

    boolean tlsActivated() default false;

    boolean saslMutualAuthentication() default false;

    /**
     * Gives the attribute used to sort the result.
     *
     * @return The attribute used to sort the result
     */
    String sortedBy() default "";

    CreateValuesType[] binaryAttributes() default {};

    boolean recursiveDelete() default false;

    boolean relaxRules() default false;

    SaslQopType saslQop() default SaslQopType.AUTH;

    // Database Connection specific parameter
    String driver() default "";

    // Plugin Connection specific parameters
    String configurationClass() default "";

    String implementationClass() default "";
}
