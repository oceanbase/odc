#!/bin/bash
# Usage: build odc-build docker image for x86 or aarch

# To build odc-build-arm image, building process should be under aarch architecture
# attention odc-build docker image use separated version
IMAGE_VERSION=${2:-0.2}

main() {
    local register="docker.io"
    local namespace="oceanbase"
    local app_name=odc-build
    local default_latest_tag=latest
    local docker_file_path=Dockerfile
    if [ $(uname -m) = "aarch64" ]; then
        app_name=odc-build-arm
        default_latest_tag=latest_aarch64
        docker_file_path=aarch/Dockerfile
    fi
    local image_name=${register}/${namespace}/${app_name}
    local image_tag=${IMAGE_VERSION}

    case $1 in
    build)
        docker build -t ${image_name}:${image_tag} -f ${docker_file_path} .
        ;;
    tag)
        docker tag ${image_name}:${image_tag} ${image_name}:${default_latest_tag}
        ;;
    login)
        docker login ${register}
        ;;
    push)
        docker tag ${image_name}:${image_tag} ${image_name}:${default_latest_tag}
        docker push ${image_name}:${image_tag}
        docker push ${image_name}:${default_latest_tag}
        ;;
    *)
        echo "Usage: build.sh build [<IMAGE_VERSION>]"
        echo "       build.sh login"
        echo "       build.sh push [<IMAGE_VERSION>]"
        ;;
    esac
}

main $*
