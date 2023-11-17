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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPolicyChannelRelationRepository
        extends JpaRepository<NotificationChannelRelationEntity, Long>,
        JpaSpecificationExecutor<NotificationChannelRelationEntity> {


    List<NotificationChannelRelationEntity> findByOrganizationIdAndNotificationPolicyId(Long organizationId,
            Long notificationPolicyId);

    @Query(value = "select * from notification_policy_channel_relation where notification_policy_id in (:ids)",
            nativeQuery = true)
    List<NotificationChannelRelationEntity> findByNotificationPolicyIds(@Param("ids") Collection<Long> ids);
}
