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
package com.oceanbase.odc.core.migrate;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.ResourceUtils;
import com.oceanbase.odc.common.util.ResourceUtils.ResourceInfo;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.common.util.VersionUtils.Version;
import com.oceanbase.odc.core.migrate.ResourceMigrator.ResourceMigrateMetaInfo;
import com.oceanbase.odc.core.migrate.resource.ResourceManager;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * metadb migrate entry
 * 
 * @author yizhou.xw
 * @version : Migrates.java, v 0.1 2021-03-23 16:23
 */
@Slf4j
public class Migrates {

    private final MigrateConfiguration configuration;
    private final SchemaHistoryRepository repository;
    private final Map<String, List<SchemaHistory>> version2Histories;
    private final List<ResourceMigrateMetaInfo> migrateMetas = new LinkedList<>();

    public Migrates(MigrateConfiguration configuration, SchemaHistoryRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
        this.version2Histories = repository.listSuccess().stream()
                .collect(Collectors.groupingBy(SchemaHistory::getVersion));
        initResourceManager(configuration);
    }

    public void migrate() {
        log.debug("migrate start");
        List<Migrator> allMigrators = new ArrayList<>(scanSql());
        allMigrators.addAll(scanJdbc());
        allMigrators.addAll(scanResource());

        validate(allMigrators);
        log.info("validate success");

        Map<String, List<Migrator>> version2Migratables =
                allMigrators.stream().collect(Collectors.groupingBy(Migrator::version));
        List<Version> sortedVersions =
                version2Migratables.keySet().stream().map(Version::new).sorted().collect(Collectors.toList());
        log.info("versionCount={}", sortedVersions.size());

        degradeCheck(sortedVersions.get(sortedVersions.size() - 1).getVersion());
        for (Version version : sortedVersions) {
            log.debug("version={}", version);
            List<Migrator> migratables = version2Migratables.get(version.getVersion());
            List<Migrator> versionedMigratables =
                    migratables.stream().filter(m -> m.behavior() == Behavior.VERSIONED).collect(Collectors.toList());
            for (Migrator migratable : versionedMigratables) {
                Optional<SchemaHistory> history = getHistory(migratable);
                if (history.isPresent()) {
                    log.info("skip migrated versioned migrator, version={}, script={}",
                            migratable.version(), migratable.script());
                } else {
                    migrate(migratable);
                }
            }
            List<Migrator> repeatableMigratables =
                    migratables.stream().filter(m -> m.behavior() == Behavior.REPEATABLE).collect(Collectors.toList());
            for (Migrator migratable : repeatableMigratables) {
                if (migratable.ignoreChecksum()) {
                    log.info("ignore checksum and start to migrate directly, version={}, script={}",
                            migratable.version(), migratable.script());
                    migrate(migratable);
                } else {
                    SchemaHistory history = getHistory(migratable).orElse(new SchemaHistory());
                    if (StringUtils.equalsIgnoreCase(history.getChecksum(), migratable.checksum())) {
                        log.info("skip checksum matched repeatable migrator, version={}, script={}",
                                migratable.version(), migratable.script());
                    } else {
                        migrate(migratable);
                    }
                }
            }
        }
        log.debug("migrate done");
    }

    private void degradeCheck(String currentVersion) {
        log.info("Version check");
        Optional<Version> version = version2Histories.keySet().stream().map(Version::new).max(Version::compareTo);
        if (version.isPresent()) {
            String historyMaximumVersion = version.get().getVersion();
            if (VersionUtils.isGreaterThan(historyMaximumVersion, currentVersion)) {
                throw new RuntimeException(String.format(
                        "Software degrade is not allowed, please check your ODC version which should be greater than or equal to %s",
                        historyMaximumVersion));
            }
        }
    }

    private void initResourceManager(MigrateConfiguration configuration) {
        List<ResourceConfig> resourceConfigs = configuration.getResourceConfigs();
        if (resourceConfigs.size() == 0) {
            return;
        }
        log.info("Begin load resource file, resourceConfigCount={}", resourceConfigs.size());
        boolean match = resourceConfigs.stream().anyMatch(
                c -> !CollectionUtils.isEmpty(c.getResourceLocations()));
        if (match) {
            throw new IllegalArgumentException("Resource locations can only be set by migrate config");
        }
        try {
            Set<URL> urls = ResourceManager.getResourceUrls(configuration.getResourceLocations());
            for (ResourceConfig resourceConfig : resourceConfigs) {
                resourceConfig.setDataSource(configuration.getDataSource());
                ResourceManager manager = new ResourceManager(resourceConfig.getVariables(), urls);
                ResourceMigrateMetaInfo meta = new ResourceMigrateMetaInfo(resourceConfig, manager);
                migrateMetas.add(meta);
            }
            log.info("Load resource file succeed");
        } catch (IOException e) {
            log.warn("Failed to load resource file", e);
            throw new IllegalStateException(e);
        }
    }

