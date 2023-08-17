#!/usr/bin/env bash
# build sqlconsole (ODC frontend module)

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

build_sqlconsole
exit $?
