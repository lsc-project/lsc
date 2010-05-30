#!/bin/bash

line=""
read line
text="$line"

while test "$line" != ""
do
 read line
 text="$text
$line"
done

echo "Deleting account $1"

echo "$text" | ldapmodify -x -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PW" -H "$LDAP_URL" 

exit $?
