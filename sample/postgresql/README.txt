This README file is describing how to setup a sample PostgreSQL server and to 
setup the synchronization from and to an OpenDJ instance.

REQUIREMENTS
------------

First, please check that you have downloaded and deployed the PostgreSQL
JDBC jar from the PostgreSQL community website. This sample has been tested
with PostgreSQL-8.4-702-jdbc4.jar but may worked with any feature release and
also previous release, but with a transactional support.

POSTGRESQL SERVER SETUP
-----------------------
This sample suppose that the SQL server is installed and correctly setup.
Please open a psql command line with database administrator rights on the
embedded postgres database and drop any existing database and role lsc, BUT
TAKE CARE THAT YOU HAVE ALREADY BACKUPED ANY EXISTING DATA :

postgres=# DROP DATABASE IF EXISTS lsc;
postgres=# DROP ROLE IF EXISTS lsc;

Go in the right directory and launch the creation script :

postgres=# \cd $LSC_HOME/sample/postgresql
postgres=# \i create.sql

You should see some information message and maybe some warnings. If you don't
encountered an error, you can continue at the following step. If not, check
which is the first SQL command that has failed and solved the issue before
relaunching a copy of the script in which you will have copied only the SQL
commands that have not succeed first time. 

LSC SETUP
---------

You will find in the etc subdirectory a correct configuration to use with 
newly setup server. You need to edit the lsc.xml to change the server IP
address, TCP port, username and password :

      <url>jdbc:postgresql://127.0.0.1:5432/lsc</url>
      <username>lsc</username>
      <password>lsc</password>

This sample requires that you start the embedded OpenDJ LDAP server:
$ sample/hsqldb/bin/lsc-sample --start-ldap-server

In a new console, populate the LDAP server by using the HSQLDB sample:
$ sample/hsqldb/bin/lsc-sample --import sample/hsqldb/sample.csv
$ sample/hsqldb/bin/lsc-sample --run

Then, launch the LSC in a command line to start LDAP to postgresql synchronization: 
$ bin/lsc -f sample/postgresql/etc-opendj2postgresql -s all -c all

Or launch the LSC in a command line to start an asynchronous postgresql to LDAP task: 
$ bin/lsc -f sample/postgresql/etc-postgresql2opendj -a all

When the asynchronous task has synchronized the default jdoe account, add the jsmith by launching the insert.sql script :
postgres=# \i insert.sql

And now you should get a OpenDJ and a PostgreSQL synchronized: you should see add operations which will add entries inside your lsc database and inetorgperson table.
