#=================================================
# Specification file for LSC-project RPM
#
# Install LSC
#
# BSD License
#
# Copyright (c) 2009 - 2012 LSC Project
#=================================================

#=================================================
# Variables
#=================================================
%define lsc_logdir      %{_localstatedir}/log/lsc
%define lsc_user        lsc
%define lsc_group       lsc

#=================================================
# Header
#=================================================
Summary: LDAP Synchronization Connector
Name: lsc
Version: 2.2.0
Release: 0%{?dist}
License: BSD-3-Clause
BuildArch: noarch

URL: https://lsc-project.org

Source: https://lsc-project.org/archives/%{name}-core-%{version}-dist.tar.gz

Requires(pre): coreutils
Requires: which

%description
The Ldap Synchronization Connector project provides tools to synchronize
a LDAP directory from a list of data sources including any database with
a JDBC connector, another LDAP directory, flat files... 

#=================================================
# Source preparation
#=================================================
%prep
%setup -n  %{name}-%{version}

#=================================================
# Build
#=================================================
%build

#=================================================
# Installation
#=================================================
%install
# Create directories
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}%{_libdir}/lsc
mkdir -p %{buildroot}%{_sysconfdir}/lsc
mkdir -p %{buildroot}%{_sysconfdir}/lsc/sql-map-config.d
mkdir -p %{buildroot}%{_sysconfdir}/cron.d
mkdir -p %{buildroot}%{_initddir}
mkdir -p %{buildroot}%{_sysconfdir}/default
mkdir -p %{buildroot}%{_docdir}/lsc/bin
mkdir -p %{buildroot}%{lsc_logdir}
mkdir -p %{buildroot}%{_sharedstatedir}/lsc/nagios

