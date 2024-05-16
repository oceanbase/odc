#!/bin/bash
# create node env
NVS_VERSION="v1.6.0"
NODE_VERSION="16.14.0"

function create_node_env() {
    local nvs_home=${1:-/usr/local/nvs}

    if ! git clone https://github.com/jasongin/nvs --depth=1 "$nvs_home"; then
        echo "git clone https://github.com/jasongin/nvs failed"
        return 1
    fi

    pushd "$nvs_home"

    git fetch --all --tags
    if ! git checkout ${NVS_VERSION}; then
        echo "tag ${NVS_VERSION} not exists"
        popd
        return 2
    fi
    popd

    if ! source ${nvs_home}/nvs.sh install; then
        echo "install nvs failed"
        return 3
    fi
    echo "install nvs succeed"

    if ! nvs add node/${NODE_VERSION}; then
        echo "nvs add ${NODE_VERSION} failed"
        return 4
    fi
    if ! nvs use node/${NODE_VERSION}; then
        echo "nvs use ${NODE_VERSION} failed"
        return 5
    fi

    if ! node -v | grep ${NODE_VERSION}; then
        echo "node v${NODE_VERSION} check failed"
        return 6
    fi

    if ! npm install -g pnpm@8; then
        echo "install pnpm failed"
        return 7
    fi

    if ! pnpm -v; then
        echo "pnpm check failed"
        return 8
    fi

    return 0
}

create_node_env $*
exit $?
