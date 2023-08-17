#!/usr/bin/env bash
# init node env

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

init_node_env
exit $?
