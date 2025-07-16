#!/usr/bin/env bash
# start odc in background
# Usage: 
#   ./nohup-start-odc.sh                           # 使用默认JRE
#   ./nohup-start-odc.sh /path/to/custom-jre       # 使用自定义JRE
#   CUSTOM_JRE_PATH=/path/to/custom-jre ./nohup-start-odc.sh  # 通过环境变量指定

script_source=$(readlink -f $0)
script_directory=$(dirname $script_source)
install_directory=$(dirname $script_directory)

# 帮助函数
show_usage() {
    echo "Usage:"
    echo "  ${0}                              # 使用默认JRE启动"
    echo "  ${0} /path/to/custom-jre          # 使用自定义JRE启动"
    echo "  ${0} --help                       # 显示帮助信息"
    echo ""
    echo "Environment variables:"
    echo "  CUSTOM_JRE_PATH                   # 自定义JRE路径"
    echo ""
    echo "Examples:"
    echo "  ${0} ./custom-jre                 # 使用项目根目录下的custom-jre"
    echo "  CUSTOM_JRE_PATH=./custom-jre ${0} # 通过环境变量指定JRE"
}

# 检查帮助参数
if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
    show_usage
    exit 0
fi

# 确定要使用的JRE路径
custom_jre_path=""
if [ -n "$1" ]; then
    # 通过命令行参数指定
    custom_jre_path="$1"
elif [ -n "$CUSTOM_JRE_PATH" ]; then
    # 通过环境变量指定
    custom_jre_path="$CUSTOM_JRE_PATH"
fi

# 验证自定义JRE路径
if [ -n "$custom_jre_path" ]; then
    # 转换为绝对路径
    if [[ "$custom_jre_path" != /* ]]; then
        custom_jre_path="$install_directory/$custom_jre_path"
    fi
    
    # 检查路径是否存在
    if [ ! -d "$custom_jre_path" ]; then
        echo "Error: Custom JRE directory does not exist: $custom_jre_path"
        exit 1
    fi
    
    # 检查java执行文件是否存在
    if [ ! -f "$custom_jre_path/bin/java" ]; then
        echo "Error: Java executable not found in custom JRE: $custom_jre_path/bin/java"
        exit 1
    fi
    
    # 测试JRE是否可用
    echo "Testing custom JRE: $custom_jre_path"
    if ! "$custom_jre_path/bin/java" -version >/dev/null 2>&1; then
        echo "Error: Custom JRE is not working properly: $custom_jre_path"
        exit 1
    fi
    
    # 设置JAVA_HOME环境变量，这样start-odc.sh就会使用自定义JRE
    export JAVA_HOME="$custom_jre_path"
    echo "Using custom JRE: $custom_jre_path"
    echo "Java version: $($custom_jre_path/bin/java -version 2>&1 | head -1)"
else
    echo "Using default JRE from system PATH"
fi

# 启动ODC服务
echo "Starting ODC server in background..."
nohup ${script_directory}/start-odc.sh >/dev/null 2>&1 &
ret=$?
pid=$!
echo "start odc-server done, ret=${ret}, pid=${pid}"

sleep 1

echo "check process status:"
ps -p ${pid}
ret=$?
if [ $ret -ne 0 ]; then
    echo "process start failed!"
    echo "please try '${script_directory}/start-odc.sh' for more information!"
else
    echo "process start success!"
    echo "you may check log by 'tailf ${install_directory}/log/odc.log'"
    if [ -n "$custom_jre_path" ]; then
        echo "Running with custom JRE: $custom_jre_path"
    fi
fi
exit $ret
