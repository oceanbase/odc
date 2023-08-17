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
package com.oceanbase.odc.core.migrate.resource.factory;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link JarFileResourceUrlFactory}
 *
 * @author yh263208
 * @date 2022-04-29 12:13
 * @since ODC_release_3.3.1
 * @see ResourceUrlFactory
 */
@Slf4j
public class JarFileResourceUrlFactory implements ResourceUrlFactory {

    private final URL target;
    private final Predicate<ZipEntry> predicate;

    public JarFileResourceUrlFactory(@NonNull URL target, @NonNull Predicate<ZipEntry> predicate) {
        URI uri;
        try {
            uri = target.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (!"jar".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Scheme is illegal " + uri.getScheme());
        }
        this.target = target;
        this.predicate = predicate;
    }

    @Override
    public List<URL> generateResourceUrls() throws IOException {
        JarURLConnection connection = (JarURLConnection) this.target.openConnection();
        String entryName = connection.getEntryName() == null ? "" : connection.getEntryName();
        if (entryName.contains("!")) {
            entryName = entryName.replace("!", "");
        }
        JarFile jarFile = connection.getJarFile();
        if (log.isDebugEnabled()) {
            URL jarFileUrl = connection.getJarFileURL();
            log.debug("Jar file loaded, jarFileUrl={}, entryName={}", jarFileUrl, entryName);
        }
        Enumeration<? extends ZipEntry> enumeration = jarFile.entries();
        List<ZipEntry> targets = new LinkedList<>();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            if (zipEntry.getName().startsWith(entryName)) {
                targets.add(zipEntry);
            }
        }
        return targets.stream().filter(predicate).map(zipEntry -> {
            URL url = this.getClass().getClassLoader().getResource(zipEntry.getName());
            if (url == null) {
                throw new IllegalStateException("Failed to get resource," + zipEntry.getName());
            }
            return url;
        }).collect(Collectors.toList());
    }

}
