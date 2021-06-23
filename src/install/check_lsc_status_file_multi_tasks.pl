#! /usr/bin/perl -w

#==========================================================================
# Summary
#==========================================================================
# Check LSC status trough log file for Multi tasks
#
# Copyright (C) 2012 Clement OUDOT
# Copyright (C) 2012 LSC-project.org
#
# Adaptation 2018 Florian CAILLET 
#
#==========================================================================
# License: BSD
#==========================================================================

#==========================================================================
# Version
#==========================================================================
my $VERSION = '2.1.0';

#==========================================================================
# Modules
#==========================================================================
use strict;
use Getopt::Long;
&Getopt::Long::config('bundling');
use File::Basename;

my %ERRORS = (
    'OK'       => 0,
    'WARNING'  => 1,
    'CRITICAL' => 2,
    'UNKNOWN'  => 3,
);

#==========================================================================
# Options
#==========================================================================
my $progname = basename($0);
my $help;
my $verbose = 0;
my $warning;
my $critical;
my $perf_data;
my $logfile;
my $delay;
my $ignoreEmptySourceError;

GetOptions(
    'h'                      => \$help,
    'help'                   => \$help,
    'v+'                     => \$verbose,
    'verbose+'               => \$verbose,
    'w:f'                    => \$warning,
    'warning:f'              => \$warning,
    'c:f'                    => \$critical,
    'critical:f'             => \$critical,
    'f'                      => \$perf_data,
    'perf_data'              => \$perf_data,
    'l:s'                    => \$logfile,
    'logfile:s'              => \$logfile,
    'd:f'                    => \$delay,
    'delay:f'                => \$delay,
    'i'                      => \$ignoreEmptySourceError,
    'ignoreEmptySourceError' => \$ignoreEmptySourceError,
);

#==========================================================================
# Usage
#==========================================================================
sub print_usage {
    print "Usage: \n";
    print "$progname -l <logfile> [-h] [-v]\n\n";
    print "Use option --help for more information\n\n";
    print "$progname comes with ABSOLUTELY NO WARRANTY\n\n";
}

#=========================================================================
# Help
#=========================================================================
if ($help) {
    print "$progname version $VERSION\n";

    print "\nCheck LSC status\n";

    &print_usage;

    print "-v, --verbose\n";
    print "\tPrint extra debugging information.\n";
    print "-h, --help\n";
    print "\tPrint this help message and exit.\n";
    print "-l, --logfile\n";
    print "\tPath to LSC status log file.\n";
    print "-w, --warning=DOUBLE\n";
    print "\tError level to return a warning status.\n";
    print "-c, --critical=DOUBLE\n";
    print "\tError level to return a critical status.\n";
    print "-f, --perf_data\n";
    print "\tDisplay performance data.\n";
    print "-d, --delay\n";
    print
"\tDelay in seconds of LSC execution after which a critical error is returned.\n";
    print "-i, --ignoreEmptySourceError\n";
    print "Ignore 'Empty or non existant source' error\n";
	
    print "\n";

    exit $ERRORS{'UNKNOWN'};
}

#=========================================================================
# Functions
#=========================================================================

# DEBUG function
sub verbose {
    my $output_code = shift;
    my $text        = shift;
    if ( $verbose >= $output_code ) {
        printf "VERBOSE $output_code ===> %s\n", $text;
    }
}

# check if -l is used
sub check_logfile_param {
    if ( !defined($logfile) ) {
        printf "UNKNOWN: you have to define a logfile.\n";
        exit $ERRORS{UNKNOWN};
    }
}

# check if -w is used
sub check_warning_param {
    if ( !defined($warning) ) {
        printf "UNKNOWN: you have to define a warning thresold.\n";
        exit $ERRORS{UNKNOWN};
    }
}

# check if -c is used
sub check_critical_param {
    if ( !defined($critical) ) {
        printf "UNKNOWN: you have to define a critical thresold.\n";
        exit $ERRORS{UNKNOWN};
    }
}

#=========================================================================
# Main
#=========================================================================

# Options checks
&check_logfile_param();
&check_warning_param();
&check_critical_param();

# Check if the file is empty or not
if ( -z $logfile ) {
    printf "UNKNOWN: LSC log file is empty.\n";
    exit $ERRORS{UNKNOWN};
}

# Open logfile
unless ( open( LOG, "$logfile" ) ) {
    printf "UNKNOWN: unable to parse LSC log file.\n";
    exit $ERRORS{UNKNOWN};
}

# Check file age
my $fileage = -M $logfile;
$fileage = int( $fileage * 3600 * 24 );

if ( defined $delay and $delay < $fileage ) {
    printf "CRITICAL: LSC log file too old ($fileage seconds).\n";
    exit $ERRORS{CRITICAL};
}

if ( $fileage < 5 ) {
    printf "UNKNOWN: log file is still being written, cannot get status.\n";
    exit $ERRORS{UNKNOWN};
}

# Get all messages

my $sumEntries = 0;
my $sumModify = 0;
my $sumModified = 0;
my $sumErrors = 0;
my $logNagios = "";

my $last;

while(my $line = <LOG>)
{
	if (    $ignoreEmptySourceError
		and $line  =~ /ERROR - (.*)$/
		and $line =~ /Empty or non existant source/i )
	{
		#Ignore this type of error
	}


	if ( $line =~
	/\w+ - \w+ - All entries: (\d+), to modify entries: (\d+), (?:successfully )?modified entries: (\d+), errors: (\d+)/mi
	  ){
	
		# Sum entries and modify operations
		$sumEntries += $1;
		$sumModify += $2;
		$sumModified += $3;
		$sumErrors += $4;
		
		$logNagios .= $line;
	}
	
	$last = $line;
}


# Verified that the last instruction in the log file isn't en ERROR

if ( $last =~ /ERROR - (.*)$/ 
	and $last !~ /\w+ - \w+ - All entries:/ 
	and $last !~ /Empty or non existant source/i ) {
	print "CRITICAL: LSC error $1.\n";
	exit $ERRORS{CRITICAL};
}


#==========================================================================
# Exit with Nagios codes
#==========================================================================

# Prepare PerfParse data
my $perfparse = " ";
if ($perf_data) {
    $perfparse = "|'all'=$sumEntries 'modify'=$sumModify 'modified'=$sumModified 'errors'=$sumErrors;$warning;$critical\n".$logNagios;
}

# Test the errors and exit
if ( $sumErrors == 0 or $sumErrors < $warning ) {
    print
"OK - LSC is running with $sumErrors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'OK'};
}
elsif ( $sumErrors >= $warning and $sumErrors < $critical ) {
    print
"WARNING - LSC is running with $sumErrors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'WARNING'};
}
else {
    print
"CRITICAL - LSC is running with $sumErrors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'CRITICAL'};
}

exit $ERRORS{'UNKNOWN'};
