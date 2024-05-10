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
package com.oceanbase.odc.metadb.connection.logicaldatabase;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface LogicalTablePhysicalTableRepository extends OdcJpaRepository<LogicalTablePhysicalTableEntity, Long> {
    List<LogicalTablePhysicalTableEntity> findByLogicalTableIdIn(Collection<Long> logicalTableId);

    void deleteByLogicalTableId(Long logicalTableId);

    default List<LogicalTablePhysicalTableEntity> batchCreate(List<LogicalTablePhysicalTableEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("connect_logical_table")
                .field(LogicalTablePhysicalTableEntity_.LOGICAL_TABLE_ID)
                .field(LogicalTablePhysicalTableEntity_.PHYSICAL_DATABASE_ID)
                .field(LogicalTablePhysicalTableEntity_.PHYSICAL_DATABASE_NAME)
                .field(LogicalTablePhysicalTableEntity_.PHYSICAL_TABLE_NAME)
                .field(LogicalTablePhysicalTableEntity_.CONSISTENT)
                .field(LogicalTablePhysicalTableEntity_.ORGANIZATION_ID)
                .build();
        List<Function<LogicalTablePhysicalTableEntity, Object>> getter = valueGetterBuilder()
                .add(LogicalTablePhysicalTableEntity::getLogicalTableId)
                .add(LogicalTablePhysicalTableEntity::getPhysicalDatabaseId)
                .add(LogicalTablePhysicalTableEntity::getPhysicalDatabaseName)
                .add(LogicalTablePhysicalTableEntity::getPhysicalTableName)
                .add(LogicalTablePhysicalTableEntity::getConsistent)
                .add(LogicalTablePhysicalTableEntity::getOrganizationId)
                .build();
        return batchCreate(entities, sql, getter, LogicalTablePhysicalTableEntity::setId, 200);
    }

}
