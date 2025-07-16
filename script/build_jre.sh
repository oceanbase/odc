#!/bin/bash

#
# Copyright (c) 2025 OceanBase.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 引用functions.sh获取ODC_DIR变量
if ! source $(dirname "$0")/functions.sh; then
    echo "source functions.sh failed"
    exit 1
fi

# 使用ODC_DIR替代PROJECT_ROOT
PROJECT_ROOT="${ODC_DIR}"

# 各个目录路径
LIBS_DIR="$PROJECT_ROOT/lib"
STARTERS_DIR="$PROJECT_ROOT/starters"
MODULES_DIR="$PROJECT_ROOT/modules"
PLUGINS_DIR="$PROJECT_ROOT/plugins"

# 自动检测JDK模块路径
detect_jdk_mods_path() {
    local jdk_mods_path=""
    
    # 首先检查JAVA_HOME环境变量
    if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/jmods" ]; then
        jdk_mods_path="$JAVA_HOME/jmods"
        echo "Found jmods via JAVA_HOME: $jdk_mods_path"
        echo "$jdk_mods_path"
        return 0
    fi
    
    # 方法1：使用java -XshowSettings:properties获取java.home
    echo "Trying to detect JDK path using java -XshowSettings:properties..."
    local java_home_prop=$(java -XshowSettings:properties 2>/dev/null | grep "java.home" | head -1 | sed 's/.*= *//')
    if [ -n "$java_home_prop" ] && [ -d "$java_home_prop/jmods" ]; then
        jdk_mods_path="$java_home_prop/jmods"
        echo "Found jmods via java.home property: $jdk_mods_path"
        echo "$jdk_mods_path"
        return 0
    fi
    
    # 方法2：检查jenv
    if command -v jenv > /dev/null 2>&1; then
        echo "Detected jenv, trying to get JDK path..."
        local jenv_java_home=$(jenv prefix 2>/dev/null)
        if [ -n "$jenv_java_home" ] && [ -d "$jenv_java_home/jmods" ]; then
            jdk_mods_path="$jenv_java_home/jmods"
            echo "Found jmods via jenv: $jdk_mods_path"
            echo "$jdk_mods_path"
            return 0
        fi
    fi
    
    # 方法3：检查SDKMAN
    if [ -n "$SDKMAN_DIR" ] && [ -n "$JAVA_VERSION" ]; then
        echo "Detected SDKMAN, trying to get JDK path..."
        local sdkman_java_home="$SDKMAN_DIR/candidates/java/$JAVA_VERSION"
        if [ -d "$sdkman_java_home/jmods" ]; then
            jdk_mods_path="$sdkman_java_home/jmods"
            echo "Found jmods via SDKMAN: $jdk_mods_path"
            echo "$jdk_mods_path"
            return 0
        fi
    fi
    
    # 方法4：从java命令路径推断（传统方法）
    echo "Trying to infer JDK path from java command location..."
    local java_path=$(which java 2>/dev/null)
    if [ -n "$java_path" ]; then
        # 解析符号链接获取真实路径
        java_path=$(readlink -f "$java_path" 2>/dev/null || echo "$java_path")
        echo "Java command path: $java_path"
        
        # 跳过jenv/sdkman等版本管理器的shim路径
        if [[ "$java_path" == *"/.jenv/"* ]] || [[ "$java_path" == *"/.sdkman/"* ]]; then
            echo "Detected version manager shim, skipping path inference"
        else
            # 从 /path/to/jdk/bin/java 推断 /path/to/jdk/jmods
            local jdk_home=$(dirname $(dirname "$java_path"))
            echo "Inferred JDK home: $jdk_home"
            if [ -d "$jdk_home/jmods" ]; then
                jdk_mods_path="$jdk_home/jmods"
                echo "Found jmods via path inference: $jdk_mods_path"
                echo "$jdk_mods_path"
                return 0
            fi
        fi
    fi
    
    # 方法5：常见的macOS JDK路径
    echo "Trying common macOS JDK paths..."
    local common_paths=(
        "/Library/Java/JavaVirtualMachines/*/Contents/Home"
        "/System/Library/Java/JavaVirtualMachines/*/Contents/Home"
        "/usr/lib/jvm/*/jmods"
    )
    
    for path_pattern in "${common_paths[@]}"; do
        for potential_path in $path_pattern; do
            if [ -d "$potential_path/jmods" ]; then
                jdk_mods_path="$potential_path/jmods"
                echo "Found jmods via common path: $jdk_mods_path"
                echo "$jdk_mods_path"
                return 0
            fi
        done
    done
    
    echo "No JDK jmods directory found"
    echo ""
}

# 检测JDK模块路径
echo "=== JDK Detection Debug Info ==="
echo "JAVA_HOME: ${JAVA_HOME:-'(not set)'}"
echo "which java: $(which java 2>/dev/null || echo '(not found)')"
echo "java -version: $(java -version 2>&1 | head -1 || echo '(failed)')"

