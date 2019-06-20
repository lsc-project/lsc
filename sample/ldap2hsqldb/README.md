LDAP to HSQLDB Example
======================

# 1. Description
This sample shows the synchronisation beteen a ldap directroy and a sql database. 
The use case is, that you have an application which stores your user information into its database
but uses LDAP for authentication. The user data needs to be copied into the sql database so that users
do not need to maintain their information twice. Changes in the ldap user information should be reflected
in the database.

# 2. Environment
To play with this sample, you must have a bash interpreter installed. I recommend installing
a LDAP Administration tool and a SQL Client.

## 2.1 Bash Interpreter
On Linux the bash interpreter is already preinstalled and you probably already using it. 
On Windows, you need to install [Cygwin](https://www.cygwin.com/) or [Git Bash](https://gitforwindows.org/).

## 2.2 Java
You need a Java 8 Runtime environment.  
You can install the latest OpenJDK Version 8 on Linux or Windows.
In case you are a private user or own a license you can also use the Oracle JVM.
A version from a different vendor should also work.  
Make sure your PATH is set correctly so that typing `java --version` gives you the current version
of your Java Runtime.

## 2.3 LDAP Administrator
On Linux you have the command line tools from the openldap-client package. 
A gaphical LDAP Administrator is, for example, [Apache Directory Studio](https://directory.apache.org/studio/downloads.html).
This is available for Windows, Linux and Mac OS X.  
A more native Windows application is (LDAP Admin)[http://www.ldapadmin.org/].
There are many other applications arround. So choose the one you like best.

## 2.4 Database Client
The used HSQLDB comes with a simple Database Client. You can run it from the sample application. 
In case this is not enough, you could use (SQuirreL SQL)[http://squirrel-sql.sourceforge.net/]. 
There are also alternatives from Apache or every Database vendor provides its own client.

# 3. Setup
To get the sample running, follow this simple steps:

1. Download and extract the lsc package.  
2. Get the lsc-sample command known  
   Use the bash interpreter and change into the directory of the sample:  
   `cd <directory of extracted lsc package>/sample/ldap2sql`
   run the lsc-sample command:  
   `./bin/lsc-sample`  
   Review the output of the command.
3. Start the ldap server  
   `./bin/lsc-sample --start-ldap-server`
4. Start the hsql db server  
   `./bin/lsc-sample --start-hsql-server`

# 4. Play with the sample

1. To add a random externalId to each LDAP Record so that the record synchronizes to the DB run:  
   `./bin/lsc-sample  --set-ext-id`
2. Synchronize LDAP records into the database  
   `./bin/lsc-sample  --run`
3. Investigate database  
   Use your DB Client or run the integrated one to check the contents of the database.  
   `./bin/lsc-sample  --dbtool`
4. Change values inside the LDAP  
   e.g. Add / Modify values of synchronized attributes   
   set `accessSA` to false to get the entry removed from the database.

# 5. Shutdown
1. Shutdown the hsql db server  
   `./bin/lsc-sample --stop-hsql-server`
2. Shutdown the ldap server  
   `./bin/lsc-sample --stop-ldap-server`
3. Clean up  
   `./bin/lsc-sample --clean`
