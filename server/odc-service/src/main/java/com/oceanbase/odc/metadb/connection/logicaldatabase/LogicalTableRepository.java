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

import java.util.List;
import java.util.function.Function;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface LogicalTableRepository extends OdcJpaRepository<LogicalTableEntity, Long> {
    List<LogicalTableEntity> findByLogicalDatabaseId(Long logicalDatabaseId);


    default List<LogicalTableEntity> batchCreate(List<LogicalTableEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("connect_logical_table")
                .field(LogicalTableEntity_.LOGICAL_DATABASE_ID)
                .field(LogicalTableEntity_.EXPRESSION)
                .field(LogicalTableEntity_.NAME)
                .field(LogicalTableEntity_.ORGANIZATION_ID)
                .field(LogicalTableEntity_.LAST_SYNC_TIME)
                .build();
        List<Function<LogicalTableEntity, Object>> getter = valueGetterBuilder()
                .add(LogicalTableEntity::getLogicalDatabaseId)
                .add(LogicalTableEntity::getExpression)
                .add(LogicalTableEntity::getName)
                .add(LogicalTableEntity::getOrganizationId)
                .add(LogicalTableEntity::getLastSyncTime)
                .build();
        return batchCreate(entities, sql, getter, LogicalTableEntity::setId, 200);
    }

}
