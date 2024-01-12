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
package com.oceanbase.odc.metadb.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface NotificationPolicyRepository extends OdcJpaRepository<NotificationPolicyEntity, Long>,
        JpaSpecificationExecutor<NotificationPolicyEntity> {
    Optional<NotificationPolicyEntity> findByOrganizationIdAndMatchExpression(Long organizationId,
            String matchExpression);

    List<NotificationPolicyEntity> findByIdIn(Collection<Long> ids);

    @Transactional
    @Modifying
    @Query(value = "update notification_policy set `is_enabled`=:enabled where `id` in (:ids)", nativeQuery = true)
    int updateStatusByIds(@Param("enabled") Boolean enabled, @Param("ids") Collection<Long> ids);

    List<NotificationPolicyEntity> findByOrganizationId(Long organizationId);

    @Query(value = "select * from notification_policy where organization_id in (:organizationIds)", nativeQuery = true)
    List<NotificationPolicyEntity> findByOrganizationIds(@Param("organizationIds") Collection<Long> ids);

    boolean existsByOrganizationIdAndMatchExpression(Long organizationId, String matchExpression);

    List<NotificationPolicyEntity> findByProjectId(Long projectId);

    default List<NotificationPolicyEntity> batchCreate(List<NotificationPolicyEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("notification_policy")
                .field(NotificationPolicyEntity_.creatorId)
                .field(NotificationPolicyEntity_.organizationId)
                .field(NotificationPolicyEntity_.titleTemplate)
                .field(NotificationPolicyEntity_.contentTemplate)
                .field(NotificationPolicyEntity_.projectId)
                .field(NotificationPolicyEntity_.policyMetadataId)
                .field(NotificationPolicyEntity_.matchExpression)
                .field(NotificationPolicyEntity_.toUsers)
                .field(NotificationPolicyEntity_.ccUsers)
                .field("is_enabled")
                .build();

        List<Function<NotificationPolicyEntity, Object>> getter = valueGetterBuilder()
                .add(NotificationPolicyEntity::getCreatorId)
                .add(NotificationPolicyEntity::getOrganizationId)
                .add(NotificationPolicyEntity::getTitleTemplate)
                .add(NotificationPolicyEntity::getContentTemplate)
                .add(NotificationPolicyEntity::getProjectId)
                .add(NotificationPolicyEntity::getPolicyMetadataId)
                .add(NotificationPolicyEntity::getMatchExpression)
                .add(NotificationPolicyEntity::getToUsers)
                .add(NotificationPolicyEntity::getCcUsers)
                .add(NotificationPolicyEntity::isEnabled)
                .build();

        return batchCreate(entities, sql, getter, NotificationPolicyEntity::setId);
    }
}
