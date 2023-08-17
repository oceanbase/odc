/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.util.SystemUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PluginProperties {

    public static final String PLUGIN_DIR_ENV_KEY = "ODC_PLUGIN_DIR";
    public static final String PLUGIN_DIR_KEY = "plugin.dir";
    public static final List<Path> DEFAULT_PLUGIN_DIRS = new ArrayList<>();

    static {
        DEFAULT_PLUGIN_DIRS.add(Paths.get("").toAbsolutePath().normalize().resolve("plugins"));
        // set plugins path as currentWorkdir/distribution/plugins
        DEFAULT_PLUGIN_DIRS.add(Paths.get("").toAbsolutePath().normalize().resolve("distribution/plugins"));
        // 默认安装目录
        DEFAULT_PLUGIN_DIRS.add(new File("/opt/odc/plugins").toPath());
    }

    public List<Path> getPluginDirs() {
        String settings = SystemUtils.getEnvOrProperty(PLUGIN_DIR_KEY);
        if (StringUtils.isNotEmpty(settings)) {
            return Collections.singletonList(getDir(settings));
        }
        settings = SystemUtils.getEnvOrProperty(PLUGIN_DIR_ENV_KEY);
        if (StringUtils.isNotEmpty(settings)) {
            return Collections.singletonList(getDir(settings));
        }
        Optional<Path> optional = DEFAULT_PLUGIN_DIRS.stream().filter(this::filterDir).findFirst();
        return optional.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    private Path getDir(String pluginPath) {
        Path path = new File(pluginPath).toPath();
        if (!filterDir(path)) {
            throw new IllegalArgumentException("Illegal plugin path, " + pluginPath);
        }
        return path;
    }

    private boolean filterDir(Path pluginPath) {
        if (pluginPath != null && Files.exists(pluginPath)) {
            log.info("Valid plugin path {} ", pluginPath.getParent());
            File[] files = pluginPath.toFile().listFiles();
            if (files != null) {
                String fileNames = Arrays.stream(files).map(File::getName)
                        .collect(Collectors.joining(","));
                log.info("Found files {} in plugin path {} ", fileNames, pluginPath.getParent());
            } else {
                log.info("Empty file in plugin path {} ", pluginPath.getParent());
            }
            if (files != null && files.length > 0) {
                return Arrays.stream(files).anyMatch(f -> f.getName().endsWith("jar"));
            }
        }
        return false;
    }

}
