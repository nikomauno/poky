SUMMARY = "Next-generation RPM package management system"
DESCRIPTION = "DNF5 is a command-line package manager that automates the \
process of installing, upgrading, configuring, and removing computer programs \
in a consistent manner. It supports RPM packages, modulemd modules, and comps \
groups and environments."
HOMEPAGE = "https://github.com/rpm-software-management/dnf5"
LICENSE = "GPL-2.0-or-later | LGPL-2.1-or-later"
LIC_FILES_CHKSUM = "file://COPYING.md;md5=9733192df318d0f806fd668b92ba0ba6 \
                    file://gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
                    file://lgpl-2.1.txt;md5=4b54a1fd55a448865a0b32d41598759d \
                    "

SRC_URI = "\
    git://github.com/rpm-software-management/dnf5.git;branch=main;protocol=https \
    file://0001-Mitigate-pre-gcc-13-compile-errors.patch \
"
SRC_URI_OLD = "\
           file://0001-Corretly-install-tmpfiles.d-configuration.patch \
           file://0001-Do-not-hardcode-etc-and-systemd-unit-directories.patch \
           file://0005-Do-not-prepend-installroot-to-logdir.patch \
           file://0029-Do-not-set-PYTHON_INSTALL_DIR-by-running-python.patch \
           file://0030-Run-python-scripts-using-env.patch \
           file://0001-set-python-path-for-completion_helper.patch \
           file://0001-lock.py-fix-Exception-handling.patch \
           "

SRC_URI_OLD:append:class-native = " file://0001-dnf-write-the-log-lock-to-root.patch"

SRCREV = "4c3e4c3a46d1c5a5f0c1ead95c81d7410158b4a3"
UPSTREAM_CHECK_GITTAGREGEX = "(?P<pver>\d+(\.\d+)+)"

inherit cmake pkgconfig setuptools3-base systemd

#BUILD_CXXFLAGS:append = " -std=gnu++20"
#inherit cmake gettext bash-completion setuptools3-base systemd

#EXTRA_OECMAKE = " -DWITH_MAN=0 -DPYTHON_INSTALL_DIR=${PYTHON_SITEPACKAGES_DIR} -DPYTHON_DESIRED=3"

# manpages generation requires http://www.sphinx-doc.org/
#EXTRA_OECMAKE = " -DWITH_MAN=0 -DPYTHON_INSTALL_DIR=${PYTHON_SITEPACKAGES_DIR} -DPYTHON_DESIRED=3"
#EXTRA_OECMAKE

#PACKAGECONFIG ?= ""
#PACKAGECONFIG[perl] = ",-DCMAKE_DISABLE_FIND_PACKAGE_ICU=True,icu"
EXTRA_OECMAKE = "\
    -DWITH_PLUGIN_APPSTREAM=off \
    -DWITH_PERL5=off \
    -DENABLE_SOLV_FOCUSNEW=off \
    -DWITH_DNF5_OBSOLETES_DNF=off \
    -DWITH_HTML=off \
    -DWITH_MAN=off \
    -DWITH_TESTS=off \
    -DWITH_TRANSLATIONS=off \
    -DWITH_RUBY=off \
"
#"
EXTRA_OECMAKE:append:class-native = " -DWITH_SYSTEMD=off -DWITH_TESTS=off"

#EXTRA_OECMAKE:append = "\
#    -DWITH_COMPS=off \
#"
#
#    appstream \
#
#
# todo: PACKAGECONFIG options EXTRA_OECMAKE<->DEPENDS mappings ?
#    glibc-utils \
#
DEPENDS = "\
    fmt \
    json-c \
    libmodulemd \
    librepo \
    libsolv \
    libtoml11 \
    libxml2 \
    ruby \
    sdbus-c++ \
    swig-native \
    util-linux \
"

DEPENDS:append = " libdnf librepo libcomps"

#
DEPENDS:append:class-target = "\
    systemd \
"

#inherit toolchain/gcc-native
#DEPENDS:append:class-native = " gcc-cross"
#    cppunit \
#
#CXXFLAGS:append = " -std=gnu++17"
#smartcols"
######################
BBCLASSEXTEND = "native nativesdk"

RDEPENDS:${PN} += " \
  python3-core \
  python3-codecs \
  python3-netclient \
  python3-email \
  python3-threading \
  python3-logging \
  python3-fcntl \
  librepo \
  python3-shell \
  libcomps \
  libdnf \
  python3-sqlite3 \
  python3-compression \
  python3-rpm \
  python3-json \
  python3-curses \
  python3-misc \
  "

RDEPENDS:${PN}:class-native = ""

RRECOMMENDS:${PN}:class-target += "gnupg"

# Create a symlink called 'dnf' as 'make install' does not do it, but
# .spec file in dnf source tree does (and then Fedora and dnf documentation
# says that dnf binary is plain 'dnf').
do_install:append:false() {
        ln -rs ${D}/${bindir}/dnf5 ${D}/${bindir}/dnf
        #ln -rs ${D}/${bindir}/dnf-automatic-3 ${D}/${bindir}/dnf-automatic
}

# Direct dnf-native to read rpm configuration from our sysroot, not the one it was compiled in
do_install:append:class-native() {
        create_wrapper ${D}/${bindir}/dnf5 \
                SEQUOIA_CRYPTO_POLICY=${STAGING_DATADIR_NATIVE}/crypto-policies/back-ends/rpm-sequoia.config \
                RPM_CONFIGDIR=${STAGING_LIBDIR_NATIVE}/rpm \
                RPM_NO_CHROOT_FOR_SCRIPTS=1
}

do_install:append:class-nativesdk() {
        create_wrapper ${D}/${bindir}/dnf5 \
                RPM_CONFIGDIR=${SDKPATHNATIVE}${libdir_nativesdk}/rpm \
                RPM_NO_CHROOT_FOR_SCRIPTS=1
}

SYSTEMD_SERVICE:${PN} = "dnf-automatic.service \
                         dnf5-automatic.service \
                         dnf5-makecache.service \
                         dnf5-offline-transaction.service \
                         dnf5-offline-transaction-cleanup.service \
                         dnf5daemon-server.service \
"

FILES:${PN} += "${datadir} ${libdir}"

SYSTEMD_AUTO_ENABLE ?= "disable"

SKIP_RECIPE[dnf] ?= "${@bb.utils.contains('PACKAGE_CLASSES', 'package_rpm', '', 'does not build without package_rpm in PACKAGE_CLASSES due disabled rpm support in libsolv', d)}"

# Packages for testing purposes
PACKAGES += "${PN}-test-main ${PN}-test-dep"
ALLOW_EMPTY:${PN}-test-main = "1"
ALLOW_EMPTY:${PN}-test-dep = "1"
RRECOMMENDS:${PN}-test-main = "${PN}-test-dep"
