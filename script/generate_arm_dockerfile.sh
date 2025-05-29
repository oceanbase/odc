#!/usr/bin/env bash
# for generate arm arch Dockerfile based on x86_x64 Dockerfile
# Usage: generate_arm_dockerfile.sh <source_x86_dockerfile> <target_arm_dockerfile>
# - source_x86_dockerfile: default ${ODC_DIR}/distribution/docker/odc/Dockerfile if not set
# - target_arm_dockerfile: default ${ODC_DIR}/distribution/docker/odc/Dockerfile.arm if not set

if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

source_x86_dockerfile=${1:-"${ODC_DIR}/distribution/docker/odc/Dockerfile"}
target_arm_dockerfile=${2:-"${ODC_DIR}/distribution/docker/odc/Dockerfile.arm"}

#############################################
# generate arm arch Dockerfile based on x86_x64 Dockerfile
# PARAM $1: file name of source x86_x64 Dockerfile
# PARAM $2: file name of target arm Dockerfile
# RETURN:  0 if success, non-zero if failed
#############################################
function generate_arm_dockerfile() {
    if [ "$#" -ne 2 ]; then
        echo "Usage: $0 <source_x86_dockerfile> <target_arm_dockerfile>" >&2
        exit 1
    fi
    if ! [ -e "$1" ]; then
        echo "source file [$1] not found" >&2
        exit 2
    fi
    if [ -z "$2" ]; then
        echo "target file cannot be empty" >&2
        exit 3
    fi

    local source_x86_dockerfile="$1"
    local target_arm_dockerfile="$2"

    rm --force --verbose ${target_arm_dockerfile}

    # generate solution: replace base image
    # to: reg.docker.alibaba-inc.com/openanolis/anolisos:8.4-aarch64
    sed 's/FROM reg.docker.alibaba-inc.com\/openanolis\/anolisos:8.4-x86_64/FROM reg.docker.alibaba-inc.com\/openanolis\/anolisos:8.4-aarch64/' \
        ${source_x86_dockerfile} >${target_arm_dockerfile}

    return $?
}

generate_arm_dockerfile "$source_x86_dockerfile" "$target_arm_dockerfile"
exit $?
