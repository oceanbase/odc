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

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * {@link PartitionPlanTablePartitionKeyRepository}
 *
 * @author yh263208
 * @date 2023-01-10 18:53
 * @since ODC_release_4.2.4
 */
public interface PartitionPlanTablePartitionKeyRepository
        extends OdcJpaRepository<PartitionPlanTablePartitionKeyEntity, Long>,
        JpaSpecificationExecutor<PartitionPlanTablePartitionKeyEntity> {

    List<PartitionPlanTablePartitionKeyEntity> findByPartitionplanTableIdInAndEnabled(
            List<Long> partitionplanTableId, Boolean enabled);

    default List<PartitionPlanTablePartitionKeyEntity> batchCreate(
            List<PartitionPlanTablePartitionKeyEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan_table_partitionkey")
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKey)
                .field(PartitionPlanTablePartitionKeyEntity_.strategy)
                .field(PartitionPlanTablePartitionKeyEntity_.partitionplanTableId)
                .field("is_enabled")
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKeyInvoker)
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKeyInvokerParameters)
                .build();
        List<Function<PartitionPlanTablePartitionKeyEntity, Object>> getter = valueGetterBuilder()
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKey)
                .add(p -> p.getStrategy().name())
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionplanTableId)
                .add(PartitionPlanTablePartitionKeyEntity::getEnabled)
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKeyInvoker)
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKeyInvokerParameters)
                .build();
        return batchCreate(entities, sql, getter, PartitionPlanTablePartitionKeyEntity::setId);
    }

}
