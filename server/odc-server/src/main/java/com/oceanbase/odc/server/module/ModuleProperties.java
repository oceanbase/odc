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
package com.oceanbase.odc.server.module;

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

/**
 * {@link ModuleProperties}
 *
 * @author yh263208
 * @date 2024/08/20 14:33
 * @since ODC_release_4.3.2
 */
@Slf4j
public class ModuleProperties {

    public static final String MODULE_DIR_ENV_KEY = "ODC_MODULE_DIR";
    public static final String MODULE_DIR_KEY = "module.dir";
    public static final List<Path> MODULE_PLUGIN_DIRS = new ArrayList<>();

    static {
        MODULE_PLUGIN_DIRS.add(Paths.get("").toAbsolutePath().normalize().resolve("modules"));
        // set plugins path as currentWorkdir/distribution/plugins
        MODULE_PLUGIN_DIRS.add(Paths.get("").toAbsolutePath().normalize().resolve("distribution/modules"));
        // 默认安装目录
        MODULE_PLUGIN_DIRS.add(new File("/opt/odc/modules").toPath());
    }

    public List<Path> getModuleDirs() {
        String settings = SystemUtils.getEnvOrProperty(MODULE_DIR_KEY);
        if (StringUtils.isNotEmpty(settings)) {
            return Collections.singletonList(getDir(settings));
        }
        settings = SystemUtils.getEnvOrProperty(MODULE_DIR_ENV_KEY);
        if (StringUtils.isNotEmpty(settings)) {
            return Collections.singletonList(getDir(settings));
        }
        Optional<Path> optional = MODULE_PLUGIN_DIRS.stream().filter(this::filterDir).findFirst();
        return optional.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    private Path getDir(String modulePath) {
        Path path = new File(modulePath).toPath();
        if (!filterDir(path)) {
            throw new IllegalArgumentException("Illegal module path, " + modulePath);
        }
        return path;
    }

    private boolean filterDir(Path modulePath) {
        if (modulePath != null && Files.exists(modulePath)) {
            log.info("Valid module path {} ", modulePath.getParent());
            File[] files = modulePath.toFile().listFiles();
            if (files != null) {
                String fileNames = Arrays.stream(files).map(File::getName)
                        .collect(Collectors.joining(","));
                log.info("Found files {} in module path {} ", fileNames, modulePath.getParent());
            } else {
                log.info("Empty file in module path {} ", modulePath.getParent());
            }
            if (files != null && files.length > 0) {
                return Arrays.stream(files).anyMatch(f -> f.getName().endsWith("jar"));
            }
        }
        return false;
    }

}
