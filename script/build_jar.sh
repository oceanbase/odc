#!/usr/bin/env bash
# build jar only, for backend debug scenario
# will copy executable jar to ${ODC_DIR}/lib, so start-odc.sh is able to find jar

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

if ! maven_install_libs; then
    echo "maven install libs failed"
    exit 2
fi

if ! maven_build_jar; then
    echo "maven build jar failed"
    exit 3
fi

echo "maven build jar success, copy executable jar to ${ODC_DIR}/lib for use script/start-odc.sh locally."

mkdir -p "${ODC_DIR}/"{lib,conf}
rm --force --verbose "${ODC_DIR}"/lib/*.jar
cp -fv "${ODC_DIR}"/server/odc-server/target/odc-*-executable.jar "${ODC_DIR}"/lib/
cp -fv "${ODC_DIR}"/server/odc-server/target/classes/log4j2.xml "${ODC_DIR}"/conf/
exit $?
