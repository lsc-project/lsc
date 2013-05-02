#!/usr/bin/perl -s -w

#====================================================================
# perl script computing AES crypted passwords and their SSHA hash,
# and pushing them to ldap directory
#
# INPUT : $1=user, $2=cleartext-password
# OUTPUT: 0 if everything is ok. (and computed crypted password is pushed to ldap directory)
#
#
# Copyright (C) 2013 David Coutadeur
# Copyright (C) 2013 LTB-project.org
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
# GPL License: http://www.gnu.org/licenses/gpl.txt

use strict;
use warnings;

use vars qw/ $h /;
use Crypt::Rijndael;
use MIME::Base64 qw( encode_base64 );
use Digest::SHA1 ("sha1");
use Net::LDAP;

#====================================================================
# Configuration
#====================================================================

# some crypto variables
# $keyfile is the file containing the key for AES encryption
my $keyfile   = "/path/to/my/file.key";
my $blocksize = 128 / 8;                  #( = 16 bytes)
open( FILE, "$keyfile" ) || die("Error opening key");
my $key = <FILE>;
close(FILE);

# some ldap variables
my $ldapUrl      = "ldap://localhost:389/";
my $ldapAdmin    = "cn=Manager,dc=my-domain,dc=com";
my $ldapPassword = "secret";
my $ldapBase     = "dc=my-domain,dc=com";

# ldapFilter: USER is replaced by appropriate input
my $ldapFilter = '(&(objectClass=inetOrgPerson)(uid=USER))';

# attribute used to store SSHA user password
my $userPasswordAttr = 'userPassword';

# choose a valid ldap attribute to hold AES encrypted password
my $userPasswordEncryptedAttr = 'userPasswordEncrypted';

#====================================================================
# Functions
#====================================================================

sub compute_password {

    die("no user password specified") if !exists( $_[0] );
    my $user_passwd = $_[0];

    # manipulation on the key : truncate + padding with null character
    $key = substr( $key, 0, 15 );
    $key = "$key" . ( "\0" x ( 16 - length $key ) );

    # manipulation on the input string : padding with PKCS5-padding
    my $nb_pad =
      ( ( $blocksize - ( length $user_passwd ) - 1 ) % $blocksize ) +
      1;    # -1 +1 is used to permute the 0 value by 16
    $user_passwd = "$user_passwd" . ( chr($nb_pad) x $nb_pad );

# keysize() is 32, but 24 and 16 are also possible. In this case: 16 -> 128bits
# blocksize() is 16 -> 128bits
    my $cipher = Crypt::Rijndael->new( $key, Crypt::Rijndael::MODE_ECB() );

    # second argument is to prevent eol=\n
    my $crypt = encode_base64( $cipher->encrypt($user_passwd), '' );
    return $crypt;
}

sub send_to_directory {
    my ( $user, $userPassword, $userPasswordEncrypted ) = @_;
    my $ldap = Net::LDAP->new("$ldapUrl");

    # bind to a directory with dn and password
    my $result = $ldap->bind( "$ldapAdmin", password => "$ldapPassword" );

    $ldapFilter =~ s/USER/$user/;
    $result = $ldap->search(
        base   => "$ldapBase",
        scope  => 'sub',
        filter => "$ldapFilter",
    );
    my @entries = $result->entries;

    # if entry is duplicated in directory, quits with error
    die( "More than one entry found for " . $user ) if scalar @entries > 1;

    # if entry is not found in directory, gently quits
    exit 0 if scalar @entries == 0;

    my $dn = $entries[0]->dn;

    # do the job : modify userPasswords
    $result = $ldap->modify(
        $dn,
        replace => {
            "$userPasswordAttr"          => "$userPassword",
            "$userPasswordEncryptedAttr" => "$userPasswordEncrypted",
        }
    );

    $result = $ldap->unbind;    # take down session

}

sub make_salt {
    my $length = 32;
    $length = $_[0] if exists( $_[0] );
    my @tab = ( '.', '/', 0 .. 9, 'A' .. 'Z', 'a' .. 'z' );
    return join "", @tab[ map { rand 64 } ( 1 .. $length ) ];
}

sub password_hash {
    my $password = $_[0];
    my $hash;
    my $salt = make_salt(4);
    $hash = "{SSHA}" . encode_base64( sha1( $password . $salt ) . $salt, '' );

    return $hash;
}

sub usage() {
    print "USAGE:\n";
    print "./passwordhk.pl [-h] uid password\n\n";
}

#====================================================================
# Entry point
#====================================================================

if ($h) {
    usage();
    exit(0);
}
if ( ( scalar @ARGV ) < 2 ) {
    print "Missing or incorrect argument\n";
    usage();
    exit(1);
}

# getting the input parameters
my $uid          = $ARGV[0];
my $userPassword = $ARGV[1];

# compute AES password
my $crypted = compute_password("$userPassword");

# compute SSHA password
my $hashed = password_hash("$userPassword");

# apply the two previous passwords to the specified $uid user in ldap directory
send_to_directory( "$uid", "$hashed", "$crypted" );

exit 0;
