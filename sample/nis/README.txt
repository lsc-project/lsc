This README file is describing how to setup a sample NIS server and to setup 
the synchronization.

REQUIREMENTS
------------

First, please check that you have downloaded and deployed the nis plugin jar 
from LSC Project main website. Please also check that you have the nis jar
provided by Oracle (ex Sun) and note that this dependency is not Open Source
and is licensed under the Sun Binary license and thus can not be 
redistributed freely.

NIS SERVER SETUP
----------------
Then connect to your Debian box and install the nis package :
$ su -
$ apt-get install nis

At this step, please wait until ypbind timeouts because your NIS server is 
not already setup. Then go to /var/yp and create the required domain directory
structure :
$ cd /var/yp
$ mkdir -p lsc-project.org
$ cd lsc-project.org
$ make -f ../Makefile all

You may get some warning message about unregistered RPC service. You can safely
ignore them. Then check your installation by launching the server in debug mode
$ ypserv -d

Then launch the ypbind client in debug mode to check for connection correctness
$ ypbind -d

You must see that the connection is well established. Then open a new terminal
and check for the final test through the ypcat client :
$ ypcat passwd
 
You must see your current accounts shown there and that's it.

Then stop both daemon started in debug mode and start them again without the
-d flag. They will daemonize themselves, and it's now time to setup LSC.

LSC SETUP
---------

You will find in the etc subdirectory a correct configuration to use with 
newly setup server. You need to edit the lsc.xml to change the server IP
address and the managed NIS domain in the following line :

      <url>nis://NIS-SERVER-ADDRESS/lsc-project.org</url>

Note that this synchronization will lookup accounts through the 
passwd.byname map :

	    <map>passwd.byname</map>

This means that if you want to synchronize groups, you will need to change the
map you plan to use.

This sample requires that you start the embedded OpenDJ LDAP server:
$ sample/hsqldb/bin/lsc-sample --start-ldap-server

Then, launch the LSC in a command line 
$ bin/lsc -f sample/nis/etc -s passwd

And now you should get a NIS server and a directory synchronized :
you should see a renaming operation which will change the RDN of the uid=00000001
entry to mail=test@lsc-project.org