# Copy files
## bin
cp -a bin/lsc %{buildroot}%{_bindir}
cp -a bin/lsc-agent %{buildroot}%{_bindir}
cp -a bin/hsqldb %{buildroot}%{_bindir}
## config
cp -a etc/logback.xml %{buildroot}%{_sysconfdir}/lsc
cp -a etc/lsc.xml-sample %{buildroot}%{_sysconfdir}/lsc/lsc.xml
cp -a etc/lsc.xml-sample %{buildroot}%{_docdir}/lsc/
cp -a etc/sql-map-config.xml-sample %{buildroot}%{_sysconfdir}/lsc/sql-map-config.xml
cp -a etc/sql-map-config.xml-sample %{buildroot}%{_docdir}/lsc/
cp -a etc/sql-map-config.d/InetOrgPerson.xml-sample %{buildroot}%{_sysconfdir}/lsc/sql-map-config.d/InetOrgPerson.xml
cp -a etc/sql-map-config.d/InetOrgPerson.xml-sample %{buildroot}%{_docdir}/lsc/
## lib
cp -a lib/* %{buildroot}%{_libdir}/lsc
## sample
cp -a sample/ %{buildroot}%{_docdir}/lsc
## cron
cp -a etc/cron.d/lsc.cron %{buildroot}%{_sysconfdir}/cron.d/lsc
## init
cp -a etc/init.d/lsc %{buildroot}%{_initddir}/lsc
cp -a etc/default/lsc %{buildroot}%{_sysconfdir}/default/lsc
## nagios
cp -a bin/check_lsc* %{buildroot}%{_sharedstatedir}/lsc/nagios

# Reconfigure files
## logback
sed -i 's:/tmp/lsc/log:%{lsc_logdir}:' %{buildroot}%{_sysconfdir}/lsc/logback.xml
## cron
sed -i 's: root : %{lsc_user} :' %{buildroot}%{_sysconfdir}/cron.d/lsc
sed -i 's:#LSC_BIN#:%{_bindir}/lsc:g' %{buildroot}%{_sysconfdir}/cron.d/lsc
sed -i 's:^30:#30:' %{buildroot}%{_sysconfdir}/cron.d/lsc
## bin
sed -i 's:^CFG_DIR.*:CFG_DIR="%{_sysconfdir}/lsc":' %{buildroot}%{_bindir}/lsc %{buildroot}%{_bindir}/lsc-agent %{buildroot}%{_bindir}/hsqldb
sed -i 's:^LIB_DIR.*:LIB_DIR="%{_libdir}/lsc":' %{buildroot}%{_bindir}/lsc %{buildroot}%{_bindir}/lsc-agent %{buildroot}%{_bindir}/hsqldb
sed -i 's:^LOG_DIR.*:LOG_DIR="%{lsc_logdir}":' %{buildroot}%{_bindir}/lsc %{buildroot}%{_bindir}/lsc-agent %{buildroot}%{_bindir}/hsqldb
sed -i 's:^VAR_DIR.*:VAR_DIR="/var/lsc":' %{buildroot}%{_bindir}/hsqldb
## init
sed -i 's:^LSC_BIN.*:LSC_BIN="%{_bindir}/lsc":' %{buildroot}%{_sysconfdir}/default/lsc
sed -i 's:^LSC_CFG_DIR.*:LSC_CFG_DIR="%{_sysconfdir}/lsc":' %{buildroot}%{_sysconfdir}/default/lsc
sed -i 's:^LSC_USER.*:LSC_USER="lsc":' %{buildroot}%{_sysconfdir}/default/lsc
sed -i 's:^LSC_GROUP.*:LSC_GROUP="lsc":' %{buildroot}%{_sysconfdir}/default/lsc
sed -i 's:^LSC_PID_FILE.*:LSC_PID_FILE="/var/run/lsc.pid":' %{buildroot}%{_sysconfdir}/default/lsc

%post
#=================================================
# Post Installation
#=================================================

# Do this at first install
if [ $1 -eq 1 ]
then
        # Set lsc as service
        /sbin/chkconfig --add lsc
fi

# Always do this
# Create user and group if needed
getent group %{lsc_group} > /dev/null 2>&1 || groupadd --system %{lsc_group}
getent passwd %{lsc_user} > /dev/null 2>&1 || useradd --system --gid %{lsc_group} --home-dir %{_sysconfdir}/lsc %{lsc_user}
# Change owner
/bin/chown -R %{lsc_user}:%{lsc_group} %{lsc_logdir}

# Add symlink for sample to work
ln -sf %{_libdir}/lsc/ %{_docdir}/lsc/%{_lib}
ln -sf %{_bindir}/lsc %{_docdir}/lsc/bin/

%postun
#=================================================
# Post uninstallation
#=================================================

# Don't do this if newer version is installed
if [ $1 -eq 0 ]
then
	# Remove sample symlinks
	rm -rf %{_docdir}/lsc/%{_lib}
	rm -rf %{_docdir}/lsc/bin/

        # Delete user and group
        /usr/sbin/userdel -r %{lsc_user}
fi

#=================================================
# Files
#=================================================
%files
%license LICENSE.txt
%config(noreplace) %{_sysconfdir}/lsc/
%config(noreplace) %{_sysconfdir}/cron.d/lsc
%config(noreplace) %{_sysconfdir}/default/lsc
%{_bindir}/lsc
%{_bindir}/lsc-agent
%{_bindir}/hsqldb
%{_initddir}/lsc
%{_libdir}/lsc/
%{_docdir}/lsc
%{lsc_logdir}
%{_sharedstatedir}/lsc/

#=================================================
# Changelog
#=================================================
%changelog
* Tue Aug 24 2021 - Clement Oudot <clem@lsc-project.org> - 2.1.6-1
- Upgrade to LSC 2.1.6
* Thu Feb 20 2020 - Clement Oudot <clem@lsc-project.org> - 2.1.5-1
- Upgrade to LSC 2.1.5
* Sat Mar 25 2017 - Clement Oudot <clem@lsc-project.org> - 2.1.4-0
- Upgrade to LSC 2.1.4
* Tue Mar 03 2015 - Clement Oudot <clem@lsc-project.org> - 2.1.3-0
- Upgrade to LSC 2.1.3
* Fri Dec 19 2014 - Clement Oudot <clem@lsc-project.org> - 2.1.2-0
- Upgrade to LSC 2.1.2
* Fri Jul 25 2014 - Clement Oudot <clem@lsc-project.org> - 2.1.1-0
- Upgrade to LSC 2.1.1
* Fri Apr 25 2014 - Clement Oudot <clem@lsc-project.org> - 2.1.0-0
- Upgrade to LSC 2.1.0
* Thu Mar 06 2014 - Clement Oudot <clem@lsc-project.org> - 2.0.4-0
- Upgrade to LSC 2.0.4
* Fri Sep 13 2013 - Clement Oudot <clem@lsc-project.org> - 2.0.3-0
- Upgrade to LSC 2.0.3
* Fri Mar 22 2013 - Clement Oudot <clem@lsc-project.org> - 2.0.2-0
- Upgrade to LSC 2.0.2
* Thu Oct 11 2012 - Clement Oudot <clem@lsc-project.org> - 2.0.1-0
- Upgrade to LSC 2.0.1
* Mon Apr 02 2012 - Clement Oudot <clem@lsc-project.org> - 2.0-0
- Upgrade to LSC 2.0
* Thu Feb 09 2012 - Clement Oudot <clem@lsc-project.org> - 1.2.2-0
- Upgrade to LSC 1.2.2
- Change ownership of configuration files (#396)
- Add symlink for sample (#302)
* Sun Jul 18 2010 - Clement Oudot <clem@lsc-project.org> - 1.2.1-0
- Upgrade to LSC 1.2.1
- Build package from source
* Thu May 25 2010 - Clement Oudot <clem@lsc-project.org> - 1.2.0-0
- First package for LSC
