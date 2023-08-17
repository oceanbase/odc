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
 * @author wenniu.ly
 * @date 2021/11/24
 */
@Slf4j
@Migratable(version = "3.2.2.2", description = "migrate task info parameters")
public class V3222TaskInfoParametersMigrate implements JdbcMigratable {
    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Long> taskIds = listTasks(jdbcTemplate);
        if (CollectionUtils.isEmpty(taskIds)) {
            log.info("found no tasks need migrate parameters, skip it");
            return;
        }
        log.info("found tasks need migrate parameters, taskCount={}", taskIds.size());
        int migratedTaskCount = 0;
        for (Long id : taskIds) {
            migratedTaskCount += updateTaskParameters(jdbcTemplate, id);
        }
        log.info("migrate tasks complete, migratedTaskCount={}", migratedTaskCount);
    }

    private int updateTaskParameters(JdbcTemplate jdbcTemplate, Long id) {
        String sql = "UPDATE `odc_task_info` SET `parameters`=`parameters_old` where `id`=?;";
        return jdbcTemplate.update(sql, id);
    }

    private List<Long> listTasks(JdbcTemplate jdbcTemplate) {
        String sql = "select `id` from odc_task_info where parameters_old is not null and parameters is null";
        return jdbcTemplate.queryForList(sql, Long.class);
    }
}
