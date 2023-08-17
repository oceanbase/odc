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
package com.oceanbase.odc.migrate.jdbc.web;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.fastjson.JSONObject;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.service.connection.model.PropertiesKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "3.2.1.1", description = "set top flag migrate for connection")
public class V3211SetTopFlagMigrate implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = "select `id`,properties_json from `connect_connection` where is_set_top=0";
        String updateSql = "update `connect_connection` set is_set_top=? where id=?";
        jdbcTemplate.query(sql, resultSet -> {
            Long connectionId = Long.valueOf(resultSet.getString("id"));
            Map properties = JSONObject.parseObject(resultSet.getString("properties_json"), Map.class);
            if (properties == null) {
                return;
            }
            String value = String.valueOf(properties.get(PropertiesKeys.SET_TOP));
            if (!"true".equalsIgnoreCase(value)) {
                return;
            }
            int affectRows = jdbcTemplate.update(updateSql, true, connectionId);
            log.info("Connection needs to be on top, connectionId={}, properties={}, affectRows={}",
                    connectionId, properties, affectRows);
        });
    }

}
