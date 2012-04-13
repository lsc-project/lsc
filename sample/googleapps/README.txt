Google Apps connector
---------------------

TAKE CARE THAT THIS SERVICE DOES NOT DELETE USERS ACCOUNT, IT ONLY 
SUSPENDS THEM ! (See at below how to delete them if you need it)

This LSC destination service enables the provisioning of Google Apps users 
account. 

It has not been tested (and is not currently supported) for:
- multi domains
- OAuth and Captcha authentication

If you are looking for a way to sync your contacts 

To use this connector, setup a task with standard source connection and 
service. Then just setup a destination connection:

    <googleAppsConnection>
    	<name>googleapps-dst-conn</name>
    	<url>domain.com</url>
    	<username>admin@domain.com</username>
    	<password>XXXXXXXX</password>
    </googleAppsConnection>

Just replace:
- the URL with the domain name to provision
- the username with the email address that has administrative privileges on 
   the previously specified domain
- the password corresponding to this email address

Then, setup the corresponding information regarding the destination service:

      <googleAppsDestinationService>
        <name>googleapps-dst-service</name>
        <connection reference="googleapps-dst-conn" />
        <apiCategory>UserAccounts</apiCategory>
        <quotaLimitInMb>1000</quotaLimitInMb>
      </googleAppsDestinationService>

You can synchronize at this time, only the UserAccounts but it will be later 
included Groups and OrganizationalUnits. The quotaLimitInMb is 
self-explanatory. It can be replaced at runtime by specifying a dataset with 
a customized value regarding a particular group for example.

When this is done, try your new service through an asynchronous task launch :
$ $LSC_HOME/bin/lsc -f $LSC_HOME/samples/googleapps/etc-opendj2googleapps -s all -t 1

or 

> %LSC_HOME%\bin\lsc.bat -f %LSC_HOME%\samples\googleapps\etc-opendj2googleapps -s all -t 1

There is also a way to use the Google Apps accounts as a source to synchronize
from Google Apps to another datasource. The configuration is almost the same. 
Just notice that quotaLimitInMb service parameter is not used.

** Delete accounts

The accounts can be deleted with a vanilla setup, LSC will only deactivate the
user account. This means that if a new user comes with the same login it will 
be able to access the data of the original user !

To delete it, specify the following environment variable:
I_UNDERSTAND_THAT_GOOGLEAPPS_ACCOUNTS_WILL_BE_DELETED_WITH_THEIR_DATA=1 

