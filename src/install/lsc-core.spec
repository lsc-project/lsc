#=================================================
# Specification file for LSC
#
# Install LSC connector
#
# Copyright (c) 2009 - 2011 LSC Project
# Improved by S. Bahloul
# Originally contributed by Clement OUDOT
# 
# TODO: Need to do a real installation 
# - Need to fix lsc.cron and lsc.logrotate paths
# and install it inside the target directories 
# (/etc/{cron.d,logrotate.d})
# - Change logs location to /var/log/lsc
# - Need to move binaries to /usr/bin, libraries to /usr/lib
# and configuration to /etc/lsc
# - Force strong dependency to Sun/ORACLE JDK
#=================================================

#=================================================
# Variables
#=================================================
%define lscname		lsc
%define lscversion	trunk-SNAPSHOT
%define	lscdir		/opt/%{lscname}-%{lscversion}

#=================================================
# Header
#=================================================
Summary: LDAP Synchronization Connector
Name: %{lscname}
Version: 2.0.SNAPSHOT
Release: 2%{?dist}
License: BSD

Group: Applications/System
URL: http://www.lsc-project.org

Source: %{lscname}-%{lscversion}-dist.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

Prereq: coreutils, libxslt
Requires: jdk >= 1.6.0

%description
The Ldap Synchronization Connector project provides tools to synchronize
a LDAP directory from a list of data sources including any database with
a JDBC connector, another LDAP directory, flat files... 

#=================================================
# Source preparation
#=================================================
%prep
%setup -n %{lscname}-%{lscversion}

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

#==========================%======================
# Files
#=================================================
%files
%defattr(-, root, root, 0700)
%config %{lscdir}/etc/
%config /etc/cron.d/
%config /etc/logrotate.d/
%defattr(-, root, root, 0755)
%{lscdir}/bin
%defattr(-, root, root, 0644)
%{lscdir}/lib

#=================================================
# Changelog
#=================================================
%changelog
* Mon Aug 23 2011 - Sebastien Bahloul <sebastien@lsc-project.org> - 2.0-2
- Update the package for 2.0.x version
* Tue Mar 31 2009 - Clement Oudot <clem@lsc-project.org> - 1.0-1
- First version of My Connector RPM
