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

import static com.oceanbase.odc.server.PluginSpringApplication.addUrlToClassLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link Modules}
 *
 * @author yh263208
 * @date 2024/08/20 14:37
 * @since ODC_release_4.3.2
 */
@Slf4j
public class Modules {

    public static void load() {
        log.info("Modules is loading...");
        List<URL> moduleUrls = getModules();
        if (moduleUrls.isEmpty()) {
            return;
        }
        try {
            addUrlToClassLoader(moduleUrls, Modules.class.getClassLoader());
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException
                | NoSuchFieldException e) {
            log.warn("Failed to add module to classpath", e);
            throw new IllegalStateException(e);
        }
    }

    public static List<URL> getModules() {
        ModuleProperties properties = new ModuleProperties();
        return properties.getModuleDirs().stream().map(Path::toFile).flatMap(file -> {
            if (!file.exists()) {
                return Stream.empty();
            } else if (file.isFile()) {
                return Stream.of(file);
            } else if (file.isDirectory()) {
                return Arrays.stream(file.listFiles()).filter(File::isFile);
            }
            return Stream.empty();
        }).filter(file -> StringUtils.endsWith(file.getName(), "jar")).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());
    }

}
