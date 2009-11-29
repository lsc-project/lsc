#!/bin/bash

#==========================================================================
# Summary
#==========================================================================
# Log file analyzer for LSC for Nagios
# 
# This can read LSC logs to detect time since last succesful execution.
# An alert will be thrown if either of these durations exceeds a limit.
# 
#
# Copyright (C) 2008 Jonathan Clarke <jclarke@linagora.com>
# Copyright (C) 2008 LINAGORA <http://www.linagora.com>
# 
#==========================================================================
# License: GPLv2
#==========================================================================
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
#
#==========================================================================
# Please visit http://www.linagora.org for other contributions
#==========================================================================

#==========================================================================
# Changelog
#==========================================================================
# Version 1.0 (2008/02/28):
# - First version
# Author: Jonathan Clarke <jclarke@linagora.com>
#==========================================================================

#
# Usage: ./check_lsc -F <log_file> -w <warningT> -c <criticalT>
#

# Pattern to match in log file
SUCCESS_PATTERN="Starting LSC"

# Paths to commands used in this script.  These
# may have to be modified to match your system setup.

PROGNAME=`basename $0`
PROGPATH=`echo $0 | sed -e 's,[\\/][^\\/][^\\/]*$,,'`
REVISION=`echo '$Revision: 1.0 $' | sed -e 's/[^0-9.]//g'`

. $PROGPATH/utils.sh

print_usage() {
    echo "Usage: $PROGNAME -F logfile -w warnlevel -c criticallevel"
    echo "Usage: $PROGNAME --help"
    echo "Usage: $PROGNAME --version"
}

print_help() {
    print_revision $PROGNAME $REVISION
    echo ""
    print_usage
    echo ""
    echo "Log file analyzer for LSC for Nagios"
    echo ""
    support
}

# Make sure the correct number of command line
# arguments have been supplied

if [ $# -lt 6 ]; then
    print_usage
    exit $STATE_UNKNOWN
fi

# Grab the command line arguments

exitstatus=$STATE_UNKNOWN #default
while test -n "$1"; do
    case "$1" in
        --help)
            print_help
            exit $STATE_OK
            ;;
        -h)
            print_help
            exit $STATE_OK
            ;;
        --version)
            print_revision $PROGNAME $REVISION
            exit $STATE_OK
            ;;
        -V)
            print_revision $PROGNAME $REVISION
            exit $STATE_OK
            ;;
        --filename)
            logfile=$2
            shift
            ;;
        -F)
            logfile=$2
            shift
            ;;
	-w)
		warnlevel=$2
		shift
		;;
	-c)
		criticallevel=$2
		shift
		;;
        *)
            echo "Unknown argument: $1"
            print_usage
            exit $STATE_UNKNOWN
            ;;
    esac
    shift
done

# If the log file doesn't exist, exit

if [ ! -e $logfile ]; then
    echo "Log check error: Log file $logfile does not exist!\n"
    exit $STATE_UNKNOWN
elif [ ! -r $logfile ] ; then
    echo "Log check error: Log file $logfile is not readable!\n"
    exit $STATE_UNKNOWN
fi

# Get the last matching entry in the diff file
lastentry=`grep "$SUCCESS_PATTERN" $logfile | tail -1`

timeLastEntry=`echo $lastentry | sed 's/\([0-9]\{4\}\/[0-9]\{2\}\/[0-9]\{2\} [0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}\).*/\1/'`
timestampLastEntry=`date -d "$timeLastEntry" +%s`
timestampNow=`date +%s`
age=$(( ($timestampNow - $timestampLastEntry) / 60))

if [[ $age -lt $warnlevel ]]; then # recent execution, exit with no error
    echo "OK: Last successful execution $age minutes ago"
    exitstatus=$STATE_OK
elif [[ $age -ge $warnlevel && $age -lt $criticallevel ]]; then
    echo "WARN: Last successful execution $age minutes ago"
    exitstatus=$STATE_WARNING
elif [[ $age -ge $criticallevel ]]; then
	echo "CRITICAL: Last successful execution $age minutes ago"
	exitstatus=$STATE_CRITICAL
fi

exit $exitstatus
