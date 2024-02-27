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
package com.oceanbase.odc.metadb.structurecompare;

import java.util.List;
import java.util.function.Function;

import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * @author jingtian
 * @date 2024/1/16
 * @since ODC_release_4.2.4
 */
public interface StructureComparisonTaskResultRepository
        extends OdcJpaRepository<StructureComparisonTaskResultEntity, Long> {

    @Transactional(rollbackFor = Exception.class)
    default List<StructureComparisonTaskResultEntity> batchCreate(List<StructureComparisonTaskResultEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("structure_comparison_task_result")
                .field(StructureComparisonTaskResultEntity_.structureComparisonTaskId)
                .field(StructureComparisonTaskResultEntity_.databaseObjectName)
                .field(StructureComparisonTaskResultEntity_.databaseObjectType)
                .field(StructureComparisonTaskResultEntity_.comparingResult)
                .field(StructureComparisonTaskResultEntity_.sourceDatabaseObjectDdl)
                .field(StructureComparisonTaskResultEntity_.targetDatabaseObjectDdl)
                .field((StructureComparisonTaskResultEntity_.changeSqlScript))
                .build();

        List<Function<StructureComparisonTaskResultEntity, Object>> getter = valueGetterBuilder()
                .add(StructureComparisonTaskResultEntity::getStructureComparisonTaskId)
                .add(StructureComparisonTaskResultEntity::getDatabaseObjectName)
                .add((StructureComparisonTaskResultEntity e) -> e.getDatabaseObjectType().name())
                .add((StructureComparisonTaskResultEntity e) -> e.getComparingResult().name())
                .add(StructureComparisonTaskResultEntity::getSourceDatabaseObjectDdl)
                .add(StructureComparisonTaskResultEntity::getTargetDatabaseObjectDdl)
                .add(StructureComparisonTaskResultEntity::getChangeSqlScript)
                .build();

        return batchCreate(entities, sql, getter, StructureComparisonTaskResultEntity::setId);
    }
}
