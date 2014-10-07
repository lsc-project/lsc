#!/usr/bin/perl -s -w

#====================================================================
# perl script computing AES crypted passwords and their SSHA hash,
# and pushing them to ldap directory
#
# INPUT : $1=user, $2=cleartext-password
# OUTPUT: 0 if everything is ok. (and computed crypted password is
# pushed to ldap directory)
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
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
# GPL License: http://www.gnu.org/licenses/gpl.txt

use strict;
use warnings;

use vars qw/ $h /;
use Crypt::Rijndael;
use MIME::Base64 qw( encode_base64 );
use Digest::SHA ("sha1");
use Unicode::Map8;
use Net::LDAP;

use Authen::Krb5;
use Authen::Krb5::Admin;

use Crypt::SmbHash qw(lmhash nthash);

#====================================================================
# Configuration
#====================================================================

# some crypto variables
# $keyfile is the file containing the key for AES encryption
my $keyfile = "/path/to/passwordhk.key";
my $blocksize = 128 / 8; #( = 16 bytes)
open( FILE, "$keyfile" ) || die("Error opening key");
my $key = <FILE>;
close(FILE);

# some ldap variables
my @ldapDirectories = (
 {
 ldapUrl => "host1.com",
 ldapAdmin => "cn=Manager,dc=my-domain,dc=com",
 ldapPassword => "secret",
 ldapBase => "dc=my-domain,dc=com",
 ldapFilter => '(&(objectClass=person)(uid=USER))',
 },
 {
 ldapUrl => "host2.com",
 ldapAdmin => "cn=Manager,dc=my-domain,dc=com",
 ldapPassword => "secret",
 ldapBase => "dc=my-domain,dc=com",
 ldapFilter => '(&(objectClass=person)(uid=USER))',
 },
);

# attribute used to store SSHA user password
my $userPasswordAttr = 'userPassword';

my $push_encrypted = 1; # push or not the encrypted password

# choose a valid ldap attribute to hold AES encrypted password
my $userPasswordEncryptedAttr = 'userPasswordEncrypted';

my $push_samba_lm = 1; # push or not the samba LM password
my $push_samba_nt = 1; # push or not the samba NT password

# some AD ldap variables
my $adUrl = "hostAD.com";
my $adPort = "636";
my $adScheme = "ldaps";
my $adAdmin =
"CN=Administrateur,CN=Users,DC=ad";
my $adPassword = 'secret';
my $adBase = "DC=ad";

# ldapFilter: USER is replaced by appropriate input
my $adFilter = '(&(objectClass=person)(uid=USER))';

# attribute used to store SSHA user password
my $adUserPasswordAttr = 'unicodePwd';

# Some Kerberos Domain
my $krbServer = "admin.kerberos.com";
my $krbDomain = "KERBEROS.COM";
my $krbAdmin = "role/user";
my $krbKeytab = "/path/to/user.keytab";

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
 1; # -1 +1 is used to permute the 0 value by 16
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

 foreach my $ldapdir (@ldapDirectories) {
 my $ldap = Net::LDAP->new( $ldapdir->{ldapUrl} );

 # bind to a directory with dn and password
 my $result =
 $ldap->bind( $ldapdir->{ldapAdmin},
 password => $ldapdir->{ldapPassword} );

 $ldapdir->{ldapFilter} =~ s/USER/$user/;
 $result = $ldap->search(
 base => $ldapdir->{ldapBase},
 scope => 'sub',
 filter => $ldapdir->{ldapFilter},
 );
 my @entries = $result->entries;

 # if entry is duplicated in directory, quits with error
 if ( scalar @entries > 1 ) {
 print "More than one entry found for " . $user;
 exit 1;
 }

 # if entry is not found in directory, gently quits
 exit 2 if scalar @entries == 0;

 my $dn = $entries[0]->dn;

 # Compute samba hashes
 my $lm = lmhash("$userPassword");
 my $nt = nthash("$userPassword");

 my $replace = { "$userPasswordAttr" => "$userPassword", };
 $replace->{$userPasswordEncryptedAttr} = "$userPasswordEncrypted"
 if ( $push_encrypted == 1 );
 $replace->{sambaLMPassword} = "$lm" if ( $push_samba_lm == 1 );
 $replace->{sambaNTPassword} = "$nt" if ( $push_samba_nt == 1 );

 # do the job : modify userPasswords
 $result = $ldap->modify( $dn, replace => $replace, );

 $result = $ldap->unbind; # take down session
 }

}

sub send_to_ad {
 my ( $user, $userPassword ) = @_;
 my $ldap = Net::LDAP->new(
 "$adUrl",
 scheme => "$adScheme",
 port => "$adPort"
 ) or die($@);

 # bind to a directory with dn and password
 my $result = $ldap->bind( "$adAdmin", password => "$adPassword" );

 $adFilter =~ s/USER/$user/;
 $result = $ldap->search(
 base => "$adBase",
 scope => 'sub',
 filter => "$adFilter",
 );
 my @entries = $result->entries;

 # if entry is duplicated in directory, quits with error
 if ( scalar @entries > 1 ) {
 print "More than one entry found for " . $user;
 exit 1;
 }

 # if entry is not found in directory, quits with error
 exit 2 if scalar @entries == 0;

 my $dn = $entries[0]->dn;

 # do the job : modify userPasswords
 my $charmap = Unicode::Map8->new('latin1') or die;
 my $encpwd =
 $charmap->tou( '"' . $userPassword . '"' )->byteswap()->utf16();
 $result =
 $ldap->modify( $dn, replace => { "$adUserPasswordAttr" => "$encpwd", }
);
 if ( $result->error ne "Success" ) {
 print "Unable to modify AD password: " . $result->error;
 exit(3);
 }

 $result = $ldap->unbind; # take down session

}

sub send_to_kdc {
 my ( $user, $userPassword ) = @_;
 Authen::Krb5::init_context();
 my $krbUser = $user . '@' . $krbDomain;
 my $kadm = Authen::Krb5::Admin->init_with_skey( $krbAdmin, $krbKeytab
);
 my $princ = Authen::Krb5::parse_name($krbUser)
 or die Authen::Krb5::Admin::error;
 my $rc = $kadm->chpass_principal( $princ, $userPassword )
 or die Authen::Krb5::Admin::error;

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
 $hash = "{SSHA}" . encode_base64( sha1( $password . $salt ) . $salt, ''
);

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
my $uid = $ARGV[0];
my $userPassword = $ARGV[1];

# compute AES password
my $crypted = compute_password("$userPassword");

# compute SSHA password
my $hashed = password_hash("$userPassword");

# apply the two previous passwords to the specified $uid user in ldap
# directory
send_to_directory( "$uid", "$hashed", "$crypted" );

# apply password to Active Directory
send_to_ad( "$uid", "$userPassword" );

send_to_kdc( "$uid", "$userPassword" );

exit 0; 
 

