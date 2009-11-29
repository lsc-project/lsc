#!/bin/sh

#====================================================================
# Script for LDAP Synchronization Connector
#
# Create an archive for LSC distribution/installation
# Edit this script to set up configuration items.
# This script must be run from lsc-sample root:
#   $ src/install/make-lsc-archive.sh
#
# Requires ant and maven (with binaries in PATH)
#
# Copyright (C) 2008 Clement OUDOT
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# GPL License: http://www.gnu.org/licenses/gpl.txt
#====================================================================


#====================================================================
# Changelog
#====================================================================
# Version 0.4 (07/2009)
# - Update for new layout of configuration in LSC 1.1.0
# Author: Jonathan Clarke (jonathan@phillipoux.net)
#
# Version 0.3 (03/2009)
# - Create "sedi" function compliant with GNU sed
# - Use Maven dependency plugin
# - Compliant with MinGW/MSYS under Windows
# Author: Clement OUDOT (LINAGORA)
#
# Version 0.2 (02/2009)
# - Configure cron and logrotate
# - Copy locales resources
# Author: Clement OUDOT (LINAGORA)
#
# Version 0.1 (10/2008):
# - First version
# Author: Clement OUDOT (LINAGORA)
#====================================================================

#====================================================================
# Configuration
#====================================================================
# Connector name (please provide different names for each connector installed on the same machine)
LSCNAME="lsc-my-connector"
# Connector version
LSCVERSION="1.0"
# Target directory to create archive (relative to lsc-sample root)
TGTDIR="target"
# Distribution dir (LSC container in target directory)
LSCDIR=$LSCNAME
DSTDIR=$TGTDIR/$LSCDIR
# Target installation dir (where will be LSC deployed)
INSTDIR="/usr/local/$LSCNAME"
# Target JAVA_HOME dir
INSTJAVAHOME="/usr/java/jdk1.5.0_16"
# Archive name
ARCNAME="$LSCNAME-$LSCVERSION"
# Script name
SCRIPTFILENAME=$LSCNAME
# Other
CFGPATH="etc"
CFGDIR=$DSTDIR/$CFGPATH
BINPATH="bin"
BINDIR=$DSTDIR/$BINPATH
LIBPATH="lib"
LIBDIR=$DSTDIR/$LIBPATH
RSCPATH="$CFGPATH/resources"
RSCDIR=$DSTDIR/$RSCPATH
# Log and cron
LOGDIR="/var/log"
#LOGDIR=$DSTDIR/log
LOGFILE=$LOGDIR/$LSCNAME.log
LOGROTATEFILENAME=$LSCNAME
CRONFILENAME=$LSCNAME

#====================================================================
# Functions
#====================================================================
# This function use GNU sed and simulate -i option (inline)
# sedi "sed script" "file"
sedi() {
    script=$1
    file=$2
    tmpfile=$file.".tmp"

    sed -e "$script" $file > $tmpfile
    mv $tmpfile $file
}

#====================================================================
# Main
#====================================================================
# Prepare directories                                                
echo "-- Prepare"                                                    
rm -rf $DSTDIR                                                       
ant lsc::clean > /dev/null                                           
echo "-- Create directories"                                         
mkdir -p $DSTDIR                                                     
mkdir -p $CFGDIR                                                     
mkdir -p $CFGDIR/cron.d                                              
mkdir -p $CFGDIR/logrotate.d                                         
mkdir -p $BINDIR                                                     
mkdir -p $LIBDIR                                                     
mkdir -p $RSCDIR                                                     

# Create lsc-sample package and copy it in LIBDIR
echo "-- Package"
mvn package > /dev/null
cp -a target/*.jar $LIBDIR > /dev/null

# Get jar dependencies (with maven dependency plugin) and copy them in LIBDIR
echo "-- Resolve java dependencies"
mvn dependency:copy-dependencies -DoutputDirectory="$LIBDIR" > /dev/null

# Copy configuration
echo "-- Configuration"
cp -ar etc/* $CFGDIR > /dev/null

# Copy run scripts
echo "-- Scripts"
cp -a src/install/lsc.bin $BINDIR/$SCRIPTFILENAME > /dev/null
cp -a src/install/check_lsc.sh $BINDIR/check_lsc.sh > /dev/null
cp -a src/install/lsc.cron $CFGDIR/cron.d/$CRONFILENAME > /dev/null
cp -a src/install/lsc.logrotate $CFGDIR/logrotate.d/$LOGROTATEFILENAME > /dev/null

echo "-- Resources"
for i in src/main/resources/*; do
        filename=`basename $i`
        cp -a $i $RSCDIR/$filename.properties > /dev/null
done
chmod 644 $RSCDIR/* > /dev/null

echo "-- Replace default values"
# Update lsc binary
sedi "s|^JAVA_HOME=.*|JAVA_HOME=$INSTJAVAHOME|" $BINDIR/$SCRIPTFILENAME
sedi "s|^LSC_HOME=.*|LSC_HOME=$INSTDIR|" $BINDIR/$SCRIPTFILENAME
sedi "s|^CFG_DIR=.*|CFG_DIR=\$LSC_HOME/$CFGPATH|" $BINDIR/$SCRIPTFILENAME
sedi "s|^LIB_DIR=.*|LIB_DIR=\$LSC_HOME/$LIBPATH|" $BINDIR/$SCRIPTFILENAME
sedi "s|^LOG_DIR=.*|LOG_DIR=$LOGDIR|" $BINDIR/$SCRIPTFILENAME
sedi "s|^LOG_FILE=.*|LOG_FILE=$LOGFILE|" $BINDIR/$SCRIPTFILENAME
# Update lsc cron
sedi "s|#LSC_BIN#|$INSTDIR/$BINPATH/$SCRIPTFILENAME|g" $CFGDIR/cron.d/$CRONFILENAME
# Update lsc logrotate
sedi "s|#LSC_LOGFILE#|$LOGFILE|g" $CFGDIR/logrotate.d/$LOGROTATEFILENAME
# Update log4j.properties
sedi "s|^log4j.appender.LSC.File=.*|log4j.appender.LSC.File=$LOGFILE|" $CFGDIR/log4j.properties

echo "-- Set permissions"
# Set permissions
chmod 755 $BINDIR/$SCRIPTFILENAME > /dev/null                                     
chmod 755 $BINDIR/check_lsc.sh > /dev/null                                        
chmod 644 $CFGDIR/cron.d/$CRONFILENAME > /dev/null                                
chmod 644 $CFGDIR/logrotate.d/$LOGROTATEFILENAME > /dev/null                      

# Make tar.gz
echo "-- Create archive"
cd $TGTDIR
rm -f $ARCNAME.tar.gz
tar --owner 0 --group 0 -cf $ARCNAME.tar $LSCDIR > /dev/null
gzip $ARCNAME.tar > /dev/null
cd ..

# Remove distribution directory
echo "-- Clean"
rm -rf $DSTDIR

#====================================================================
# Exit
#====================================================================
echo "-- Archive $TGTDIR/$ARCNAME.tar.gz created"
exit 0
