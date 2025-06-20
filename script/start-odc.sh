#! /bin/bash
# Start up script for odc-server
#
# @author yh263208
# @date 2020-12-18 13:48
# @since ODC_release_2.4.0

# constants
script_source=$(readlink -f $0)
bin_directory=$(dirname $script_source)
install_directory=$(dirname $bin_directory)
app_log_config_file="${install_directory}/conf/log4j2.xml"
current_work_directory="$(pwd)"
default_server_port=8989
gc_basic_options="-XX:+UseG1GC -XX:+PrintAdaptiveSizePolicy -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"
gc_log_options="-Xloggc:${install_directory}/log/gc.log -XX:+UseGCLogFileRotation -XX:GCLogFileSize=50M -XX:NumberOfGCLogFiles=5"
default_heap_options="-XX:MaxRAMPercentage=45.0 -XX:InitialRAMPercentage=45.0"
default_gc_options="${gc_basic_options} ${gc_log_options}"
default_oom_options="-XX:+ExitOnOutOfMemoryError"

# define some helper functions
function usage() {
    echo "Usage:"
    echo "start odc-server: ${0}"
    echo "show this usage message: ${0} --help"
    echo ""
    echo "Required environment variables:"
    echo "- DATABASE_HOST      host of metadb, eg: 127.0.0.1"
    echo "- DATABASE_PORT      port of metadb, eg: 3306"
    echo "- DATABASE_USERNAME  user of metadb, eg: xxx@xxx#xxx"
    echo "- DATABASE_PASSWORD  password of metadb"
    echo "- DATABASE_NAME      database of metadb, eg: odc_metadb"
    echo "- ODC_ADMIN_INITIAL_PASSWORD: initial password for 'admin' account"
    echo ""
    echo "Optional environment variables:"
    echo "- ODC_PROFILE_MODE: profile mode, 'alipay' as default"
    echo "    - optional values [alipay,clientMode]"
    echo "- ODC_SERVER_PORT: http server listen port, '8989' as default"
    echo "- ODC_LOG_DIR: log directory identify, '${install_directory}/log' as default"
    echo "- OBCLIENT_WORK_DIR: obclient work directory identify, '${install_directory}/data' as default"
    echo "- ODC_JAR_FILE: odc jar file identify, '${install_directory}/lib/odc-server-*.jar' as default"
    echo "- ODC_PLUGIN_DIR: odc plugin dir identify, '${install_directory}/plugins' as default"
    echo "- ODC_STARTER_DIR: odc starter dir identify, '${install_directory}/starters' as default"
    echo "- ODC_MODULE_DIR: odc module dir identify, '${install_directory}/modules' as default"
    echo "- ODC_WORK_DIR: odc work directory, '${current_work_directory}' as default"
    echo "- ODC_JVM_HEAP_OPTIONS: JVM heap options, e.g. -Xmx4096m -Xms4096m..., '${default_heap_options}' as default"
    echo "- ODC_JVM_GC_OPTIONS: JVM gc options, e.g. -XX:+UseG1GC -XX:+PrintGCDetails..."
    echo "- ODC_JVM_OOM_OPTIONS: JVM oom options, e.g. -XX:+ExitOnOutOfMemoryError..."
    echo "- ODC_JVM_EXTRA_OPTIONS: JVM extra options, any other jvm options except heap/gc/oom"
    echo "- ODC_APP_EXTRA_ARGS: ODC application extra args, any other app options"
    echo "- ODC_JVM_TRY_USE_JDK: If try to use jdk instead of jre, available while JAVA_HOME not set"
    echo ""
    echo "Deprecated environment variables:"
    echo "- PROFILE_MODE: deprecated environment variable, same as ODC_PROFILE_MODE, for compatibility"
    echo "- SERVER_PORT: deprecated environment variable, same as ODC_SERVER_PORT, for compatibility"
}

function log_info() {
    echo "$(date +"%Y-%m-%dT%H:%M:%S.%Z") [INFO]" "$*"
}

