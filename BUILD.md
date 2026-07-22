# Build distribution

## Tarball

```
$ mvn clean package
```

`.tar.gz` and `.zip` files available in `target/` directory.

## RPM

Configure `~/.rpmmacros`:
```
%_topdir /home/builder/rpmbuild
%dist .el8
%distribution .el8
%_signature gpg
%_gpg_name 876FCB011B5BABF4
%_gpgbin /usr/bin/gpg
%packager LSC Project Security <security@lsc-project.org>
%vendor LSC Project
```

Copy lsc-VERSION.zip in `SOURCES/` and lsc.spec in `SPECS/` and run:
```
$ rpmbuild -ba SPECS/lsc.spec
```

Sign packages:
```
$ rpmsign --addsign RPMS/noarch/*
```

## DEBIAN

Copy debian/ directory in dist directory:
```
$ cp -a src/install/debian/ target/lsc-core-VERSION-dist/lsc-VERSION/
```

Build packages:
```
$ cd target/lsc-core-VERSION-dist/lsc-VERSION/
$ dpkg-buildpackage
```

Sign packages:
```
$ debsigs --sign=origin -k 'LSC Project Security' *.deb
```

