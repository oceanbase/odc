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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;

@Component
public class OrganizationConfigDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Query the organization configuration entity list based on the organizationId
     *
     * @param organizationId organization ID
     * @return config list
     */
    public List<OrganizationConfigEntity> queryByOrganizationId(Long organizationId) {
        PreConditions.notNull(organizationId, "organizationId");
        String sql = "SELECT organization_id, `key`, `value`, create_time, update_time, description"
                + " FROM config_organization_configuration WHERE organization_id = ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(OrganizationConfigEntity.class), organizationId);
    }

    /**
     * Query the organization configuration entity based on the organizationId and key
     *
     * @param organizationId organization ID
     * @param key configuration key
     * @return config entity
     */
    public OrganizationConfigEntity queryByOrganizationIdAndKey(Long organizationId, String key) {
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notBlank(key, "key");
        String sql = "SELECT organization_id, `key`, `value`, create_time, update_time, description"
                + " FROM config_organization_configuration WHERE organization_id = ? AND `key` = ?";
        try {
            return jdbcTemplate.queryForObject(sql,
                new BeanPropertyRowMapper<>(OrganizationConfigEntity.class), organizationId, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Batch insert or update organization configuration entity list
     *
     * @param entities config list
     * @return update or insert count
     */
    public int batchUpsert(List<OrganizationConfigEntity> entities) {
        PreConditions.notEmpty(entities, "entities");
        String sql = "INSERT INTO config_organization_configuration(organization_id, `key`, `value`, description)"
                + " VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = ?";
        int[] rets = jdbcTemplate.batchUpdate(sql, entities.stream().map(
                entity -> new Object[] {entity.getOrganizationId(), entity.getKey(), entity.getValue(),
                        entity.getDescription(), entity.getValue()})
                .collect(Collectors.toList()));
        return Arrays.stream(rets).sum();
    }
}
