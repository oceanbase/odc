#!/bin/bash
# Usage: build odc docker image
# Author: yh263208

cd $(dirname "$0") || exit 1

function get_cpu_arch() {
    local cpu_arch=$(uname -m)
    case "$cpu_arch" in
    x86*)
        cpu_arch="amd64"
        ;;
    aarch*)
        cpu_arch="arm64"
        ;;
    *)
        cpu_arch="unknown"
        ;;
    esac
    echo "${cpu_arch}"
}

function get_odc_version() {
    local odc_version=$(cat "../odc-server-VER.txt" | sed 's/[ \s\t\n\r]*//g')
    echo "${odc_version}"
}

main() {

    local register="docker.io"
    local namespace="oceanbase"
    local app_name=odc
    local image_name=${register}/${namespace}/${app_name}
    local image_tag=${2:-$(get_odc_version)}
    local cpu_arch=$(get_cpu_arch)

    echo "image_name=${image_name}, image_tag=${image_tag}, cpu_arch=${cpu_arch}"

    case $1 in
    build-odc)
        if [ ! -e resources/*.rpm ]; then
            echo "There is no rpm packages in \"resources\""
            echo "run \"../../script/build_rpm.sh\" to create rpm and copy to resources/"
            exit 1
        fi
        docker build --build-arg "ARCH=${cpu_arch}" -t ${image_name}:${image_tag} -f odc/Dockerfile .
        ;;
    tag)
        docker tag ${image_name}:${image_tag} ${image_name}:latest
        ;;
    login)
        local username=${2}
        if [ ${#username} == 0 ]; then
            echo "username is necessary"
            exit 1
        fi
        docker login --username=${username} ${register}
        ;;
    save)
        echo "save docker image to resources/odc-${image_tag}-${cpu_arch}.tar.gz"
        docker save ${image_name}:${image_tag} |
            gzip > resources/odc-${image_tag}-${cpu_arch}.tar.gz
        ;;
    push)
        docker tag ${image_name}:${image_tag} ${image_name}:latest
        docker push ${image_name}:${image_tag}
        docker push ${image_name}:latest
        ;;
    *)
        echo "Usage: build.sh build-odc [ <IMAGE_TAG> ]"
        echo "       build.sh login <USERNAME>"
        echo "       build.sh push [ <IMAGE_TAG> ]"
        ;;
    esac
}

main $*
