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
package com.oceanbase.odc.metadb.shadowtable;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableComparingTaskRepository extends JpaRepository<ShadowTableComparingTaskEntity, Long>,
        JpaSpecificationExecutor<ShadowTableComparingTaskEntity> {

    Optional<ShadowTableComparingTaskEntity> findByIdAndCreatorId(
            @Param("id") Long id, @Param("creatorId") Long creatorId);


    @Transactional
    @Modifying
    @Query(value = "update shadowtable_table_comparing_task set flow_instance_id=:flowInstanceId where id=:id",
            nativeQuery = true)
    int updateFlowInstanceIdById(@Param("id") Long id, @Param("flowInstanceId") Long flowInstanceId);
}
