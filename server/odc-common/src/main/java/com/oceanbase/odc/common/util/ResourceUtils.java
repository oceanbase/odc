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
package com.oceanbase.odc.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : ResourceUtils.java, v 0.1 2021-04-05 20:04
 */
@Slf4j
public class ResourceUtils {

    public static File loadDirectory(String resourceLocation) {
        Validate.notEmpty(resourceLocation, "parameter resourceLocation may not be null");
        URL url = ResourceUtils.class.getClassLoader().getResource(resourceLocation);
        if (url == null) {
            throw new RuntimeException(String.format("folder '%s' not exists", resourceLocation));
        }
        File folder = new File(url.getPath());
        if (!folder.exists()) {
            throw new RuntimeException(String.format("folder '%s' not exists", resourceLocation));
        }
        if (!folder.isDirectory()) {
            throw new RuntimeException(String.format("location '%s' not a directory", resourceLocation));
        }
        return folder;
    }

    public static InputStream getFileAsStream(String resourceLocation) {
        Validate.notEmpty(resourceLocation, "parameter resourceLocation may not be null");
        InputStream stream = ResourceUtils.class.getClassLoader().getResourceAsStream(resourceLocation);
        if (stream == null) {
            throw new RuntimeException(String.format("file '%s' not exists", resourceLocation));
        }
        return stream;
    }

    /**
     * while in jar file, the path of location may looks like:
     * jar:file:/home/admin/ob-odc/lib/odc-web-starter-2.4.1-SNAPSHOT-executable.jar!/BOOT-INF/classes!/migrate/common
     */
    public static List<ResourceInfo> listResourcesFromDirectory(String location) {
        Validate.notEmpty(location, "parameter location may not be null");
        URL url = ResourceUtils.class.getClassLoader().getResource(location);
        if (url == null) {
            throw new RuntimeException(String.format("folder '%s' not exists", location));
        }
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    String.format("cannot convert url to uri, folder '%s' may not exists", location), e);
        }
        if (uri.getScheme().contains("jar")) {
            return listResourcesFromJarPackage(url);
        }
        return listResourcesFromFileSystem(uri);
    }

    private static List<ResourceInfo> listResourcesFromFileSystem(URI uri) {
        Validate.notNull(uri, "cannot list resources, parameter uri may not be null");
        Path path = Paths.get(uri);
        List<ResourceInfo> resources = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);) {
            for (Path p : directoryStream) {
                File file = p.toFile();
                InputStream inputStream = new FileInputStream(file);
                resources.add(new ResourceInfo(file.getName(), inputStream));
            }
        } catch (IOException e) {
            throw new RuntimeException("iterator directory stream failed, it seems in IDE mode", e);
        }
        return resources;
    }

    private static List<ResourceInfo> listResourcesFromJarPackage(URL target) {
        try {
            JarURLConnection connection = (JarURLConnection) target.openConnection();
            String entryName = connection.getEntryName() == null ? "" : connection.getEntryName();
            if (entryName.contains("!")) {
                entryName = entryName.replace("!", "");
            }
            JarFile jarFile = connection.getJarFile();
            if (log.isDebugEnabled()) {
                URL jarFileUrl = connection.getJarFileURL();
                log.debug("Jar file loaded, jarFileUrl={}, entryName={}", jarFileUrl, entryName);
            }
            Enumeration<JarEntry> enumeration = jarFile.entries();
            List<ResourceInfo> resources = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                log.debug("file in jar, name={}", name);
                if (name.startsWith(entryName) && !jarEntry.isDirectory()) {
                    String fileName = FilenameUtils.getName(name);
                    InputStream inputStream = getFileAsStream(name);
                    resources.add(new ResourceInfo(fileName, inputStream));
                }
            }
            return resources;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read nested jar from jar", e);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceInfo {
        private String resourceName;
        private InputStream inputStream;
    }

}
