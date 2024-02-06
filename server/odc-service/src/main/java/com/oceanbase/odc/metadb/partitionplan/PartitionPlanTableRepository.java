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

/**
 * {@link PartitionPlanTableRepository}
 *
 * @author yh263208
 * @date 2024-01-10 17:04
 * @since ODC_release_4.2.4
 */
public interface PartitionPlanTableRepository extends OdcJpaRepository<PartitionPlanTableEntity, Long>,
        JpaSpecificationExecutor<PartitionPlanTableEntity> {

    List<PartitionPlanTableEntity> findByPartitionPlanIdInAndEnabled(List<Long> partitionPlanIds, Boolean enabled);

    @Transactional
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    List<PartitionPlanTableEntity> findByIdIn(List<Long> ids);

    @Transactional
    @Query("update PartitionPlanTableEntity set enabled=:enabled where id in (:ids)")
    @Modifying
    int updateEnabledByIdIn(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);

    default List<PartitionPlanTableEntity> batchCreate(List<PartitionPlanTableEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan_table")
                .field(PartitionPlanTableEntity_.tableName)
                .field("partitionplan_id")
                .field(PartitionPlanTableEntity_.scheduleId)
                .field("is_enabled")
                .field(PartitionPlanTableEntity_.partitionNameInvoker)
                .field(PartitionPlanTableEntity_.partitionNameInvokerParameters)
                .build();
        List<Function<PartitionPlanTableEntity, Object>> getter = valueGetterBuilder()
                .add(PartitionPlanTableEntity::getTableName)
                .add(PartitionPlanTableEntity::getPartitionPlanId)
                .add(PartitionPlanTableEntity::getScheduleId)
                .add(PartitionPlanTableEntity::getEnabled)
                .add(PartitionPlanTableEntity::getPartitionNameInvoker)
                .add(PartitionPlanTableEntity::getPartitionNameInvokerParameters)
                .build();
        return batchCreate(entities, sql, getter, PartitionPlanTableEntity::setId);
    }

}
