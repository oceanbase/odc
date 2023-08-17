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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/7 下午3:28
 * @Description: [Migrate odc config_system_configuration value]
 */
@Slf4j
@Migratable(version = "3.2.3.4", description = "migrate config_system_configuration value")
public class V3234SystemConfigurationMigrate implements JdbcMigratable {
    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> configKeys = listConfigKeys(jdbcTemplate);
        if (CollectionUtils.isEmpty(configKeys)) {
            log.info("no value need to migrate in config_system_configuration");
            return;
        }
        log.info("found values need to migrate, count={}", configKeys.size());
        int migratedValueCount = 0;
        for (String key : configKeys) {
            migratedValueCount += updateConfigValue(jdbcTemplate, key);
        }
        log.info("migrate system configuration complete, migratedValueCount={}", migratedValueCount);
    }


    private int updateConfigValue(JdbcTemplate jdbcTemplate, String key) {
        String sql = "UPDATE `config_system_configuration` SET `value`=`value_deprecated` where `key`=?;";
        return jdbcTemplate.update(sql, key);
    }

    private List<String> listConfigKeys(JdbcTemplate jdbcTemplate) {
        String sql =
                "select `key` from `config_system_configuration` where `value_deprecated` is not null and `value` is null";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
