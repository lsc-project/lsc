Active Directory synchronizations samples 
-----------------------------------------

These LSC samples demonstrate the capabilities to read accounts, groups and hosts from an Active Directory 
and to synchronize them to an NIS/Posix schema inside the OpenDJ instance. 

It is tested on a Active Directory Domain Service 2008r2 and is know to work with previous versions (2000 and 2003).

To use these samples, please consider configuring the following parameters according to your infrastructure:

      <asyncLdapSourceService>
        <name>openldap-source-service</name>
        <.../>
        <serverType>ActiveDirectory</serverType>
      </asyncLdapSourceService>

When this is done, try your new service through an asynchronous task launch :
$ $LSC_HOME/bin/lsc -f $LSC_HOME/sample/ad/etc-ad2opendj etc -a all -t 1

or 

> %LSC_HOME%\bin\lsc.bat -f %LSC_HOME%\sample\ad\etc-ad2opendj -a all -t 1

If you don't give any details about the host and URL, LSC will locate a domain controller through a DNS request 
(_ldap._tcp.foo.bar for the naming context DC=FOO,DC=BAR) and will try to connect on the 389 port. 
This cannot be used if * you want to establish a secure connection which requires to know the exact host to
set the corresponding certificate (this may be achieved if you use the Microsoft PKI by trusting the
certificate authority instead of the server certificate)

But resolving directory services through this method enables using the hot standby and the load balancing 
built-in mechanisms included in the Domain services architecture.

Regarding the authentication methods, only the simple BindDN / password is supported. Authentication through
advanced authentication scheme (SASL, GSS-API for Kerberos, ...) will be supported later). 