#!/usr/bin/env bash
# create slim jar by remove static resources from fat jar

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

if ! ls "${ODC_DIR}"/server/odc-server/target/odc-*-executable.jar; then
    echo "create slim jar failed, source fat jar not exists"
    exit 2
fi

source_file_name=$(ls "${ODC_DIR}"/server/odc-server/target/odc*executable.jar)
target_file_name="${source_file_name/executable/slim}"

cp -fv "${source_file_name}" "${target_file_name}"
zip -d "${target_file_name}" "BOOT-INF/classes/static/*"

echo "create slim jar success, file_name=${target_file_name}"

echo "all jars:"
ls -l "${ODC_DIR}"/server/odc-server/target/odc-*.jar

exit $?
