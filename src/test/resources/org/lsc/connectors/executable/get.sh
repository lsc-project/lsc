#!/bin/bash

line=""
read line
text="$line"
filter="(&(`echo "$line" | sed -e "s/^\([a-zA-Z0-9]\+\): \(.*\)$/\1=\2/g"`)"

while test "$line" != ""
do
 read line
 text="$text
$line"
 if test "$line" != ""; then
   filter="$filter(`echo "$line" | sed -e "s/^\([a-zA-Z0-9]\+\): \(.*\)$/\1=\2/g"`)"
 fi 
done

filter="$filter)"

echo "Getting user information for id=$1" 1>&2
ldapsearch -x -LLL -H "$LDAP_URL" -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PW" -b "$LDAP_BASE" -s "$LDAP_SCOPE" "$filter" \
	| sed -e "s/^\(dn: .*\),dc=lsc-project, *dc=org$/\1/g" 

exit $?
