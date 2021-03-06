#!/usr/bin/perl

# Script to run fixity checks on objects stored in the CDR
# 
# To run, modify your .irodsEnv to make the user fedora
# and connect to iRODS (iinit) and run the script.  It is set
# up to start in the passed in path and resources and verify 
# checksums on objects which haven't been checked in 6 months.

use strict;
use POSIX; # needed for number processing

sub trim($);
sub needsChecksum($$);
sub extractResources(@);

#my $sixMonthsInSeconds = 6; # for testing 
my $sixMonthsInSeconds = 15778463; # six months in seconds

my $currentTime = time();
my $sixMonthsAgo = $currentTime - $sixMonthsInSeconds;

my $goodoutputfile = "good_checksums.log.$currentTime";
my $badoutputfile = "bad_checksums.log.$currentTime";

my @requiredResources = ();

if ($#ARGV <= 0) {
	print "usage: cdr_fixity_check /zoneName/home/fedora resource1 resource2\n";
	exit;
}

# extract resources from command line and load into hash
for my $i (1 .. $#ARGV) {
	push(@requiredResources, $ARGV[$i]); 

	# print "$ARGV[$i]\n";
}

# my $startingdirectory = "/cdrZone/home/fedora";
my $startingdirectory = $ARGV[0];

# Change to target iRODS directory
qx{icd $startingdirectory};

# Get the directory tree
my @var = qx{ils -r};  # ils -r

# print @var->[0];

my $currentdirectory = "";

open GOODFILE, ">>", $goodoutputfile or die $!;
open BADFILE, ">>", $badoutputfile or die $!;

print GOODFILE strftime("%Y-%m-%d", localtime), "\n";
print BADFILE strftime("%Y-%m-%d", localtime), "\n";

# For each directory and file
for my $i (0 .. $#var) {
    
    # See if it is a stored object (may want to change this check to include the very few system files that don't
    # start with uuid)
    if(trim($var[$i]) =~ m/^(uuid)/i) {
      my $filename = trim($var[$i]);
      
	# check to see if this file needs a fixity check at this time
	my $needschecksum = needNewChecksum($currentdirectory, $filename);

	if($needschecksum) {
	      # Run a checksum on the file
	      my $checksum = `ichksum -a -K $currentdirectory/$filename 2>&1`;

	      # print "$checksum\n";

	      # parse the results
      
	      if(trim($checksum) =~ m/^(ERROR)/i) {
				
			for my $i (0..$#requiredResources) 
			{
	      			# Run a checksum on the file
				my $cs4resource = `ichksum -a -K -R $requiredResources[$i] $currentdirectory/$filename 2>&1`;
			        if(trim($cs4resource) =~ m/^(ERROR)/i) {
					print BADFILE "$currentdirectory/$filename failed a checksum on resource $requiredResources[$i]\n";
					print BADFILE $checksum;
				}
			}	
	      }
	      else {


		my @values = split(' ',$checksum);

		# record the successful outcome and update set the current fixity timestamp
		print GOODFILE "$currentdirectory/$filename $values[1]\n";
	        # print GOODFILE $checksum;

	  	my $updateAVU = `imeta add -d $currentdirectory/$filename cdrFixity $currentTime`;
	      }
	}
    }
    else {
      # directory comes in as 'directorypath:' so we need to remove the ':'
      if ((trim($var[$i]) =~ /:$/) && (trim($var[$i]) =~ /\//)) {
              $currentdirectory = substr(trim($var[$i]), 0, -1);
      } 
    }
}


# get cdrFixity AVU and extract current value; return if it has been too long and need to calculate checksum again
sub needNewChecksum($$)
{
       my $currentdirectory = shift;
       my $filename = shift;

       # print "$currentdirectory/$filename\n";

	# query for the cdrFixity metadata
        my @fixityAVU = `imeta ls -d $currentdirectory/$filename cdrFixity`;

	my $arraySize = @fixityAVU;

	# see if a value is set; if so, extract it
	if(($arraySize > 2) && ($fixityAVU[2] =~ m/^(value)/i)) {
		my @array = split(/\s+/,$fixityAVU[2]);

		$arraySize = @array;

		# parse and check time
		if(($arraySize == 2) && (isdigit(trim(@array[1])))) {
			
			if(trim(@array[1]) > $sixMonthsAgo) { # don't do a checksum at this time

				return 0;
			} else {
				# remove old fixity value; otherwise they accumulate

				`imeta rmw -d $currentdirectory/$filename cdrFixity %`;

				return 1; # perform a new checksum
			}
		} else {

			print BADFILE "Problem: $currentdirectory/$filename has cdrFixity of '@array[1]'";
			return 0; # something wrong with cdrFixity value; need to look into problem
		}
	} else {

		return 1; # probably no checksum present, so request one
	}


	return 1; # get a new checksum if we end up here, just in case
}

sub extractResources(@) 
{
	my (@lsListing) = @_;

	my %fileResources = ();

        foreach (@lsListing) { 	
		my @array = split(/\s+/,$_);

		# print "$array[0] v $array[1] v $array[2] v $array[3]\n";

		$fileResources{$array[3]} = $array[3];
	}

	return %fileResources;
}

# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
