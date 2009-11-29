#=================================================
# Specification file for LSC
#
# Install LSC connector
#
# Copyright (C) 2008 Clement OUDOT
#=================================================

#=================================================
# Variables
#=================================================
%define lscname		lsc-my-connector
%define lscversion	1.0
%define	lscdir		/usr/local/%{lscname}

#=================================================
# Header
#=================================================
Summary: LDAP Synchronization Connector
Name: %{lscname}
Version: %{lscversion}
Release: 1%{?dist}
License: BSD

Group: Applications/System
URL: http://www.lsc-project.org

Source: %{lscname}-%{lscversion}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

Prereq: coreutils
Requires: jdk

%description
The Ldap Synchronization Connector project provides tools to synchronize
a LDAP directory from a list of data sources including any database with
a JDBC connector, another LDAP directory, flat files... 

#=================================================
# Source preparation
#=================================================
%prep
%setup -n %{lscname}

#=================================================
# Installation
#=================================================
%install
rm -rf %{buildroot}

# Create directories
mkdir -p %{buildroot}%{lscdir}
mkdir -p %{buildroot}%{lscdir}/bin
mkdir -p %{buildroot}%{lscdir}/etc
mkdir -p %{buildroot}%{lscdir}/etc/resources
mkdir -p %{buildroot}%{lscdir}/lib
mkdir -p %{buildroot}/etc/cron.d
mkdir -p %{buildroot}/etc/logrotate.d

# Copy files
cp -a bin/*  %{buildroot}%{lscdir}/bin
cp -a etc/*  %{buildroot}%{lscdir}/etc
cp -a lib/*  %{buildroot}%{lscdir}/lib
cp -a etc/cron.d/*  %{buildroot}/etc/cron.d
cp -a etc/logrotate.d/*  %{buildroot}/etc/logrotate.d

#=================================================
# Cleaning
#=================================================
%clean
rm -rf %{buildroot}

#=================================================
# Files
#=================================================
%files
%defattr(-, root, root, 0755)
%config %{lscdir}/etc/
%config /etc/cron.d/
%config /etc/logrotate.d/
%{lscdir}/bin
%{lscdir}/lib

#=================================================
# Changelog
#=================================================
%changelog
* Tue Mar 31 2009 - Clement Oudot <clem@lsc-project.org> - 1.0-1
- First version of My Connector RPM
