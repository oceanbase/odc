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

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author jingtian
 * @date 2024/1/16
 * @since ODC_release_4.2.4
 */
public interface StructureComparisonTaskRepository extends JpaRepository<StructureComparisonTaskEntity, Long>,
        JpaSpecificationExecutor<StructureComparisonTaskEntity> {

    @Transactional
    @Query(value = "update structure_comparison_task set total_change_sql_script=:changeSqlScript, storage_object_id=:storageObjectId where id=:id",
            nativeQuery = true)
    @Modifying
    int updateTotalChangeSqlScriptAndStorageObjectIdById(@Param("id") Long id,
            @Param("changeSqlScript") String changeSqlScript, @Param("storageObjectId") String storageObjectId);

}
