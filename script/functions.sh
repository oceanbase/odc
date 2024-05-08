# This file contains functions to be used by most or all shell scripts in the script directory.

# constants used in functions.sh
SQL_CONSOLE_DIR="client"
NVS_HOME="$HOME/.nvs"
RPM_DEFAULT_INSTALL_PREFIX="/opt"
NVS_VERSION="v1.6.0"
NODE_VERSION="16.14.0"
OSS_RETRY_TIMES=50
OBCLIENT_VERSION=2_2_4
OBCLIENT_VERSION_NUMBER=2.2.4

ODC_FUNCTION_SCRIPT_SOURCE=$(readlink -f $0)

# global environment variables, may refer out of function.sh
export ODC_SCRIPT_DIR=$(dirname ${ODC_FUNCTION_SCRIPT_SOURCE})
export ODC_DIR=$(dirname ${ODC_SCRIPT_DIR})

# oss related environment variables
export ODC_OSS_ENDPOINT=${oss_endpoint:-}
export ODC_OSS_BUCKET_NAME=${oss_bucket_name:-}
export ODC_OSS_ACCESS_KEY_ID=${oss_key_id:-}
export ODC_OSS_ACCESS_KEY_SECRET=${oss_key_secret:-}
export ODC_OSS_CONFIG_FILE_NAME=$(echo ~/.odcossutilconfig)
export ODC_CDN_BASE_URL=${odc_cdn_base_url:-}

function log_info() {
    echo "$(date +"%Y-%m-%dT%H:%M:%S.%Z") [INFO]" "$*"
}

function log_error() {
    echo 1>&2 "$(date +"%Y-%m-%dT%H:%M:%S.%Z") [ERROR]" "$*"
}

#############################################
# echo with function name prefix
# PARAM $1: message
#############################################
function func_echo() {
    echo 1>&2 "[${FUNCNAME[1]:-N/A}] " "$*"
}

#############################################
# check if given cmd exists
# PARAM $1: command name to check
# RETURN:  0 if cmd exists, non-zero if failed
#############################################
function is_cmd_exists() {
    local cmd="$1"
    if [ -z "$cmd" ]; then
        echo "Usage: is_cmd_exist your_cmd"
        return 1
    fi

    if ! type "$cmd" >/dev/null 2>&1; then
        echo "command ${cmd} not exists"
        return 2
    fi

    return 0
}

#########################################################
# in order to adapt all os,
# use sed print other new file and overwrite old file
#########################################################
function sed_file() {
    local replace_expression=$1
    local file_name=$2
    sed "${replace_expression}" "${file_name}" >"${file_name}.bak" && mv -f "${file_name}.bak" "${file_name}"
}

#############################################
# initial ossutil config
# RETURN:  0 if succeed, non-zero if failed
#############################################
function config_ossutil() {
    if ! is_cmd_exists ossutil64; then
        func_echo "config ossutil failed, ossutil64 command not found"
        return 1
    fi

    if [ -z "$ODC_OSS_ACCESS_KEY_ID" ]; then
        echo "oss access key not set, skip config ossutil"
        return 0
    fi

    ossutil64 config --language=EN \
        --endpoint "${ODC_OSS_ENDPOINT}" \
        --access-key-id "${ODC_OSS_ACCESS_KEY_ID}" \
        --access-key-secret "${ODC_OSS_ACCESS_KEY_SECRET}" \
        --config-file "${ODC_OSS_CONFIG_FILE_NAME}"

    func_echo "config ossutil success"

    return 0
}

#############################################
# get odc version number
# RETURN:  ODC version number, e.g. 3.3.0
#############################################
function get_odc_version() {
    local odc_version=$(cat "${ODC_DIR}/distribution/odc-server-VER.txt" | sed 's/[ \s\t\n\r]*//g')
    echo "${odc_version}"
}

#############################################
# initial node environment,
# include node12 and pnpm
# RETURN:  0 if succeed, non-zero if failed
#############################################
function init_node_env() {
    if ! is_cmd_exists nvs; then
        if ! git clone https://github.com/jasongin/nvs --depth=1 "$NVS_HOME"; then
            func_echo "git clone https://github.com/jasongin/nvs failed"
            return 1
        fi

        pushd "$NVS_HOME"

        git fetch --all --tags
        if ! git checkout ${NVS_VERSION}; then
            func_echo "tag ${NVS_VERSION} not exists"
            popd
            return 2
        fi
        popd

        if ! source ${NVS_HOME}/nvs.sh install; then
            func_echo "install nvs failed"
            return 3
        fi
        func_echo "install nvs succeed"
    fi

    if ! nvs link node/${NODE_VERSION}; then
        func_echo "node ${NODE_VERSION} not installed, will install"
        if ! nvs add node/${NODE_VERSION}; then
            func_echo "nvs add ${NODE_VERSION} failed"
            return 3
        fi
        if ! nvs link node/${NODE_VERSION}; then
            func_echo "nvs use ${NODE_VERSION} failed"
            return 4
        fi
    fi

    if ! node -v | grep "${NODE_VERSION}"; then
        func_echo "node${NODE_VERSION} check failed"
        return 5
    fi

    if ! is_cmd_exists pnpm; then
        if ! npm install -g pnpm@8; then
            func_echo "install pnpm failed"
            return 6
        fi
    fi

    if ! pnpm -v; then
        func_echo "pnpm check failed"
        return 7
    fi

    return 0
}

