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
package org.lsc.jndi.parser;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.*;

import org.slf4j.LoggerFactory;

/**
 * Test an ldap object class representation
 * Successfully tested with OpenLDAP 2.3
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class LdapObjectClassTest  {

	@Test
	public final void testParsing() {
		LoggerFactory.getLogger(this.getClass()).info("A warn message about 'Multiple inheritence not supported' is normal.");
		assertNotNull(LdapObjectClass.parse(
						"( 1.3.6.1.4.1.7135.1.3.201.1.4.1.14 NAME 'objectclassToTest' DESC 'ObjectClass to test' SUP ( firstAttribute $ secondAttribute $ thirdAttribute $ organizationalUnit ) STRUCTURAL MUST ( description $ domaineMessagerie ) MAY ( adressePrincipaleService $ facsimileTelephoneNumber $ labeledURI $ mail $ mailLocalAddress $ mailRoutingAddress $ telephoneNumber ) )",
						new HashMap<String, LdapAttributeType>()));
		assertNotNull(LdapObjectClass.parse(
						"( 1.3.6.1.4.1.3317.4.3.2.1 NAME 'radiusprofile' DESC '' SUP top AUXILIARY MUST cn MAY ( radiusArapFeatures $ radiusArapSecurity $ radiusArapZoneAccess $ radiusAuthType $ radiusCallbackId $ radiusCallbackNumber $ radiusCalledStationId $ radiusCallingStationId $ radiusClass $ radiusClientIPAddress $ radiusFilterId $ radiusFramedAppleTalkLink $ radiusFramedAppleTalkNetwork $ radiusFramedAppleTalkZone $ radiusFramedCompression $ radiusFramedIPAddress $ radiusFramedIPNetmask $ radiusFramedIPXNetwork $ radiusFramedMTU $ radiusFramedProtocol $ radiusCheckItem $ radiusReplyItem $ radiusFramedRoute $ radiusFramedRouting $ radiusIdleTimeout $ radiusGroupName $ radiusHint $ radiusHuntgroupName $ radiusLoginIPHost $ radiusLoginLATGroup $ radiusLoginLATNode $ radiusLoginLATPort $ radiusLoginLATService $ radiusLoginService $ radiusLoginTCPPort $ radiusLoginTime $ radiusPasswordRetry $ radiusPortLimit $ radiusPrompt $ radiusProxyToRealm $ radiusRealm $ radiusReplicateToRealm $ radiusServiceType $ radiusSessionTimeout $ radiusStripUserName $ radiusTerminationAction $ radiusTunnelClientEndpoint $ radiusProfileDn $ radiusSimultaneousUse $ radiusTunnelAssignmentId $ radiusTunnelMediumType $ radiusTunnelPassword $ radiusTunnelPreference $ radiusTunnelPrivateGroupId $ radiusTunnelServerEndpoint $ radiusTunnelType $ radiusUserCategory $ radiusVSA $ radiusExpiration $ dialupAccess $ radiusNASIpAddress $ radiusReplyMessage ) )",
						new HashMap<String, LdapAttributeType>()));
	}
}
