#!/usr/bin/env bash
# update git submodule

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

update_submodule
exit $?
