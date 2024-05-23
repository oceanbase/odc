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
package com.oceanbase.odc.metadb.dbobject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;
import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author gaoda.xy
 * @date 2024/3/27 17:55
 */
public interface DBObjectRepository extends OdcJpaRepository<DBObjectEntity, Long> {

    List<DBObjectEntity> findByIdIn(Collection<Long> ids);

    List<DBObjectEntity> findByDatabaseIdAndType(Long databaseId, DBObjectType type);

    List<DBObjectEntity> findByDatabaseIdAndTypeIn(Long databaseId, Collection<DBObjectType> types);

    @Modifying
    @Transactional
    @Query(value = "delete from database_schema_object t where t.id in (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "delete from database_schema_object t where t.database_id in (:databaseIds)", nativeQuery = true)
    int deleteByDatabaseIdIn(@Param("databaseIds") Collection<Long> databaseIds);

    default List<DBObjectEntity> batchCreate(List<DBObjectEntity> entities, int batchSize) {
        String sql = InsertSqlTemplateBuilder.from("database_schema_object")
                .field(DBObjectEntity_.name)
                .field(DBObjectEntity_.databaseId)
                .field(DBObjectEntity_.type)
                .field(DBObjectEntity_.organizationId)
                .build();
        List<Function<DBObjectEntity, Object>> getter = valueGetterBuilder()
                .add(DBObjectEntity::getName)
                .add(DBObjectEntity::getDatabaseId)
                .add(e -> e.getType().name())
                .add(DBObjectEntity::getOrganizationId)
                .build();
        Iterable<List<DBObjectEntity>> partitions = Iterables.partition(entities, batchSize);
        List<DBObjectEntity> result = new ArrayList<>();
        for (List<DBObjectEntity> partition : partitions) {
            result.addAll(batchCreate(partition, sql, getter, DBObjectEntity::setId));
        }
        return result;
    }

}
