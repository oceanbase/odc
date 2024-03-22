/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.metadb.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;

@Component
public class SystemConfigDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<SystemConfigEntity> queryByKeyPrefix(String keyPrefix) {
        PreConditions.notNull(keyPrefix, "keyPrefix");
        String sql = "SELECT `application`, `profile`, `key`, `value`, `create_time`, `update_time`, `description` "
                + "FROM `config_system_configuration` "
                + "WHERE `application`='odc' AND `profile`='default' AND `label`='master' AND `key` LIKE ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(SystemConfigEntity.class), keyPrefix + "%");
    }

    public SystemConfigEntity queryByKey(String key) {
        PreConditions.notEmpty(key, "ket");
        String sql = "SELECT `application`, `profile`, `key`, `value`, `create_time`, `update_time`, `description` "
                + "FROM `config_system_configuration` "
                + "WHERE `application`='odc' AND `profile`='default' AND `label`='master' AND `key` = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(SystemConfigEntity.class), key);
    }

    public int upsert(SystemConfigEntity entity) {
        PreConditions.notNull(entity, "systemConfigEntity");
        String sql = "INSERT INTO `config_system_configuration`(`key`, `value`, `description`)"
                + " VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `value`=?";
        return jdbcTemplate.update(sql, entity.getKey(), entity.getValue(), entity.getDescription(), entity.getValue());
    }
}
