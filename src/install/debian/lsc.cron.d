#
# Regular cron jobs for the Ldap Synchronization Connector
#

#30 * * * * lsc [ -x /usr/bin/lsc ] && /usr/bin/lsc -s all -c all > /dev/null 2>&1