    private void validate(List<Migrator> allMigrators) {
        long distinctCount = allMigrators.stream().map(migrator -> migrator.version() + migrator.script())
                .distinct().count();
        Validate.isTrue(distinctCount == allMigrators.size(),
                String.format("duplicated version.script exists, total=%d, distinct=%d",
                        allMigrators.size(), distinctCount));
        List<Migrator> versionedMigrators = allMigrators.stream()
                .filter(migratable -> migratable.behavior() == Behavior.VERSIONED).collect(Collectors.toList());
        long distinctVersionCount = versionedMigrators.stream().map(Migrator::version).distinct().count();
        Validate.isTrue(distinctVersionCount == versionedMigrators.size(),
                String.format("duplicated versioned script exists, total=%d, distinct=%d",
                        versionedMigrators.size(), distinctVersionCount));
    }

    private Optional<SchemaHistory> getHistory(Migrator migratable) {
        String version = migratable.version();
        String script = migratable.script();
        List<SchemaHistory> histories = version2Histories.get(version);
        if (CollectionUtils.isEmpty(histories)) {
            return Optional.empty();
        }
        for (SchemaHistory history : histories) {
            if (StringUtils.equalsIgnoreCase(script, history.getScript())) {
                return Optional.of(history);
            }
        }
        return Optional.empty();
    }

    private void migrate(Migrator migratable) {
        String migrateIdentify = String.format("%s %s %s %s",
                migratable.behavior(), migratable.type(), migratable.version(), migratable.description());
        log.info("migrate {} start...", migrateIdentify);

        if (configuration.isDryRun()) {
            log.info("migrate {} done, DRY RUN!", migrateIdentify);
            return;
        }
        String initVersion = configuration.getInitVersion();
        long start = System.currentTimeMillis();
        boolean result = false;
        try {
            if (StringUtils.isNotEmpty(initVersion)
                    && Behavior.VERSIONED == migratable.behavior()
                    && VersionUtils.isGreaterThanOrEqualsTo(initVersion, migratable.version())) {
                log.info("skip less than initVersion, initVersion={}, migrateIdentify={}",
                        initVersion, migrateIdentify);
                result = true;
            } else {
                result = migratable.doMigrate();
            }
        } finally {
            long end = System.currentTimeMillis();
            long durationMillis = end - start;
            SchemaHistory history = SchemaHistory.fromMigratable(migratable);
            history.setSuccess(result);
            history.setExecutionMillis(durationMillis);
            history.setInstalledOn(Timestamp.from(Instant.ofEpochMilli(end)));

            log.info("migrate {} done, result={}", migrateIdentify, result);
            repository.create(history);
        }
    }

    private List<Migrator> scanResource() {
        List<String> resourceLocations = configuration.getResourceLocations();
        return resourceLocations.stream()
                .flatMap(t -> scanSqlFromLocation(t, i -> StringUtils.endsWithIgnoreCase(i.getResourceName(), ".yaml")
                        || StringUtils.endsWithIgnoreCase(i.getResourceName(), ".yml"),
                        i -> new ResourceMigrator(i.getResourceName(), i.getInputStream(), migrateMetas)).stream())
                .collect(Collectors.toList());
    }

    private List<Migrator> scanSql() {
        List<String> resourceLocations = configuration.getResourceLocations();
        DataSource dataSource = configuration.getDataSource();
        return resourceLocations.stream()
                .flatMap(t -> scanSqlFromLocation(t, i -> StringUtils.endsWithIgnoreCase(i.getResourceName(), ".sql"),
                        info -> new SqlMigrator(info.getResourceName(), info.getInputStream(), dataSource)).stream())
                .collect(Collectors.toList());
    }

    private List<Migrator> scanSqlFromLocation(String resourceLocation,
            Predicate<ResourceInfo> predicate, Function<ResourceInfo, Migrator> function) {
        List<ResourceInfo> resourceInfos = ResourceUtils.listResourcesFromDirectory(resourceLocation);
        log.info("scanSqlFromLocation, resourceLocation={}, resourceCount={}",
                resourceLocation, resourceInfos.size());
        resourceInfos = resourceInfos.stream().filter(predicate).collect(Collectors.toList());
        return resourceInfos.stream().map(function).collect(Collectors.toList());
    }

    private List<Migrator> scanJdbc() {
        List<String> basePackages = configuration.getBasePackages();
        if (CollectionUtils.isEmpty(basePackages)) {
            log.info("basePackages not set, skip scan Java classes");
            return Collections.emptyList();
        }
        return basePackages.stream()
                .flatMap(basePackage -> scanJdbcFromBasePackage(basePackage).stream())
                .collect(Collectors.toList());
    }

    private List<Migrator> scanJdbcFromBasePackage(String basePackage) {
        List<Migrator> list = new ArrayList<>();
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends JdbcMigratable>> subTypes;
        try {
            subTypes = reflections.getSubTypesOf(JdbcMigratable.class);
        } catch (ReflectionsException e) {
            throw new RuntimeException(
                    String.format("no JdbcMigratable implementation found under basePackages '%s'", basePackage), e);
        }
        for (Class<? extends JdbcMigratable> classType : subTypes) {
            JdbcMigrator jdbcMigrator = new JdbcMigrator(classType, configuration.getDataSource());
            list.add(jdbcMigrator);
        }
        if (list.isEmpty()) {
            throw new RuntimeException(
                    String.format("no JdbcMigratable implementation found under basePackages '%s'", basePackage));
        }
        return list;
    }
}
