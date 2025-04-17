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
package com.oceanbase.odc.metadb.resourcehistory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.common.jdbc.JdbcTemplateUtils;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.PreConditions;

import jakarta.transaction.Transactional;
import lombok.NonNull;

@Repository
public interface ResourceLastAccessRepository extends OdcJpaRepository<ResourceLastAccessEntity, Long>,
        JpaRepository<ResourceLastAccessEntity, Long>, JpaSpecificationExecutor<ResourceLastAccessEntity> {
    Optional<ResourceLastAccessEntity> findByOrganizationIdAndProjectIdAndUserIdAndResourceTypeAndResourceId(
            @NonNull Long organizationId, @NonNull Long projectId,
            @NonNull Long userId, @NonNull String resourceType, @NonNull Long resourceId);

    Page<ResourceLastAccessEntity> findByOrganizationIdAndProjectIdAndUserIdAndResourceType(
            @NonNull Long organizationId, @NonNull Long projectId,
            @NonNull Long userId, @NonNull String resourceType,
            @NonNull Pageable pageable);

    @Transactional
    int deleteByOrganizationIdAndProjectIdAndResourceTypeAndResourceIdIn(
            @NonNull Long organizationId, @NonNull Long projectId,
            @NonNull String resourceType,
            @NonNull Collection<Long> resourceIds);

    @Transactional
    default int batchUpsert(List<ResourceLastAccessEntity> entities) {
        PreConditions.notEmpty(entities, "entities");
        String sql =
                "INSERT INTO history_resource_last_access(organization_id, project_id, user_id, resource_type, resource_id, last_access_time)"
                        + " VALUES(?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `last_access_time` = ?";
        int[] rets = getJdbcTemplate().batchUpdate(sql, entities.stream().map(
                entity -> new Object[] {entity.getOrganizationId(), entity.getProjectId(), entity.getUserId(),
                        entity.getResourceType(), entity.getResourceId(), entity.getLastAccessTime(),
                        entity.getLastAccessTime()})
                .collect(Collectors.toList()));
        return JdbcTemplateUtils.batchInsertAffectRows(rets);
    }
}
