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

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface TableMappingRepository extends OdcJpaRepository<TableMappingEntity, Long> {
    List<TableMappingEntity> findByLogicalTableIdIn(Collection<Long> logicalTableId);

    List<TableMappingEntity> findByLogicalTableId(Long logicalTableId);

    @Transactional
    int deleteByLogicalTableId(Long logicalTableId);

    @Modifying
    @Transactional
    @Query(value = "delete from database_table_mapping t where t.physical_database_id in (:physicalDatabaseIds)",
            nativeQuery = true)
    int deleteByPhysicalDatabaseIds(@Param("physicalDatabaseIds") Collection<Long> physicalDatabaseIds);

    default List<TableMappingEntity> batchCreate(List<TableMappingEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("database_table_mapping")
                .field(TableMappingEntity_.logicalTableId)
                .field(TableMappingEntity_.physicalDatabaseId)
                .field(TableMappingEntity_.physicalDatabaseName)
                .field(TableMappingEntity_.physicalTableName)
                .field(TableMappingEntity_.expression)
                .field("is_consistent")
                .field(TableMappingEntity_.organizationId)
                .build();
        List<Function<TableMappingEntity, Object>> getter = valueGetterBuilder()
                .add(TableMappingEntity::getLogicalTableId)
                .add(TableMappingEntity::getPhysicalDatabaseId)
                .add(TableMappingEntity::getPhysicalDatabaseName)
                .add(TableMappingEntity::getPhysicalTableName)
                .add(TableMappingEntity::getExpression)
                .add(TableMappingEntity::getConsistent)
                .add(TableMappingEntity::getOrganizationId)
                .build();
        return batchCreate(entities, sql, getter, TableMappingEntity::setId, 200);
    }

}
