#!/usr/bin/env bash
# build rpm
# Usage: build_rpm.sh <rpm_release:1>
# Supported environment variables
# - SYNC_SUBMODULE, default 1, skip sync submodule if 0
# - BUILD_FRONTEND, default 1, skip build frontend if 0
# - FETCH_FROM_CDN, default 0, fetch sqlconsole from cdn if 1
# - FETCH_FROM_OSS, default 0, fetch obclient.tar.gz from oss if 1
# - BUILD_PROFILE, default <empty>, set to maven -P if not empty, e.g. apsara

# read parameters
rpm_release=${1:-"1"}

# read environment variables
sync_submodule_flag=${SYNC_SUBMODULE:-"1"}
build_frontend_flag=${BUILD_FRONTEND:-"1"}
fetch_from_cdn_flag=${FETCH_FROM_CDN:-"0"}
fetch_from_oss_flag=${FETCH_FROM_OSS:-"0"}
build_profile=${BUILD_PROFILE:-""}

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

function install_dependencies() {
    local package_list="rpm-build git"
    for package_name in $package_list; do
        rpm --quiet --query "$package_name" || yum install -y "$package_name"
    done
}

function build_rpm() {
    print_env_info

    log_info "build rpm package starting..."

    log_info "install dependencies start"
    install_dependencies
    log_info "install dependencies done"

    if [ "${sync_submodule_flag}" == "1" ]; then
        log_info "update submodule start"
        if ! update_submodule; then
            log_error "update submodule failed"
            return 1
        fi
        log_info "update submodule done"
    fi

    if [ "${build_frontend_flag}" == "1" ]; then
        log_info "build sqlconsole start"
        if ! build_sqlconsole; then
            log_error "build sqlconsole failed"
            return 3
        fi
        log_info "build sqlconsole done"
    fi

    if [ "${fetch_from_cdn_flag}" == "1" ]; then
        log_info "fetch sqlconsole from cdn start"
        if ! fetch_sqlconsole_from_cdn; then
            log_error "fetch sqlconsole from cdn failed"
            return 7
        fi
        log_info "fetch sqlconsole from cdn done"
    fi

    log_info "maven build jar start"
    local maven_extra_args=()
    if [ ! -z "$build_profile" ]; then
        maven_extra_args=("-P" ${build_profile[@]})
        log_info "maven_extra_args=${maven_extra_args[@]}"
    fi

    if ! maven_build_jar ${maven_extra_args[@]}; then
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
        return 6
    fi
    log_info "maven build rpm done"

    log_info "copy rpm resources start"
    if ! copy_rpm_resources; then
        log_error "copy rpm resources failed"
        return 7
    fi
    log_info "copy rpm resources done"

    log_info "build rpm package completed."
}

build_rpm
exit $?
