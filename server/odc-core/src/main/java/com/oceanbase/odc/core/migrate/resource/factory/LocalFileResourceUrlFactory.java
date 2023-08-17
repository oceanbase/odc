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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LocalFileResourceUrlFactory}
 *
 * @author yh263208
 * @date 2022-04-29 13:55
 * @since ODC_release_3.3.1
 * @see com.oceanbase.odc.core.migrate.resource.factory.ResourceUrlFactory
 */
@Slf4j
public class LocalFileResourceUrlFactory implements ResourceUrlFactory {

    private final File target;
    private final Predicate<File> predicate;

    public LocalFileResourceUrlFactory(@NonNull URL url, @NonNull Predicate<File> predicate)
            throws FileNotFoundException {
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (!"file".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Scheme is illegal " + uri.getScheme());
        }
        this.target = new File(uri);
        if (!this.target.exists()) {
            throw new FileNotFoundException("File is not found " + uri);
        }
        this.predicate = predicate;
    }

    @Override
    public List<URL> generateResourceUrls() throws IOException {
        if (target.isFile() && predicate.test(target)) {
            return Collections.singletonList(new URL(target.toURI().toString()));
        } else if (target.isFile()) {
            return Collections.emptyList();
        }
        return getAllFiles(target, predicate).stream().map(file -> {
            try {
                return new URL(file.toURI().toString());
            } catch (MalformedURLException e) {
                log.warn("Failed to new URL", e);
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());
    }

    private List<File> getAllFiles(File rootDir, Predicate<File> predicate) throws FileNotFoundException {
        if (!rootDir.exists()) {
            throw new FileNotFoundException(rootDir.getAbsolutePath());
        }
        List<File> returnVal = new LinkedList<>();
        if (predicate.test(rootDir)) {
            returnVal.add(rootDir);
        }
        if (rootDir.isFile()) {
            return returnVal;
        } else {
            for (File subFile : rootDir.listFiles()) {
                if (subFile.isFile()) {
                    if (predicate.test(subFile)) {
                        returnVal.add(subFile);
                    }
                } else {
                    returnVal.addAll(getAllFiles(subFile, predicate));
                }
            }
            return returnVal;
        }
    }

}
