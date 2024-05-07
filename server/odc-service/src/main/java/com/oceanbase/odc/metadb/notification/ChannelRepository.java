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

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;

import lombok.NonNull;

public interface ChannelRepository extends OdcJpaRepository<ChannelEntity, Long>,
        JpaSpecificationExecutor<ChannelEntity> {

    List<ChannelEntity> findByIdIn(Collection<Long> ids);

    Optional<ChannelEntity> findByProjectIdAndName(Long projectId, String name);

    Optional<ChannelEntity> findByIdAndProjectId(Long id, Long projectId);

    @Modifying
    @Query(value = "select c.* from notification_channel c inner join notification_policy_channel_relation cr "
            + "on c.id=cr.channel_id WHERE cr.notification_policy_id=?1", nativeQuery = true)
    List<ChannelEntity> findByPolicyId(Long policyId);

    default Page<ChannelEntity> find(@NonNull QueryChannelParams params, @NonNull Pageable pageable) {
        Specification<ChannelEntity> specs = Specification
                .where(OdcJpaRepository.eq(ChannelEntity_.projectId, params.getProjectId()))
                .and(OdcJpaRepository.like(ChannelEntity_.name, params.getFuzzyChannelName()))
                .and(OdcJpaRepository.in(ChannelEntity_.type, params.getChannelTypes()));
        return findAll(specs, pageable);
    }

    @Modifying
    @Transactional
    @Query(value = "update notification_channel set name=:#{#channel.name}, type=:#{#channel.type.name()}, "
            + "description=:#{#channel.description} where id=:#{#channel.id}",
            nativeQuery = true)
    int update(@Param("channel") ChannelEntity channel);

}
