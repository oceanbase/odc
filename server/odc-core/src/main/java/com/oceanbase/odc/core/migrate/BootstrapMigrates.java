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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.common.util.VersionUtils.Version;

import lombok.extern.slf4j.Slf4j;

/**
 * metadb migrate entry
 * 
 * @author yizhou.xw
 * @version : Migrates.java, v 0.1 2021-03-23 16:23
 */
@Slf4j
public class BootstrapMigrates extends BaseMigrates {

    public static final String DEFAULT_TABLE = "migrate_schema_history";
    private final SchemaHistoryRepository repository;
    private final Map<String, List<SchemaHistory>> version2Histories;

    public BootstrapMigrates(MigrateConfiguration configuration) {
        super(configuration);
        this.repository = new SchemaHistoryRepository(DEFAULT_TABLE, configuration.getDataSource());
        this.version2Histories = repository.listSuccess().stream()
                .collect(Collectors.groupingBy(SchemaHistory::getVersion));
    }

    @Override
    protected boolean preHandle(Migrator migrator) {
        switch (migrator.behavior()) {
            case VERSIONED:
                Optional<SchemaHistory> history = getHistory(migrator);
                if (history.isPresent()) {
                    log.info("skip migrated versioned migrator, version={}, script={}",
                            migrator.version(), migrator.script());
                    return false;
                }
                return true;
            case REPEATABLE:
                if (migrator.ignoreChecksum()) {
                    log.info("ignore checksum and start to migrate directly, version={}, script={}",
                            migrator.version(), migrator.script());
                    return true;
                }
                String checkSum = getHistory(migrator).orElse(new SchemaHistory()).getChecksum();
                if (StringUtils.equalsIgnoreCase(checkSum, migrator.checksum())) {
                    log.info("skip checksum matched repeatable migrator, version={}, script={}",
                            migrator.version(), migrator.script());
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void preCheck(List<Migrator> allMigrators) {
        Map<String, List<Migrator>> version2Migratables =
                allMigrators.stream().collect(Collectors.groupingBy(Migrator::version));
        List<Version> sortedVersions =
                version2Migratables.keySet().stream().map(Version::new).sorted().collect(Collectors.toList());
        String currentVersion = sortedVersions.get(sortedVersions.size() - 1).getVersion();
        log.info("Version check");
        Optional<Version> version = version2Histories.keySet().stream().map(Version::new).max(Version::compareTo);
        if (!version.isPresent()) {
            return;
        }
        String maxVersion = version.get().getVersion();
        if (VersionUtils.isGreaterThan(maxVersion, currentVersion)) {
            throw new RuntimeException(String.format(
                    "Software degrade is not allowed, please check your ODC version which should be greater than or equal to %s",
                    maxVersion));
        }
    }

    @Override
    protected void afterCompletion(Migrator migrator, boolean result, long durationMillis) {
        long end = System.currentTimeMillis();
        SchemaHistory history = SchemaHistory.fromMigratable(migrator);
        history.setSuccess(result);
        history.setExecutionMillis(durationMillis);
        history.setInstalledOn(Timestamp.from(Instant.ofEpochMilli(end)));
        repository.create(history);
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

}
