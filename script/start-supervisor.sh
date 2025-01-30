#! /bin/bash
# Start up script for odc-job
#
# @author yaobin
# @date 2024-01-29
# @since 4.2.4

# constants
script_source=$(readlink -f $0)
bin_directory=$(dirname $script_source)
install_directory=$(dirname $bin_directory)
app_log_config_file="${install_directory}/conf/log4j2-supervisor.xml"
current_work_directory="$(pwd)"
gc_basic_options="-XX:+UseG1GC -XX:+PrintAdaptiveSizePolicy -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"
gc_log_options="-Xloggc:${install_directory}/log/gc.log -XX:+UseGCLogFileRotation -XX:GCLogFileSize=50M -XX:NumberOfGCLogFiles=5"
default_heap_options="-Xmx1024m -Xms512m"
default_gc_options="${gc_basic_options} ${gc_log_options}"
default_oom_options="-XX:+ExitOnOutOfMemoryError"
default_agent_main_class_name="com.oceanbase.odc.supervisor.SupervisorAgent"
default_spring_boot_loader="org.springframework.boot.loader.PropertiesLauncher"
main_class_caller="-Dloader.main=${default_agent_main_class_name} ${default_spring_boot_loader}"

# define some helper functions
function usage() {
    echo "Usage:"
    echo "start odc-supervisor: ${0}"
    echo "show this usage message: ${0} --help"
    echo ""
    echo "Required environment variables:"

    echo ""
    echo "Optional environment variables:"
    echo "- ODC_SUPERVISOR_LISTEN_PORT: http server listen port, '9999' as default"
    echo "- ODC_LOG_DIR: log directory identify, '${install_directory}/log' as default"
    echo "- ODC_JAR_FILE: odc jar file identify, '${install_directory}/lib/odc-server-*.jar' as default"
    echo "- ODC_WORK_DIR: odc work directory, '${current_work_directory}' as default"
    echo "- ODC_JVM_HEAP_OPTIONS: JVM heap options, e.g. -Xmx4096m -Xms4096m..., '${default_heap_options}' as default"
    echo "- ODC_JVM_GC_OPTIONS: JVM gc options, e.g. -XX:+UseG1GC -XX:+PrintGCDetails..."
    echo "- ODC_JVM_OOM_OPTIONS: JVM oom options, e.g. -XX:+ExitOnOutOfMemoryError..."
    echo "- ODC_JVM_EXTRA_OPTIONS: JVM extra options, any other jvm options except heap/gc/oom"
    echo "- ODC_APP_EXTRA_ARGS: ODC application extra args, any other app options"
    echo "- ODC_JVM_TRY_USE_JDK: If try to use jdk instead of jre, available while JAVA_HOME not set"
    echo ""
}

function check_env_value_set() {
    local name=$1
    local value=$2
    if [ -z "$value" ]; then
        echo "FATAL ERROR!, environment variable <${name}> not set, cannot start odc-job"
        exit 1
    fi
}

function init_parameters() {
    echo "init parameters start"

    # init parameters with default value
    app_log_directory="${ODC_LOG_DIR:-${install_directory}/log}"
    jar_file="${ODC_JAR_FILE:-${install_directory}/lib/odc-server-*.jar}"
    plugin_directory="${ODC_PLUGIN_DIR:-${install_directory}/plugins}"
    echo "init parameters done"
}

# init jvm args
function init_jvm_options() {
    echo "init jvm options start"
    heap_options=${ODC_JVM_HEAP_OPTIONS:-${default_heap_options}}
    gc_options=${ODC_JVM_GC_OPTIONS:-${default_gc_options}}
    init_remote_debug_options
    oom_options=${ODC_JVM_OOM_OPTIONS:-${default_oom_options}}
    extra_options="${ODC_JVM_EXTRA_OPTIONS}"
    if [ -z "${SPACEV_JAVA_AGENT}" ]; then
        spacev_java_agent_options=""
        echo "SPACEV_JAVA_AGENT is not set"
    else
        spacev_java_agent_options="${SPACEV_JAVA_AGENT}"
        echo "SPACEV_JAVA_AGENT is set"
    fi
    local log_options="-Dlog4j.configurationFile=${app_log_config_file} -Dodc.log.directory=${app_log_directory}"
    local work_dir_options="-Duser.dir=${ODC_WORK_DIR:-${current_work_directory}}"
    local plugin_options="-Dplugin.dir=${plugin_directory}"

    app_options="${log_options} ${work_dir_options} ${plugin_options}"

    local extra_args="${ODC_APP_EXTRA_ARGS}"
    app_args="${extra_args}"

    echo "init jvm options done"
}

function init_remote_debug_options() {
    if [ -z "${ODC_REMOTE_DEBUG_PORT}" ]; then
        echo "ODC_REMOTE_DEBUG_PORT not set, will disable remote debug."
    else
        echo "ODC_REMOTE_DEBUG_PORT is set to ${ODC_REMOTE_DEBUG_PORT}, will enable remote debug."
        remote_debug_options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${ODC_REMOTE_DEBUG_PORT}"
    fi
}

function init_java_exec() {
    echo "init java exec start"
    try_use_jdk=${ODC_JVM_TRY_USE_JDK:-0}
    java_exec=java
    if [ ! -z "${JAVA_HOME}" ]; then
        echo "JAVA_HOME detected, will use ${JAVA_HOME}/bin/java instead"
        java_exec="${JAVA_HOME}/bin/java"
    elif [ "1" = "${try_use_jdk}" ]; then
        echo "ODC_JVM_TRY_USE_JDK detected, try detect jdk home directory..."
        local jdk_home=$(dirname $(dirname $(dirname $(readlink -f $(which java)))))
        local has_jdk=$(if [ -f "${jdk_home}/bin/java" ]; then echo 1; else echo 0; fi)
        if [ "1" = "${has_jdk}" ]; then
            export JAVA_HOME=${jdk_home}
            echo "jdk_home detected, set as JAVA_HOME, JAVA_HOME=${JAVA_HOME}"
            java_exec="${JAVA_HOME}/bin/java"
        fi
    fi
    ${java_exec} -version
    if [ $? != 0 ]; then
        echo "FATAL ERROR! java program <${java_exec}> not found, cannot start odc-job"
        exit 1
    fi
    echo "init java exec done, java_exec=${java_exec}"
}

main() {
    if [[ "$1" == "--help" ]]; then
        usage
        exit 0
    fi

    init_java_exec
    init_parameters
    init_jvm_options

    if [ ! -e ${jar_file} ]; then
        echo "FATAL ERROR!, jar file <${jar_file}> not found, cannot start odc-job"
        exit 1
    fi

    echo "Starting odc-supervisor..."

    local cmd="${java_exec} ${remote_debug_options} ${spacev_java_agent_options} ${gc_options} ${heap_options} ${oom_options}
    ${extra_options} ${app_options} -cp
    ${jar_file} ${main_class_caller} ${app_args}"
    echo "cmd=${cmd}"
    eval ${cmd}
    return $?
}

main $*
exit $?
