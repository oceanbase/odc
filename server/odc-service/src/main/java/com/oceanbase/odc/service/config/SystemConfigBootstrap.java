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
package com.oceanbase.odc.service.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.jdbc.JdbcTemplateUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * for persistent system configurations from environment ODC_APP_EXTRA_ARGS while starting up
 */
@Slf4j
@Component
@DependsOn("metadbMigrate")
public class SystemConfigBootstrap {
    private static final int BATCH_SIZE = 100;

    @Autowired
    protected DataSource dataSource;

    @PostConstruct
    public void bootstrap() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Configuration> configurations = readSystemConfigurationsFromEnvironment();
        upsertSystemConfigurations(jdbcTemplate, configurations);

    }

    private List<Configuration> readSystemConfigurationsFromEnvironment() {
        String odcAppExtraArgs = SystemUtils.getEnvOrProperty("ODC_APP_EXTRA_ARGS");
        if (StringUtils.isBlank(odcAppExtraArgs)) {
            return Collections.emptyList();
        }
        String[] configItems = odcAppExtraArgs.split("\\s+");
        if (configItems.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(configItems)
                .map(item -> item.trim().replaceFirst("\\-\\-", "").split("="))
                .filter(kv -> kv.length == 2)
                .map(kv -> new Configuration(StringUtils.trim(kv[0]), StringUtils.trim(kv[1])))
                .collect(Collectors.toList());
    }

    private void upsertSystemConfigurations(JdbcTemplate jdbcTemplate, List<Configuration> configurations) {
        if (CollectionUtils.isEmpty(configurations)) {
            return;
        }
        String sql = "INSERT INTO config_system_configuration (`key`, `value`) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE `value` = ?";
        int[][] results = jdbcTemplate.batchUpdate(sql, configurations, BATCH_SIZE,
                (ps, config) -> {
                    ps.setString(1, config.getKey());
                    ps.setString(2, config.getValue());
                    ps.setString(3, config.getValue());
                });
        int affectedRows = JdbcTemplateUtils.batchInsertAffectRowsWithBatchSize(results);
        log.info("System configurations are upserted successfully, size={}, affectedRows={}",
                configurations.size(), affectedRows);
    }

}
