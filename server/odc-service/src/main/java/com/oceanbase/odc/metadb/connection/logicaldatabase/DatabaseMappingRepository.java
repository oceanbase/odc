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

public interface DatabaseMappingRepository extends OdcJpaRepository<DatabaseMappingEntity, Long> {

    List<DatabaseMappingEntity> findByLogicalDatabaseId(Long logicalDatabaseId);

    default List<DatabaseMappingEntity> batchCreate(List<DatabaseMappingEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("connect_database_mapping")
                .field(DatabaseMappingEntity_.logicalDatabaseId)
                .field(DatabaseMappingEntity_.physicalDatabaseId)
                .field(DatabaseMappingEntity_.organizationId)
                .build();
        List<Function<DatabaseMappingEntity, Object>> getter = valueGetterBuilder()
                .add(DatabaseMappingEntity::getLogicalDatabaseId)
                .add(DatabaseMappingEntity::getPhysicalDatabaseId)
                .add(DatabaseMappingEntity::getOrganizationId)
                .build();
        return batchCreate(entities, sql, getter, DatabaseMappingEntity::setId, 200);
    }
}
