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
package com.oceanbase.odc.server.starter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Starters}
 *
 * @author yh263208
 * @date 2023-07-14 17:35
 * @since ODC_release_4.2.0
 */
@Slf4j
public class Starters {

    public static void load() {
        String activeProfiles = getEnvOrSystemProperties("spring.profiles.active");
        if (StringUtils.isEmpty(activeProfiles)) {
            activeProfiles = getEnvOrSystemProperties("ODC_PROFILE_MODE");
            if (StringUtils.isEmpty(activeProfiles)) {
                activeProfiles = "alipay,jdbc";
            }
        }
        load(new HashSet<>(Arrays.asList(activeProfiles.split(","))));
    }

    public static void load(@NonNull Set<String> activeProfiles) {
        log.info("Starters is loading, activeProfiles={}", activeProfiles);
        List<URL> starterUrls = getStarters(activeProfiles);
        if (starterUrls.isEmpty()) {
            return;
        }
        try {
            URLClassLoader classLoader = (URLClassLoader) Starters.class.getClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            for (URL url : starterUrls) {
                method.invoke(classLoader, url);
                log.info("Starter has been added to classpath, url={}", url);
            }
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.warn("Failed to add starter to classpath", e);
            throw new IllegalStateException(e);
        }
    }

    private static String getEnvOrSystemProperties(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }
        return System.getenv(key);
    }

    private static List<URL> getStarters(Set<String> activeProfiles) {
        StarterProperties properties = new StarterProperties();
        return properties.getStarterDirs().stream().map(Path::toFile).flatMap(file -> {
            if (!file.exists()) {
                return Stream.empty();
            } else if (file.isFile()) {
                return Stream.of(file);
            } else if (file.isDirectory()) {
                return Arrays.stream(file.listFiles()).filter(File::isFile);
            }
            return Stream.empty();
        }).filter(filterStarter(activeProfiles)).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());
    }

    private static Predicate<File> filterStarter(Set<String> activeProfiles) {
        return file -> {
            if (!StringUtils.endsWith(file.getName(), "jar")) {
                return false;
            }
            try {
                JarFile jarFile = new JarFile(file);
                Manifest manifest = jarFile.getManifest();
                if (manifest == null || manifest.getMainAttributes() == null) {
                    return false;
                }
                String profiles = manifest.getMainAttributes().getValue("Starter-Profiles");
                if (StringUtils.isEmpty(profiles)) {
                    return false;
                }
                return CollectionUtils.containsAny(Arrays.asList(profiles.split(",")), activeProfiles);
            } catch (IOException e) {
                return false;
            }
        };
    }

}