function log_error() {
    echo 1>&2 "$(date +"%Y-%m-%dT%H:%M:%S.%Z") [ERROR]" "$*"
}

function check_env_value_set() {
    local name=$1
    local value=$2
    if [ -z "$value" ]; then
        log_error "FATAL ERROR!, environment variable <${name}> not set, cannot start odc-server"
        exit 1
    fi
}

function init_parameters() {
    log_info "init parameters start"

    # init parameters require environment variable
    check_env_value_set DATABASE_HOST "${DATABASE_HOST}"
    check_env_value_set DATABASE_PORT "${DATABASE_PORT}"
    check_env_value_set DATABASE_USERNAME "${DATABASE_USERNAME}"
    check_env_value_set DATABASE_NAME "${DATABASE_NAME}"

    # init parameters with default value
    profile="${ODC_PROFILE_MODE:-${PROFILE_MODE:-alipay}}"
    server_port="${ODC_SERVER_PORT:-${SERVER_PORT:-${default_server_port}}}"
    app_log_directory="${ODC_LOG_DIR:-${install_directory}/log}"
    jar_file="${ODC_JAR_FILE:-${install_directory}/lib/odc-server-*.jar}"
    obclient_work_directory="${OBCLIENT_WORK_DIR:-${install_directory}/data}"
    plugin_directory="${ODC_PLUGIN_DIR:-${install_directory}/plugins}"
    starter_directory="${ODC_STARTER_DIR:-${install_directory}/starters}"
    module_directory="${ODC_MODULE_DIR:-${install_directory}/modules}"
    obclient_file_path="${OBCLIENT_FILE_PATH:-${install_directory}/obclient/bin/obclient}"
    if [ ! -z "${ODC_HOST}" ]; then
        log_info "ODC_HOST given, will set LOCAL_IP by ODC_HOST, ODC_HOST=${ODC_HOST}"
        export LOCAL_IP="${ODC_HOST}"
    else
        log_info "ODC_HOST not given, will set ODC_HOST by ip addr"
        export LOCAL_IP=$(ip addr | grep "eth0" | grep "inet" | awk '{print $2}' | awk -F '/' '{print $1}')
    fi
    export LOCAL_HOSTNAME=$(hostname -I | awk -F ' ' '{print $1}')
    export ODC_PROFILE_MODE="${profile}"

    # set heap MaxRAMPercentage=60.0 if ODC_TASK_RUN_MODE is K8S
    if [[ "${ODC_TASK_RUN_MODE}" == "K8S" ]]; then
        default_heap_options="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=60.0"
    fi
    log_info "init parameters done"
}

# init jvm args
function init_jvm_options() {
    log_info "init jvm options start"
    heap_options=${ODC_JVM_HEAP_OPTIONS:-${default_heap_options}}
    gc_options=${ODC_JVM_GC_OPTIONS:-${default_gc_options}}
    init_remote_debug_options
    oom_options=${ODC_JVM_OOM_OPTIONS:-${default_oom_options}}
    extra_options="${ODC_JVM_EXTRA_OPTIONS}"
    if [ -z "${SPACEV_JAVA_AGENT}" ]; then
        spacev_java_agent_options=""
        log_info "SPACEV_JAVA_AGENT is not set"
    else
        spacev_java_agent_options="${SPACEV_JAVA_AGENT}"
        log_info "SPACEV_JAVA_AGENT is set"
    fi
    local log_options="-Dlog4j.configurationFile=${app_log_config_file} -Dodc.log.directory=${app_log_directory}"
    local work_dir_options="-Duser.dir=${ODC_WORK_DIR:-${current_work_directory}}"
    local plugin_options="-Dplugin.dir=${plugin_directory}"
    local starter_options="-Dstarter.dir=${starter_directory}"

    app_options="${log_options} ${work_dir_options} ${plugin_options} ${starter_options}"

    local listen_port_args="--server.port=${server_port}"
    local obclient_args="--obclient.work.dir=${obclient_work_directory} --obclient.file.path=${obclient_file_path}"
    local file_args="--file.storage.dir=${obclient_work_directory}"
    local extra_args="${ODC_APP_EXTRA_ARGS}"
    app_args="${listen_port_args} ${obclient_args} ${file_args} ${extra_args}"

    log_info "init jvm options done"
}

