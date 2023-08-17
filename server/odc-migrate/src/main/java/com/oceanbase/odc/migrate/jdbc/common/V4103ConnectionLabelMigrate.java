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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.service.connection.model.PropertiesKeys;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link V4103ConnectionLabelMigrate}
 *
 * @author yh263208
 * @date 2022-11-30 18:21
 * @since ODC_release_4.1.0
 */
@Slf4j
@Migratable(version = "4.1.0.3", description = "Connection label migrate")
public class V4103ConnectionLabelMigrate implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String querySql =
                "SELECT `id`,`properties_json`,`creator_id` FROM `connect_connection` WHERE `visible_scope`='PRIVATE'";
        List<Object[]> params = jdbcTemplate.query(querySql, (rs, rowNum) -> {
            String json = rs.getString("properties_json");
            if (StringUtils.isBlank(json)) {
                return null;
            }
            Map<String, String> map = JsonUtils.fromJson(json, new TypeReference<Map<String, String>>() {});
            if (map == null) {
                return null;
            }
            String labelIdStr = map.get(PropertiesKeys.LABEL_ID);
            if (StringUtils.isBlank(labelIdStr)) {
                return null;
            }
            return new Object[] {rs.getLong("id"), Long.valueOf(labelIdStr), rs.getLong("creator_id")};
        }).stream().filter(Objects::nonNull).collect(Collectors.toList());

        String insertSql =
                "INSERT INTO `connect_connection_label`(`connection_id`,`label_id`,`user_id`) VALUES (?,?,?)";
        jdbcTemplate.batchUpdate(insertSql, params);
        log.info("Migrate connection label succeed");
    }

}
