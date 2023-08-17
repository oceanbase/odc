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
package com.oceanbase.odc.core.migrate.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.common.util.MapperUtils;
import com.oceanbase.odc.common.util.TopoOrderComparator;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.resource.factory.JarFileResourceUrlFactory;
import com.oceanbase.odc.core.migrate.resource.factory.LocalFileResourceUrlFactory;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceManager}
 *
 * @author yh263208
 * @date 2022-04-21 14:09
 * @since ODC_release_3.3.1
 */
@Slf4j
public class ResourceManager {

    private static Integer PREFIX = null;
    private static final String REF_FILE_FIELD_NAME = "ref_file";
    @Getter
    private final LinkedList<URL> resourceUrls;
    private final Map<URL, ResourceSpec> url2Resource = new HashMap<>();

    public ResourceManager(List<String> locations) throws IOException {
        this(new HashMap<>(), locations);
    }

    public ResourceManager(Map<String, ?> parameters, String... locations) throws IOException {
        this(parameters, Arrays.asList(locations));
    }

    public ResourceManager(String... locations) throws IOException {
        this(Arrays.asList(locations));
    }

    /**
     * 只有在 ODC 迁移框架中才能使用该构造器，该构造器中去掉了资源文件拓扑排序的相关逻辑。如果该构造函数被用在其他场景中可能导致资源文件执行顺序不对而导致错误。在 ODC
     * 迁移过程中资源文件的执行顺序是由迁移框架控制的，因此不用进行拓扑排序。
     */
    public ResourceManager(Map<String, ?> variables, Set<URL> urls) throws IOException {
        for (URL url : urls) {
            StringSubstitutor substitutor = new StringSubstitutor(variables);
            ResourceSpec entity = YamlUtils.from(
                    substitutor.replace(IOUtils.toString(url)), ResourceSpec.class);
            url2Resource.putIfAbsent(url, entity);
        }
        resourceUrls = new LinkedList<>(url2Resource.keySet());
    }

    public ResourceManager(Map<String, ?> variables, List<String> locations) throws IOException {
        Set<URL> urls = getResourceUrls(locations);
        TopoOrderComparator<URL> comparator = new TopoOrderComparator<>();
        for (URL url : urls) {
            log.info("Read resource content, path={}", getShortFilePath(url));
            StringSubstitutor substitutor = new StringSubstitutor(variables);
            ResourceSpec entity = YamlUtils.from(
                    substitutor.replace(IOUtils.toString(url)), ResourceSpec.class);
            Verifiable.verifyField(entity);

            Set<URL> reliedUrls = getRelyResourceFileUrls(entity, urls);
            /**
             * 允许引用自身，但是为了防止拓扑排序报错，这里需要去掉自己
             */
            reliedUrls.remove(url);
            comparator.addAll(url, reliedUrls);
            url2Resource.putIfAbsent(url, entity);
        }
        resourceUrls = new LinkedList<>(url2Resource.keySet());
        ListUtils.sortByTopoOrder(resourceUrls, (o1, o2) -> -comparator.compare(o1, o2));
    }

    public static String getShortFilePath(@NonNull URL url) {
        return url.toString().substring(getPrefixLength());
    }

    public ResourceSpec findByUrl(@NonNull URL resourceUrl) {
        return url2Resource.get(resourceUrl);
    }

    public List<ResourceSpec> findBySuffix(@NonNull String suffix) {
        return findBySuffix(url2Resource, suffix);
    }

    private <T> List<T> findBySuffix(Map<URL, T> map, String suffix) {
        return map.keySet().stream().filter(s -> s.toString().endsWith(suffix))
                .map(map::get).collect(Collectors.toList());
    }

    private synchronized static int getPrefixLength() {
        if (PREFIX != null) {
            return PREFIX;
        }
        URL prefix = ResourceManager.class.getClassLoader().getResource("");
        if (prefix != null) {
            int count = 0;
            try {
                URI uri = prefix.toURI();
                if ("jar".equals(uri.getScheme())) {
                    count = 1;
                }
            } catch (URISyntaxException e) {
                log.warn("Failed to parse the url, url={}", prefix);
            }
            PREFIX = prefix.toString().length() - count;
        }
        return PREFIX;
    }

    @SuppressWarnings("all")
    private Set<URL> getRelyResourceFileUrls(ResourceSpec entity, Set<URL> targets) {
        Object values = MapperUtils.get(entity, (prefix, current) -> {
            if (current == null) {
                return false;
            }
            return REF_FILE_FIELD_NAME.equals(current.toString());
        });
        if (values == null) {
            return new HashSet<>();
        }
        if (values instanceof List) {
            return ((List<String>) values).stream().filter(Objects::nonNull)
                    .map(s -> getRelyUrl(targets, s)).collect(Collectors.toSet());
        }
        URL url = getRelyUrl(targets, values.toString());
        return new HashSet<>(Collections.singletonList(url));
    }

    private URL getRelyUrl(Set<URL> targets, String suffix) {
        List<URL> files = targets.stream().filter(file -> file.toString().endsWith(suffix))
                .collect(Collectors.toList());
        if (files.isEmpty()) {
            throw new IllegalStateException("File not found " + suffix);
        } else if (files.size() > 1) {
            throw new IllegalStateException("Duplicated files " + suffix);
        }
        return files.get(0);
    }

    public static Set<URL> getResourceUrls(List<String> locations) throws IOException {
        Set<URL> targets = new HashSet<>();
        for (String location : locations) {
            URL url = ResourceManager.class.getClassLoader().getResource(location);
            if (url == null) {
                throw new FileNotFoundException("Location is not found " + location);
            }
            URI uri;
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                log.warn("Failed to get uri, url={}", url.toString(), e);
                throw new IllegalStateException(e);
            }
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                LocalFileResourceUrlFactory factory = new LocalFileResourceUrlFactory(url, new YamlFilePredicate());
                targets.addAll(factory.generateResourceUrls());
            } else if ("jar".equals(scheme)) {
                JarFileResourceUrlFactory factory = new JarFileResourceUrlFactory(url, new YamlEntryPredicate());
                targets.addAll(factory.generateResourceUrls());
            } else {
                throw new IllegalArgumentException("UnSupported scheme " + scheme);
            }
        }
        return targets;
    }

    static class YamlFilePredicate implements Predicate<File> {
        @Override
        public boolean test(File file) {
            return file.isFile() && (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"));
        }
    }

    static class YamlEntryPredicate implements Predicate<ZipEntry> {
        @Override
        public boolean test(ZipEntry entry) {
            return !entry.isDirectory() && (entry.getName().endsWith(".yml") || entry.getName().endsWith(".yaml"));
        }
    }

}