# clean nvs temp files for avoid nvs use operation require interaction
function clean_nvs_temp_files() {
    rm -fv ${NVS_HOME}/nvs_tmp_*.sh
}

#############################################
# update submodule
# RETURN: 0 if succeed, non-zero if failed
#############################################
function update_submodule() {
    func_echo "start update submodule ,current directory: [$(pwd)]"

    # git submodule command need to run from the top level of the working tree
    pushd "${ODC_DIR}"
    func_echo "goto ${ODC_DIR}"

    # initial submodule local configuration file
    git submodule init
    # refresh submodule git url to project
    git submodule sync
    # refresh submodule from url
    git submodule update --init --remote
    local submodule_update_ret=$?

    # show submodule status and summary
    func_echo "submodule status: "
    git submodule status

    func_echo "submodule summary: "
    git submodule summary

    popd
    func_echo "back to previous directory: [$(pwd)]"

    func_echo "update submodule complete"
    return ${submodule_update_ret}
}

function build_sqlconsole() {
    local sqlconsole_path="${ODC_DIR}/${SQL_CONSOLE_DIR}"
    if [ ! -d "${sqlconsole_path}" ]; then
        func_echo "${sqlconsole_path} directory not exists"
        return 1
    fi

    if ! npm install pnpm@8 -g; then
        func_echo "npm install pnpm -g failed"
        return 2
    fi
    func_echo "npm install pnpm -g success"

    pushd "${sqlconsole_path}"
    if ! (pnpm install || pnpm install || pnpm install); then
        func_echo "pnpm install failed"
        popd
        return 2
    fi
    func_echo "pnpm install success"

    if ! pnpm run build:odc; then
        func_echo "pnpm run build:odc failed"
        popd
        return 3
    fi
    func_echo "pnpm run build:odc success"

    popd

    local backend_static_path="${ODC_DIR}/server/odc-server/src/main/resources/static/"
    if [ ! -d "${backend_static_path}" ]; then
        func_echo "mkdir ${backend_static_path}"
        mkdir -p "${backend_static_path}"
    fi

    rm -vfR ${backend_static_path}/*

    func_echo "copy files to backend static path"
    cp -vfR ${sqlconsole_path}/dist/renderer/* ${backend_static_path}
    return $?
}

#########################################################
# use cdn integration for develop stage
#########################################################
function fetch_sqlconsole_from_cdn() {
    if [ -z "$ODC_CDN_BASE_URL" ]; then
        echo "ODC_CDN_BASE_URL is null"
        return 1
    fi
    local backend_static_path="${ODC_DIR}/server/odc-server/src/main/resources/static/"

    if ! git config -f "${ODC_DIR}/.gitmodules" --get submodule.client.branch; then
        func_echo "check sqlconsole branch name failed"
        return 1
    fi

    rm -vfR ${backend_static_path}/*

    if [ ! -d "${backend_static_path}" ]; then
        func_echo "mkdir ${backend_static_path}"
        mkdir -p "${backend_static_path}"
    fi

    local sqlconsole_branch=$(git config -f "${ODC_DIR}/.gitmodules" --get submodule.client.branch)
    local sqlconsole_base_url="${ODC_CDN_BASE_URL}/${sqlconsole_branch}"
    local sqlconsole_index_html="${sqlconsole_base_url}/index.html"

    func_echo "download index.html to backend static path"
    curl -o "${backend_static_path}index.html" "${sqlconsole_index_html}"
    return $?
}

#############################################
# use maven to build jar
# PARAM $1: extra maven args
#############################################
function maven_build_jar() {
    local maven_extra_args=$@
    pushd "${ODC_DIR}" || return 1

    func_echo "maven build jar package starting..."
    mvn help:system
    if ! mvn clean install -Dmaven.test.skip=true ${maven_extra_args[@]}; then
        func_echo "maven build jar ${maven_extra_args[@]} failed"
        popd
        return 2
    fi
    func_echo "maven build jar ${maven_extra_args[@]} succeed"

    popd
    return 0
}

# local install libs
function maven_install_libs() {
    local maven_extra_args=$@
    pushd "${ODC_DIR}/libs" || return 1

    func_echo "maven install libs ..."

    for module_name in *; do
        if [ -d "$module_name" ]; then
            pushd "$module_name" || return 2
            func_echo "start install lib $module_name"
            if ! mvn clean install -Dmaven.test.skip=true ${maven_extra_args[@]}; then
                func_echo "maven install lib $module_name with args ${maven_extra_args[@]} failed"
            else
                func_echo "maven install lib $module_name with args ${maven_extra_args[@]} succeed"
            fi
            popd
        fi
    done

    func_echo "maven install libs with args ${maven_extra_args[@]} succeed"
    popd
    return 0
}

function oss_fetch_obclient() {
    local rpm_arch=$(get_cpu_arch)
    if ! config_ossutil; then
        log_error "config ossutil failed"
        return 1
    fi

    target_obclient_file_path="oss://${ODC_OSS_BUCKET_NAME}/library/obclient/${OBCLIENT_VERSION}/${rpm_arch}/obclient.tar.gz"
    local_obclient_file_path="${ODC_DIR}/import/"
    echo "local_obclient_file_path: ${local_obclient_file_path}"
    ossutil64 cp --force --retry-times=${OSS_RETRY_TIMES} --config-file "${ODC_OSS_CONFIG_FILE_NAME}" \
        "${target_obclient_file_path}" "${local_obclient_file_path}"

    echo "list files in import directory"
    ls ${local_obclient_file_path}
}

#
# copy architecture matched obclient install package to `import/obclient.tar.gz`
#
function copy_obclient() {
    local source_directory_name=$(get_cpu_arch | grep -q x86 && echo "linux_x86" || echo "linux_arm64")
    local source_obclient_file_path="${ODC_DIR}/build-resource/obclient/${OBCLIENT_VERSION_NUMBER}/${source_directory_name}/obclient.tar.gz"
    local target_obclient_file_path="${ODC_DIR}/import/obclient.tar.gz"
    if [[ -f "${source_obclient_file_path}" ]]; then
        echo "${source_obclient_file_path} exists, will copy to ${target_obclient_file_path} ..."
        cp -vf "${source_obclient_file_path}" "${target_obclient_file_path}"
        return $?
    else
        echo "${source_obclient_file_path} not exists, skip copy."
        return 0
    fi
}

function maven_build_rpm() {
    local rpm_release=$1
    if [ -z "$rpm_release" ]; then
        echo "Usage: maven_build_rpm <rpm_release>"
        return 1
    fi

    pushd "${ODC_DIR}" || return 2

    func_echo "maven build rpm package starting..."
    if ! mvn --file server/odc-server/pom.xml rpm:rpm \
        -Drpm.prefix=${RPM_DEFAULT_INSTALL_PREFIX} \
        -Drpm.release=${rpm_release}; then
        func_echo "maven build rpm failed"
        popd
        return 3
    fi
    func_echo "maven build rpm succeed"

    popd
    return 0
}

function copy_rpm_resources() {
    echo "copy rpm package(s) to distribution/docker/resources"
    rm -vf ${ODC_DIR}/distribution/docker/resources/odc-*.rpm
    mkdir -p ${ODC_DIR}/distribution/docker/resources/
    mv --verbose ${ODC_DIR}/server/odc-server/target/rpm/odc-server/RPMS/*/odc-*.rpm ${ODC_DIR}/distribution/docker/resources/
    return $?
}

