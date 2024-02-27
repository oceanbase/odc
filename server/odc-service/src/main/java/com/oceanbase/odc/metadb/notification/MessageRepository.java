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

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.QueryMessageParams;

import lombok.NonNull;

public interface MessageRepository extends OdcJpaRepository<MessageEntity, Long>,
        JpaSpecificationExecutor<MessageEntity> {
    @Query(value = "update notification_message set `status`=:#{#status.name()} where `id`=:id", nativeQuery = true)
    @Transactional
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") MessageSendingStatus status);

    @Query(value = "update notification_message set `status`=:#{#status.name()}, `last_sent_time`=now() where `id`=:id",
            nativeQuery = true)
    @Transactional
    @Modifying
    int updateStatusAndSentTimeById(@Param("id") Long id, @Param("status") MessageSendingStatus status);

    @Query(value = "update notification_message set `status`=:#{#status.name()}, `error_message`=:errorMessage,"
            + " `last_sent_time`=now(), `retry_times`=retry_times+1 where `id`=:id",
            nativeQuery = true)
    @Transactional
    @Modifying
    int updateStatusAndRetryTimesAndErrorMessageById(@Param("id") Long id, @Param("status") MessageSendingStatus status,
            @Param("errorMessage") String errorMessage);

    @Query(value = "select * from notification_message where `status`=:#{#status.name()} and `retry_times`< `max_retry_times` limit "
            + ":limit for update nowait",
            nativeQuery = true)
    List<MessageEntity> findNByStatusForUpdate(@Param("status") MessageSendingStatus status,
            @Param("limit") Integer limit);

    default Page<MessageEntity> find(@NonNull QueryMessageParams queryParams, @NonNull Pageable pageable) {
        Specification<MessageEntity> specs = Specification
                .where(OdcJpaRepository.eq(MessageEntity_.projectId, queryParams.getProjectId()))
                .and(OdcJpaRepository.like(MessageEntity_.title, queryParams.getFuzzyTitle()))
                .and(OdcJpaRepository.in(MessageEntity_.channelId, queryParams.getChannelIds()))
                .and(OdcJpaRepository.in(MessageEntity_.status, queryParams.getStatuses()));
        return findAll(specs, pageable);
    }

}
