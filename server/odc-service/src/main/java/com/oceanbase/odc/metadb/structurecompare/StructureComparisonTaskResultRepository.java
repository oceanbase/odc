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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author jingtian
 * @date 2024/1/16
 * @since ODC_release_4.2.4
 */
public interface StructureComparisonTaskResultRepository
        extends JpaRepository<StructureComparisonTaskResultEntity, Long>,
        JpaSpecificationExecutor<StructureComparisonTaskResultEntity> {

    @Query(value = "select * from structure_comparison where id=:id and structure_comparison_task_id=:comparisonTaskId",
            nativeQuery = true)
    StructureComparisonTaskResultEntity findByIdAndComparisonTaskId(@Param("id") Long id,
            @Param("comparisonTaskId") Long comparisonTaskId);
}
