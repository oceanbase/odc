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
 * @date 2021/11/26
 */

@Slf4j
@Migratable(version = "3.2.2.3", description = "migrate odc sql script text")
public class V3223SqlScriptTextMigrate implements JdbcMigratable {
    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Long> scriptIds = listScripts(jdbcTemplate);
        if (CollectionUtils.isEmpty(scriptIds)) {
            log.info("found no script need migrate script_text, skip it");
            return;
        }
        log.info("found script need migrate script_text, count={}", scriptIds.size());
        int migratedScriptCount = 0;
        for (Long id : scriptIds) {
            migratedScriptCount += updateScriptText(jdbcTemplate, id);
        }
        log.info("migrate tasks complete, migratedTaskCount={}", migratedScriptCount);
    }


    private int updateScriptText(JdbcTemplate jdbcTemplate, Long id) {
        String sql = "UPDATE `odc_sql_script` SET `script_text`=`script_text_old` where `id`=?;";
        return jdbcTemplate.update(sql, id);
    }

    private List<Long> listScripts(JdbcTemplate jdbcTemplate) {
        String sql = "select `id` from `odc_sql_script` where `script_text_old` is not null and `script_text` is null";
        return jdbcTemplate.queryForList(sql, Long.class);
    }
}
