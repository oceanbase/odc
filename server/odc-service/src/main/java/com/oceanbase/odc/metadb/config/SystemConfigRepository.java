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
package com.oceanbase.odc.metadb.config;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.PreConditions;

public interface SystemConfigRepository extends OdcJpaRepository<SystemConfigEntity, Long> {

    default List<SystemConfigEntity> queryByKeyPrefix(String keyPrefix) {
        PreConditions.notNull(keyPrefix, "keyPrefix");
        String sql = "SELECT `application`, `profile`, `key`, `value`, `create_time`, `update_time`, `description` "
                + "FROM `config_system_configuration` "
                + "WHERE `application`='odc' AND `profile`='default' AND `label`='master' AND `key` LIKE ?";
        return getJdbcTemplate().query(sql, new BeanPropertyRowMapper<>(SystemConfigEntity.class), keyPrefix + "%");
    }

    default SystemConfigEntity queryByKey(String key) {
        PreConditions.notEmpty(key, "key");
        String sql = "SELECT `application`, `profile`, `key`, `value`, `create_time`, `update_time`, `description` "
                + "FROM `config_system_configuration` "
                + "WHERE `application`='odc' AND `profile`='default' AND `label`='master' AND `key` = ?";
        return getJdbcTemplate().queryForObject(sql, new BeanPropertyRowMapper<>(SystemConfigEntity.class), key);
    }

    default int insert(SystemConfigEntity entity) {
        PreConditions.notNull(entity, "systemConfigEntity");
        String sql = "INSERT INTO `config_system_configuration`(`key`, `value`, `description`)"
                + " VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `id`=`id`";
        return getJdbcTemplate().update(sql, entity.getKey(), entity.getValue(), entity.getDescription());
    }

    default int upsert(SystemConfigEntity entity) {
        PreConditions.notNull(entity, "systemConfigEntity");
        String sql = "INSERT INTO `config_system_configuration`(`key`, `value`, `description`)"
                + " VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `value`=?";
        return getJdbcTemplate().update(sql, entity.getKey(), entity.getValue(), entity.getDescription(),
                entity.getValue());
    }

    List<SystemConfigEntity> findByKeyLike(String key);

    SystemConfigEntity findByKey(String key);


}
