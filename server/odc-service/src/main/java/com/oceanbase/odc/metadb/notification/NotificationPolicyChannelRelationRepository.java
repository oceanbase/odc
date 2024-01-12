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
import java.util.function.Function;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface NotificationPolicyChannelRelationRepository
        extends OdcJpaRepository<NotificationChannelRelationEntity, Long>,
        JpaSpecificationExecutor<NotificationChannelRelationEntity> {


    List<NotificationChannelRelationEntity> findByOrganizationIdAndNotificationPolicyId(Long organizationId,
            Long notificationPolicyId);

    @Transactional
    int deleteByChannelId(Long channelId);

    @Transactional
    @Query(value = "delete from notification_policy_channel_relation where notification_policy_id in (:ids)",
            nativeQuery = true)
    @Modifying
    int deleteByNotificationPolicyIds(@Param("ids") Collection<Long> ids);

    @Query(value = "select * from notification_policy_channel_relation where notification_policy_id in (:ids)",
            nativeQuery = true)
    List<NotificationChannelRelationEntity> findByNotificationPolicyIds(@Param("ids") Collection<Long> ids);

    default List<NotificationChannelRelationEntity> batchCreate(List<NotificationChannelRelationEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("notification_policy_channel_relation")
                .field(NotificationChannelRelationEntity_.creatorId)
                .field(NotificationChannelRelationEntity_.organizationId)
                .field(NotificationChannelRelationEntity_.notificationPolicyId)
                .field(NotificationChannelRelationEntity_.channelId)
                .build();

        List<Function<NotificationChannelRelationEntity, Object>> getter = valueGetterBuilder()
                .add(NotificationChannelRelationEntity::getCreatorId)
                .add(NotificationChannelRelationEntity::getOrganizationId)
                .add(NotificationChannelRelationEntity::getNotificationPolicyId)
                .add(NotificationChannelRelationEntity::getChannelId)
                .build();

        return batchCreate(entities, sql, getter, NotificationChannelRelationEntity::setId);
    }
}
