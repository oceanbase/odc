#!/usr/bin/env bash
# build rpm without sql console
# Usage: build_rpm_without_sqlconsole.sh <rpm_release:1>

# read parameters
rpm_release=${1:-"1"}

# read environment variables
fetch_from_oss_flag=${FETCH_FROM_OSS:-"0"}

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

function build_rpm_without_sqlconsole() {
    log_info "maven build jar start"
    if ! maven_build_jar; then
        log_error "maven build jar failed"
        return 4
    fi
    log_info "maven build jar done"

    if [ "${fetch_from_oss_flag}" == "1" ]; then
        log_info "oss fetch obclient start"
        if ! oss_fetch_obclient; then
            log_error "oss fetch obclient failed"
            return 5
        fi
        log_info "oss fetch obclient done"
    else
        if ! copy_obclient; then
            log_error "copy obclient.tar.gz to import folder failed"
            return 5
        fi
    fi

    log_info "maven build rpm start"
    if ! maven_build_rpm "${rpm_release}"; then
        log_error "maven build rpm failed"
        return 5
    fi
    log_info "maven build rpm done"

    log_info "copy rpm resources start"
    if ! copy_rpm_resources; then
        log_error "copy rpm resources failed"
        return 6
    fi
    log_info "copy rpm resources done"

    log_info "build rpm package completed."
}

build_rpm_without_sqlconsole
exit $?
