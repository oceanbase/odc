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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;

@Component
public class UserConfigDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<UserConfigEntity> queryByUserId(Long userId) {
        PreConditions.notNull(userId, "userId");
        String sql = "SELECT user_id, `key`, `value`, create_time, update_time, description"
                + " FROM config_user_configuration WHERE user_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserConfigEntity.class), userId);
    }

    public UserConfigEntity queryByUserIdAndKey(Long userId, String key) {
        PreConditions.notNull(userId, "userId");
        PreConditions.notBlank(key, "key");
        String sql = "SELECT user_id, `key`, `value`, create_time, update_time, description"
                + " FROM config_user_configuration WHERE user_id = ? AND `key` = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(UserConfigEntity.class), userId, key);
    }

    public int batchUpsert(List<UserConfigEntity> entities) {
        PreConditions.notEmpty(entities, "entities");
        String sql = "INSERT INTO config_user_configuration(user_id, `key`, `value`, description)"
                + " VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = ?";
        int[] rets = jdbcTemplate.batchUpdate(sql, entities.stream().map(
                entity -> new Object[] {entity.getUserId(), entity.getKey(), entity.getValue(),
                        entity.getDescription(), entity.getValue()})
                .collect(Collectors.toList()));
        return Arrays.stream(rets).sum();
    }

}
