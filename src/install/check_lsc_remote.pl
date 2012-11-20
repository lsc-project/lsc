#! /usr/bin/perl -w

#==========================================================================
# Summary
#==========================================================================
# Check LSC task status
# Need lsc-agent bin to work
#
# Copyright (C) 2012 Clement OUDOT
# Copyright (C) 2012 LSC-project.org
#
#==========================================================================
# License: BSD
#==========================================================================

#==========================================================================
# Version
#==========================================================================
my $VERSION = '2.0.2';

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
my $host;
my $warning;
my $critical;
my $perf_data;
my $port;
my $task;
my $bin_lsc_agent;

GetOptions(
    'h'          => \$help,
    'help'       => \$help,
    'v+'         => \$verbose,
    'verbose+'   => \$verbose,
    'H:s'        => \$host,
    'host:s'     => \$host,
    'w:f'        => \$warning,
    'warning:f'  => \$warning,
    'c:f'        => \$critical,
    'critical:f' => \$critical,
    'f'          => \$perf_data,
    'perf_data'  => \$perf_data,
    'p:i'        => \$port,
    'port:i'     => \$port,
    't:s'        => \$task,
    'task:s'     => \$task,
    'b:s'        => \$bin_lsc_agent,
    'bin:s'      => \$bin_lsc_agent,

);

$bin_lsc_agent ||= "/usr/bin/lsc-agent";

#==========================================================================
# Usage
#==========================================================================
sub print_usage {
    print "Usage: \n";
    print "$progname -H <hostname> -t <task> [-h] [-v]\n\n";
    print "Use option --help for more information\n\n";
    print "$progname comes with ABSOLUTELY NO WARRANTY\n\n";
}

#=========================================================================
# Help
#=========================================================================
if ($help) {
    print "$progname version $VERSION\n";

    print "\nCheck LSC task status\n";

    &print_usage;

    print "-v, --verbose\n";
    print "\tPrint extra debugging information.\n";
    print "-h, --help\n";
    print "\tPrint this help message and exit.\n";
    print "-H, --host=STRING\n";
    print "\tIP or name (FQDN) of the LSC connector\n";
    print "-p, --port=INTEGER\n";
    print "\tJMX port port to connect to.\n";
    print "-w, --warning=DOUBLE\n";
    print "\tError level to return a warning status.\n";
    print "-c, --critical=DOUBLE\n";
    print "\tError level to return a critical status.\n";
    print "-f, --perf_data\n";
    print "\tDisplay performance data.\n";
    print "-b, --bin\n";
    print "\tPath to lsc-agent binary.\n";

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

# check if -H is used
sub check_host_param {
    if ( !defined($host) ) {
        printf "UNKNOWN: you have to define a hostname.\n";
        exit $ERRORS{UNKNOWN};
    }
}

# check if -t is used
sub check_task_param {
    if ( !defined($task) ) {
        printf "UNKNOWN: you have to define a task.\n";
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

sub check_bin {
    if ( !-x $bin_lsc_agent ) {
        printf "UNKNOWN: cannot execute $bin_lsc_agent.\n";
        exit $ERRORS{UNKNOWN};
    }
}

#=========================================================================
# Main
#=========================================================================

# Options checks
&check_host_param();
&check_task_param();
&check_warning_param();
&check_critical_param();

# Launch lsc-agent
&check_bin();
my $output = `$bin_lsc_agent -h $host -p $port -s $task`;
my $result = $?;

# Check task stopped error
if ( $output =~ /Asynchronous task $task is stopped/mi ) {
    printf "ERROR: LSC task $task is stopped.\n";
    exit $ERRORS{CRITICAL};
}

# Check if we have a valid result
if ($result) {
    printf "ERROR: Bad LSC agent status. Check host, port and task name\n";
    exit $ERRORS{CRITICAL};
}

# Get statistics
my ( $all, $modify, $modified, $errors ) =
  ( $output =~
/All entries: (\d+), to modify entries: (\d+), successfully modified entries: (\d+), errors: (\d+)/mi
  );

#==========================================================================
# Exit with Nagios codes
#==========================================================================

# Prepare PerfParse data
my $perfparse = " ";
if ($perf_data) {
    $perfparse =
"|'all'=$all 'modify'=$modify 'modified'=$modified 'errors'=$errors;$warning;$critical";
}

# Test the errors and exit
if ( $errors == 0 or $errors < $warning ) {
    print
"OK - LSC task $task is running with $errors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'OK'};
}
elsif ( $errors >= $warning and $errors < $critical ) {
    print
"WARNING - LSC task $task is running with $errors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'WARNING'};
}
else {
    print
"CRITICAL - LSC task $task is running with $errors errors (W:$warning - C:$critical)$perfparse\n";
    exit $ERRORS{'CRITICAL'};
}

exit $ERRORS{'UNKNOWN'};