function init_remote_debug_options() {
    if [ -z "${ODC_REMOTE_DEBUG_PORT}" ]; then
        log_info "ODC_REMOTE_DEBUG_PORT not set, will disable remote debug."
    else
        log_info "ODC_REMOTE_DEBUG_PORT is set to ${ODC_REMOTE_DEBUG_PORT}, will enable remote debug."
        remote_debug_options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${ODC_REMOTE_DEBUG_PORT}"
    fi
}

function init_java_exec() {
    log_info "init java exec start"
    try_use_jdk=${ODC_JVM_TRY_USE_JDK:-0}
    java_exec=java
    if [ ! -z "${JAVA_HOME}" ]; then
        log_info "JAVA_HOME detected, will use ${JAVA_HOME}/bin/java instead"
        java_exec="${JAVA_HOME}/bin/java"
    elif [ "1" = "${try_use_jdk}" ]; then
        log_info "ODC_JVM_TRY_USE_JDK detected, try detect jdk home directory..."
        local jdk_home=$(dirname $(dirname $(dirname $(readlink -f $(which java)))))
        local has_jdk=$(if [ -f "${jdk_home}/bin/java" ]; then echo 1; else echo 0; fi)
        if [ "1" = "${has_jdk}" ]; then
            export JAVA_HOME=${jdk_home}
            log_info "jdk_home detected, set as JAVA_HOME, JAVA_HOME=${JAVA_HOME}"
            java_exec="${JAVA_HOME}/bin/java"
        fi
    fi
    ${java_exec} -version
    if [ $? != 0 ]; then
        log_error "FATAL ERROR! java program <${java_exec}> not found, cannot start odc-server"
        exit 1
    fi
    log_info "init java exec done, java_exec=${java_exec}"
}

main() {
    # if start as supervisor agent mode
    if [[ "${ODC_SUPERVISOR_LISTEN_PORT}" ]]; then
        echo "start as supervisor agent mode  with listen port ${ODC_SUPERVISOR_LISTEN_PORT}"
        sh -c "${install_directory}/bin/start-supervisor.sh"
        exit 0;
    fi
    # if ODC_BOOT_MODE is TASK_EXECUTOR start odc server as task executor mode
    if [[ "${ODC_BOOT_MODE}" == "TASK_EXECUTOR" ]]; then
        echo "start odc as ${ODC_BOOT_MODE}"
        sh -c "${install_directory}/bin/start-job.sh"
        exit 0
    fi

    if [[ "$1" == "--help" ]]; then
        usage
        exit 0
    fi

    init_java_exec
    init_parameters
    init_jvm_options

    if [ ! -e ${jar_file} ]; then
        log_error "FATAL ERROR!, jar file <${jar_file}> not found, cannot start odc-server"
        exit 1
    fi

    log_info "Starting odc-server..."

    export ODC_DATABASE_HOST=${DATABASE_HOST}
    export ODC_DATABASE_PORT=${DATABASE_PORT}
    export ODC_DATABASE_USERNAME=${DATABASE_USERNAME}
    export ODC_DATABASE_PASSWORD=${DATABASE_PASSWORD:-""}
    export ODC_DATABASE_NAME=${DATABASE_NAME}

    local cmd="${java_exec} ${remote_debug_options} ${spacev_java_agent_options} ${gc_options} ${heap_options} ${oom_options}
    ${extra_options} ${app_options} -jar
    ${jar_file} ${app_args}"
    log_info "cmd=${cmd}"
    eval ${cmd}
    return $?
}

main $*
exit $?
