#=================================================
# Specification file for LSC-project RPM
#
# Install LSC
#
# BSD License
#
# Copyright (C) 2010 Clement OUDOT
# Copyright (C) 2010 LSC-project
#=================================================

#=================================================
# Variables
#=================================================
%define lsc_name	lsc
%define lsc_version	1.2.1
%define lsc_logdir      /var/log/lsc
%define lsc_user        lsc
%define lsc_group       lsc

#=================================================
# Header
#=================================================
Summary: LDAP Synchronization Connector
Name: %{lsc_name}
Version: %{lsc_version}
Release: 0%{?dist}
License: BSD
BuildArch: noarch

Group: Applications/System
URL: http://lsc-project.org

Source: %{lsc_name}-core-%{lsc_version}-src.tar.gz
Source1: lsc.cron
Source2: lsc.logrotate
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

Prereq: coreutils
Requires: jdk
# Warning: build requires maven

%description
The Ldap Synchronization Connector project provides tools to synchronize
a LDAP directory from a list of data sources including any database with
a JDBC connector, another LDAP directory, flat files... 

#=================================================
# Source preparation
#=================================================
%prep
%setup -n  %{lsc_name}-%{lsc_version}-src

#=================================================
# Build
#=================================================
%build
export LANG=en_US.UTF-8
mvn package

#=================================================
# Installation
#=================================================
%install
rm -rf %{buildroot}

# Change to target direct
cd target/%{lsc_name}-core-%{lsc_version}-dist/%{lsc_name}-%{lsc_version}/

# Create directories
mkdir -p %{buildroot}/usr/bin
mkdir -p %{buildroot}/usr/%{_lib}/lsc
mkdir -p %{buildroot}/etc/lsc
mkdir -p %{buildroot}/etc/lsc/sql-map-config.d
mkdir -p %{buildroot}/etc/cron.d
mkdir -p %{buildroot}/etc/logrotate.d
mkdir -p %{buildroot}/usr/share/doc/lsc
mkdir -p %{buildroot}%{lsc_logdir}

# Copy files
## bin
cp -a bin/lsc %{buildroot}/usr/bin
## config
cp -a etc/logback.xml %{buildroot}/etc/lsc
cp -a etc/lsc.properties-sample %{buildroot}/etc/lsc/lsc.properties
cp -a etc/sql-map-config.xml-sample %{buildroot}/etc/lsc/sql-map-config.xml
## lib
cp -a lib/* %{buildroot}/usr/%{_lib}/lsc
## sample
cp -a sample/ %{buildroot}/usr/share/doc/lsc
## cron && logrotate
install -m 644 %{SOURCE1} %{buildroot}/etc/cron.d/lsc
install -m 644 %{SOURCE2} %{buildroot}/etc/logrotate.d/lsc

# Reconfigure files
## logback
sed -i 's:/tmp/lsc.log:%{lsc_logdir}/lsc.log:' %{buildroot}/etc/lsc/logback.xml
## logrotate
sed -i 's:/tmp/lsc.log:%{lsc_logdir}/lsc.log:' %{buildroot}/etc/logrotate.d/lsc
sed -i 's:root root:%{lsc_user} %{lsc_group}:' %{buildroot}/etc/logrotate.d/lsc
## cron
sed -i 's: root : %{lsc_user} :' %{buildroot}/etc/cron.d/lsc
## bin
sed -i 's:^CFG_DIR.*:CFG_DIR="/etc/lsc":' %{buildroot}/usr/bin/lsc
sed -i 's:^LIB_DIR.*:LIB_DIR="/usr/%{_lib}/lsc":' %{buildroot}/usr/bin/lsc
sed -i 's:^LOG_DIR.*:LOG_DIR="%{lsc_logdir}":' %{buildroot}/usr/bin/lsc

%post
#=================================================
# Post Installation
#=================================================

# Do this at first install
if [ $1 -eq 1 ]
then
        # Create user and group
        /usr/sbin/groupadd %{lsc_group}
        /usr/sbin/useradd %{lsc_user} -g %{lsc_group}
fi

# Always do this
# Change owner
/bin/chown -R %{lsc_user}:%{lsc_group} /usr/bin/lsc
/bin/chown -R %{lsc_user}:%{lsc_group} /usr/%{_lib}/lsc
/bin/chown -R %{lsc_user}:%{lsc_group} %{lsc_logdir}

# Add symlink for sample to work
ln -sf /usr/%{_lib}/lsc /usr/share/doc/lsc/%{_lib}

%postun
#=================================================
# Post uninstallation
#=================================================

# Don't do this if newer version is installed
if [ $1 -eq 0 ]
then
        # Delete user and group
        /usr/sbin/userdel -r %{lsc_user}
fi

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
%config(noreplace) /etc/lsc/
%config(noreplace) /etc/cron.d/lsc
%config(noreplace) /etc/logrotate.d/lsc
/usr/bin/lsc
/usr/%{_lib}/lsc/
/usr/share/doc/lsc
%{lsc_logdir}

#=================================================
# Changelog
#=================================================
%changelog
* Sun Jul 18 2010 - Clement Oudot <clem@lsc-project.org> - 1.2.1-0
- Upgrade to LSC 1.2.1
- Build package from source
* Thu May 25 2010 - Clement Oudot <clem@lsc-project.org> - 1.2.0-0
- First package for LSC
