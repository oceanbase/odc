#!/usr/bin/env bash
# build libs only, only required in dev stage, after there exists changes in libs

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

if ! maven_install_libs; then
    echo "maven install libs failed"
    exit 2
fi

echo "maven install libs succeed"

exit 0
