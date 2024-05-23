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

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.google.common.collect.Iterables;
import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * @author gaoda.xy
 * @date 2024/3/27 19:03
 */
public interface DBColumnRepository extends OdcJpaRepository<DBColumnEntity, Long> {

    List<DBColumnEntity> findByDatabaseIdAndObjectIdIn(Long databaseId, Collection<Long> objectIds);

    @Modifying
    @Transactional
    @Query(value = "delete from database_schema_column t where t.id in (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "delete from database_schema_column t where t.database_id = :databaseId and t.object_id in (:objectIds)",
            nativeQuery = true)
    int deleteByDatabaseIdAndObjectIdIn(@Param("databaseId") Long databaseId,
            @Param("objectIds") Collection<Long> objectIds);

    @Modifying
    @Transactional
    @Query(value = "delete from database_schema_column t where t.database_id in (:databaseIds)", nativeQuery = true)
    int deleteByDatabaseIdIn(@Param("databaseIds") Collection<Long> databaseIds);

    default List<DBColumnEntity> batchCreate(List<DBColumnEntity> entities, int batchSize) {
        String sql = InsertSqlTemplateBuilder.from("database_schema_column")
                .field(DBColumnEntity_.name)
                .field(DBColumnEntity_.databaseId)
                .field(DBColumnEntity_.objectId)
                .field(DBColumnEntity_.organizationId)
                .build();
        List<Function<DBColumnEntity, Object>> getter = valueGetterBuilder()
                .add(DBColumnEntity::getName)
                .add(DBColumnEntity::getDatabaseId)
                .add(DBColumnEntity::getObjectId)
                .add(DBColumnEntity::getOrganizationId)
                .build();
        Iterable<List<DBColumnEntity>> partitions = Iterables.partition(entities, batchSize);
        List<DBColumnEntity> result = new ArrayList<>();
        for (List<DBColumnEntity> partition : partitions) {
            result.addAll(batchCreate(partition, sql, getter, DBColumnEntity::setId));
        }
        return result;
    }

}
