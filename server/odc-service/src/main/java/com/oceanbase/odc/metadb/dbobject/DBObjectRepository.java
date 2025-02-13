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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;

/**
 * @author gaoda.xy
 * @date 2024/3/27 17:55
 */
public interface DBObjectRepository extends OdcJpaRepository<DBObjectEntity, Long> {

    List<DBObjectEntity> findByIdIn(Collection<Long> ids);

    List<DBObjectEntity> findByDatabaseIdAndType(Long databaseId, DBObjectType type);

    List<DBObjectEntity> findByDatabaseIdAndTypeOrderByNameAsc(Long databaseId, DBObjectType type);

    List<DBObjectEntity> findByDatabaseIdAndTypeIn(Long databaseId, Collection<DBObjectType> types);

    List<DBObjectEntity> findByDatabaseIdInAndTypeIn(Collection<Long> databaseIds, Collection<DBObjectType> types);

    /**
     * list physical tables that not belongs to any logical tables in a physical database
     * 
     * @param databaseId
     */
    @Query(value = "select * from database_schema_object t1 where t1.type='TABLE' and t1.database_id=:databaseId and t1.id not in(select t2.physical_table_id from database_table_mapping t2 where t2.physical_database_id=:databaseId)",
            nativeQuery = true)
    List<DBObjectEntity> listPhysicalTablesThatNotBelongsToLogicalTables(@Param("databaseId") Long databaseId);

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
        return DBSchemaAccessorUtil.partitionFind(entities, batchSize,
                e -> batchCreate(e, sql, getter, DBObjectEntity::setId));
    }

}
