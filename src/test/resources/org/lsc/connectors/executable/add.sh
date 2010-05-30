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

echo "Adding new account $1 :
$text" 

echo "$text" | ldapadd -x -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PW" -H "$LDAP_URL"

exit $?
