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
import java.util.Optional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface LogicalDBExecutionRepository extends OdcJpaRepository<LogicalDBChangeExecutionUnitEntity, Long> {
    Optional<LogicalDBChangeExecutionUnitEntity> findByExecutionId(String executionId);

    List<LogicalDBChangeExecutionUnitEntity> findByScheduleTaskIdOrderByExecutionOrderAsc(Long scheduleTaskId);

    List<LogicalDBChangeExecutionUnitEntity> findByScheduleTaskIdAndPhysicalDatabaseIdOrderByExecutionOrderAsc(
            Long scheduleTaskId, Long physicalDatabaseId);
}
