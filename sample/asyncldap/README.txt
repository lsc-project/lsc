Asynchronous LDAP notifications 
-------------------------------

This LSC source service enables event-based updates handling upon supported directories based on various extended controls
(LDAP Persistent search control, LDAP Sync - RFC 4533, ...)

It supports :
- through LDAP Persistent Search Control : Netscape Directory Server, Sun Directory Server (native and Java edition), Oracle Directory 
   server, OpenDS and OpenDJ
- through LDAP Sync Control: OpenLDAP and Apache DS (to be fully checked)
- through Microsoft proprietary control (1.2.840.113556.1.4.528): Active Directory

To use this connector, setup a task with standard source connection and service. You just need to change the service 
from ldapSourceService to "org.lsc.service.SyncReplServiceConfiguration" and a node called "serverType" with
the corresponding value (OpenLDAP for example). 

      <asyncLdapSourceService>
        <name>openldap-source-service</name>
        <.../>
        <serverType>OpenLDAP</serverType>
      </asyncLdapSourceService>

When this is done, try your new service through an asynchronous task launch :
$ $LSC_HOME/bin/lsc -f $LSC_HOME/etc -a all -t 1

or 

> %LSC_HOME%\bin\lsc.bat -f %LSC_HOME%\etc -a all -t 1
