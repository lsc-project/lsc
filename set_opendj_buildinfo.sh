#!/bin/bash

version=2.6.4.2af80a949263f9233e7048de3cc57a53f1f9d410

echo $version > src/test/resources/etc/config/buildinfo
echo $version > sample/ldap2hsqldb/etc/config/buildinfo
