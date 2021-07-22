#!/bin/sh

cd $CONF_DIR

sed -i "s~LSC_LDAP_URL~$LSC_LDAP_URL~g" lsc.xml
sed -i "s/LSC_LDAP_BIND_DN/$LSC_LDAP_BIND_DN/g" lsc.xml
sed -i "s/LSC_LDAP_BIND_PW/$LSC_LDAP_BIND_PW/g" lsc.xml
sed -i "s/LSC_JAMES_ADMIN_URL/$LSC_JAMES_ADMIN_URL/g" lsc.xml
sed -i "s/LSC_LDAP_BASE_DN/$LSC_LDAP_BASE_DN/g" lsc.xml

