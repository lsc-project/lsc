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
%define lsc_name	lsc
%define lsc_version	2.2.0
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
URL: https://lsc-project.org

Source: %{lsc_name}-core-%{lsc_version}-dist.zip
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

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
%setup -n  %{lsc_name}-%{lsc_version}

#=================================================
# Build
#=================================================
%build

#=================================================
# Installation
#=================================================
%install
rm -rf %{buildroot}

# Create directories
mkdir -p %{buildroot}/usr/bin
mkdir -p %{buildroot}/usr/%{_lib}/lsc
mkdir -p %{buildroot}/etc/lsc
mkdir -p %{buildroot}/etc/lsc/sql-map-config.d
mkdir -p %{buildroot}/etc/cron.d
mkdir -p %{buildroot}/etc/init.d
mkdir -p %{buildroot}/etc/default
mkdir -p %{buildroot}/usr/share/doc/lsc/bin
mkdir -p %{buildroot}%{lsc_logdir}
mkdir -p %{buildroot}/var/lib/lsc/nagios

# Copy files
## bin
cp -a bin/lsc %{buildroot}/usr/bin
cp -a bin/lsc-agent %{buildroot}/usr/bin
cp -a bin/hsqldb %{buildroot}/usr/bin
## config
cp -a etc/logback.xml %{buildroot}/etc/lsc
cp -a etc/lsc.xml-sample %{buildroot}/etc/lsc/lsc.xml
cp -a etc/lsc.xml-sample %{buildroot}/usr/share/doc/lsc/
cp -a etc/sql-map-config.xml-sample %{buildroot}/etc/lsc/sql-map-config.xml
cp -a etc/sql-map-config.xml-sample %{buildroot}/usr/share/doc/lsc/
cp -a etc/sql-map-config.d/InetOrgPerson.xml-sample %{buildroot}/etc/lsc/sql-map-config.d/InetOrgPerson.xml
cp -a etc/sql-map-config.d/InetOrgPerson.xml-sample %{buildroot}/usr/share/doc/lsc/
## lib
cp -a lib/* %{buildroot}/usr/%{_lib}/lsc
## sample
cp -a sample/ %{buildroot}/usr/share/doc/lsc
## cron
cp -a etc/cron.d/lsc.cron %{buildroot}/etc/cron.d/lsc
## init
cp -a etc/init.d/lsc %{buildroot}/etc/init.d/lsc
cp -a etc/default/lsc %{buildroot}/etc/default/lsc
## nagios
cp -a bin/check_lsc* %{buildroot}/var/lib/lsc/nagios

# Reconfigure files
## logback
sed -i 's:/tmp/lsc/log:%{lsc_logdir}:' %{buildroot}/etc/lsc/logback.xml
## cron
sed -i 's: root : %{lsc_user} :' %{buildroot}/etc/cron.d/lsc
sed -i 's:#LSC_BIN#:/usr/bin/lsc:g' %{buildroot}/etc/cron.d/lsc
sed -i 's:^30:#30:' %{buildroot}/etc/cron.d/lsc
## bin
sed -i 's:^CFG_DIR.*:CFG_DIR="/etc/lsc":' %{buildroot}/usr/bin/lsc %{buildroot}/usr/bin/lsc-agent %{buildroot}/usr/bin/hsqldb
sed -i 's:^LIB_DIR.*:LIB_DIR="/usr/%{_lib}/lsc":' %{buildroot}/usr/bin/lsc %{buildroot}/usr/bin/lsc-agent %{buildroot}/usr/bin/hsqldb
sed -i 's:^LOG_DIR.*:LOG_DIR="%{lsc_logdir}":' %{buildroot}/usr/bin/lsc %{buildroot}/usr/bin/lsc-agent %{buildroot}/usr/bin/hsqldb
sed -i 's:^VAR_DIR.*:VAR_DIR="/var/lsc":' %{buildroot}/usr/bin/hsqldb
## init
sed -i 's:^LSC_BIN.*:LSC_BIN="/usr/bin/lsc":' %{buildroot}/etc/default/lsc
sed -i 's:^LSC_CFG_DIR.*:LSC_CFG_DIR="/etc/lsc":' %{buildroot}/etc/default/lsc
sed -i 's:^LSC_USER.*:LSC_USER="lsc":' %{buildroot}/etc/default/lsc
sed -i 's:^LSC_GROUP.*:LSC_GROUP="lsc":' %{buildroot}/etc/default/lsc
sed -i 's:^LSC_PID_FILE.*:LSC_PID_FILE="/var/run/lsc.pid":' %{buildroot}/etc/default/lsc

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
getent passwd %{lsc_user} > /dev/null 2>&1 || useradd --system --gid %{lsc_group} --home-dir /etc/lsc %{lsc_user}
# Change owner
/bin/chown -R %{lsc_user}:%{lsc_group} %{lsc_logdir}

# Add symlink for sample to work
ln -sf /usr/%{_lib}/lsc/ /usr/share/doc/lsc/%{_lib}
ln -sf /usr/bin/lsc /usr/share/doc/lsc/bin/

%postun
#=================================================
# Post uninstallation
#=================================================

# Don't do this if newer version is installed
if [ $1 -eq 0 ]
then
	# Remove sample symlinks
	rm -rf /usr/share/doc/lsc/%{_lib}
	rm -rf /usr/share/doc/lsc/bin/

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
%config(noreplace) /etc/default/lsc
/usr/bin/lsc
/usr/bin/lsc-agent
/usr/bin/hsqldb
/etc/init.d/lsc
/usr/%{_lib}/lsc/
/usr/share/doc/lsc
%{lsc_logdir}
/var/lib/lsc/nagios

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