# print env info, includes:
# - location: path and user
# - infrastructure: cpu,memory and disk
# - software: node and java
function print_env_info() {
    echo "-------------------------------"
    echo "--- environment check start ---"
    echo "-------------------------------"

    echo "check pwd"
    pwd

    echo "check whoami"
    whoami

    echo "check cpu"
    nproc

    echo "check memory"
    free -h

    echo "check disk"
    df -h

    echo "check node"
    node -v

    echo "check pnpm"
    pnpm -v

    echo "check jdk"
    java -version

    echo "check maven"
    mvn -version

    echo "-------------------------------"
    echo "---  environment check end  ---"
    echo "-------------------------------"
}

# get os version
# optional return value: linux, macos, unknown
function get_os_version() {
    os_version=$(uname -s)
    case "$os_version" in
    Linux*)
        os_version="linux"
        ;;
    Darwin*)
        os_version="macos"
        ;;
    *)
        os_version="unknown"
        ;;
    esac
    echo "${os_version}"
}

# get cpu architecture
# optional return value: x86, aarch, unknown
function get_cpu_arch() {
    local cpu_arch=$(uname -m)
    case "$cpu_arch" in
    x86*)
        cpu_arch="x86"
        ;;
    aarch*)
        cpu_arch="aarch"
        ;;
    *)
        cpu_arch="unknown"
        ;;
    esac
    echo "${cpu_arch}"
}
