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
package com.oceanbase.odc.metadb.partitionplan;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

import lombok.NonNull;

/**
 * {@link PartitionPlanRepository}
 *
 * @author yh263208
 * @date 2024-01-10 16:55
 * @since ODC_release_4.2.4
 */
public interface PartitionPlanRepository extends OdcJpaRepository<PartitionPlanEntity, Long>,
        JpaSpecificationExecutor<PartitionPlanEntity> {

    List<PartitionPlanEntity> findByDatabaseIdAndEnabled(Long databaseId, Boolean enabled);

    Optional<PartitionPlanEntity> findByFlowInstanceId(@NonNull Long flowInstanceId);

    @Transactional
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    List<PartitionPlanEntity> findByIdIn(List<Long> ids);

    @Transactional
    @Query("update PartitionPlanEntity set enabled=:enabled, lastModifierId=:lastModifierId where id in (:ids)")
    @Modifying
    int updateEnabledAndLastModifierIdByIdIn(@Param("ids") List<Long> ids,
            @Param("enabled") Boolean enabled, @Param("lastModifierId") Long lastModifierId);

    default List<PartitionPlanEntity> batchCreate(List<PartitionPlanEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan")
                .field(PartitionPlanEntity_.flowInstanceId)
                .field("is_enabled")
                .field(PartitionPlanEntity_.creatorId)
                .field(PartitionPlanEntity_.lastModifierId)
                .field(PartitionPlanEntity_.databaseId)
                .build();
        List<Function<PartitionPlanEntity, Object>> getter = valueGetterBuilder()
                .add(PartitionPlanEntity::getFlowInstanceId)
                .add(PartitionPlanEntity::getEnabled)
                .add(PartitionPlanEntity::getCreatorId)
                .add(PartitionPlanEntity::getLastModifierId)
                .add(PartitionPlanEntity::getDatabaseId)
                .build();
        return batchCreate(entities, sql, getter, PartitionPlanEntity::setId);
    }

}
