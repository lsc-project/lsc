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

echo "Renaming account $1 to $2"

echo "$text" | ldapmodify -x -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PW" -H "$LDAP_URL"

exit $?