JDK_MODS_PATH=$(detect_jdk_mods_path)
if [ -z "$JDK_MODS_PATH" ]; then
    echo "================================="
    echo "Error: Cannot detect JDK jmods path."
    echo ""
    echo "Possible solutions:"
    echo "1. Set JAVA_HOME environment variable:"
    echo "   export JAVA_HOME=/path/to/your/jdk"
    echo ""
    echo "2. For jenv users, ensure JDK (not JRE) is installed:"
    echo "   jenv versions  # check available versions"
    echo "   jenv prefix    # show current JDK path"
    echo ""
    echo "3. For macOS users, install JDK from Oracle or OpenJDK:"
    echo "   brew install openjdk@17"
    echo ""
    echo "4. Verify jmods directory exists in your JDK:"
    echo "   ls -la \$JAVA_HOME/jmods"
    echo ""
    echo "Current detection attempts:"
    echo "- JAVA_HOME: ${JAVA_HOME:-'(not set)'}"
    echo "- java command: $(which java 2>/dev/null || echo '(not found)')"
    if command -v jenv > /dev/null 2>&1; then
        echo "- jenv prefix: $(jenv prefix 2>/dev/null || echo '(failed)')"
    fi
    echo "================================="
    exit 1
fi

echo "Using JDK modules path: $JDK_MODS_PATH"

CUSTOM_JRE_OUTPUT="$PROJECT_ROOT/custom-jre"

# 检查 JDK 工具是否存在
if ! command -v jdeps > /dev/null; then
  echo "Error: jdeps not found! Please ensure JDK is installed and added to PATH."
  exit 1
fi

if ! command -v jlink > /dev/null; then
  echo "Error: jlink not found! Please ensure JDK is installed and added to PATH."
  exit 1
fi

# 检查目录是否存在
for dir in "$LIBS_DIR" "$STARTERS_DIR" "$MODULES_DIR" "$PLUGINS_DIR"; do
    if [ ! -d "$dir" ]; then
        echo "Warning: Directory $dir does not exist, skipping..."
    fi
done

# 临时文件，用于存储 jdeps 输出
TEMP_MODULES_FILE=$(mktemp)

# 检查是否有JAR文件
JAR_COUNT=$(find "$LIBS_DIR" "$STARTERS_DIR" "$MODULES_DIR" "$PLUGINS_DIR" -name "*.jar" 2>/dev/null | wc -l)
if [ "$JAR_COUNT" -eq 0 ]; then
    echo "Error: No JAR files found in the specified directories."
    rm -f "$TEMP_MODULES_FILE"
    exit 1
fi

# 查找所有 JAR 文件并分析依赖（修复while循环问题）
echo "Analyzing JAR dependencies using jdeps..."
echo "Found $JAR_COUNT JAR files to analyze..."

# 使用for循环替代while循环，避免子shell问题
for JAR_PATH in $(find "$LIBS_DIR" "$STARTERS_DIR" "$MODULES_DIR" "$PLUGINS_DIR" -name "*.jar" 2>/dev/null); do
    echo "Processing: $JAR_PATH"
    # 使用 jdeps 分析每个 JAR 的模块依赖，并追加到临时文件
    jdeps --ignore-missing-deps --multi-release 17 --print-module-deps "$JAR_PATH" >> "$TEMP_MODULES_FILE" 2>/dev/null
done

# 汇总所有模块依赖并去重
echo "Extracting unique modules..."
UNIQUE_MODULES=$(cat "$TEMP_MODULES_FILE" | tr ',' '\n' | sort | uniq | grep -v '^$' | tr '\n' ',' | sed 's/,$//')

# 清理临时文件
rm -f "$TEMP_MODULES_FILE"

# 如果没有模块依赖，报错退出
if [ -z "$UNIQUE_MODULES" ]; then
  echo "Error: No modules found to include in the custom JRE."
  echo "This might indicate that the JAR files don't use the module system."
  exit 1
fi

# 打印依赖的模块
echo "Modules to be included in custom JRE: $UNIQUE_MODULES"

# 创建自定义 JRE
echo "Generating custom JRE with jlink..."
rm -rf "$CUSTOM_JRE_OUTPUT"
jlink \
  --module-path "$JDK_MODS_PATH" \
  --add-modules "$UNIQUE_MODULES" \
  --output "$CUSTOM_JRE_OUTPUT" \
  --strip-debug \
  --compress=2

# 检查生成结果
if [ $? -eq 0 ]; then
  echo "Custom JRE generated successfully at $CUSTOM_JRE_OUTPUT"
  echo "JRE size: $(du -sh "$CUSTOM_JRE_OUTPUT" | cut -f1)"
else
  echo "Error: Failed to generate custom JRE."
  exit 1
fi