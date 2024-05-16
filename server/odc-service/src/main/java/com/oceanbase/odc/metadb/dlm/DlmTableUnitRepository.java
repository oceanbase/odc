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
package com.oceanbase.odc.metadb.dlm;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.TaskStatus;

/**
 * @Author：tinker
 * @Date: 2024/5/13 20:49
 * @Descripition:
 */
public interface DlmTableUnitRepository extends OdcJpaRepository<DlmTableUnitEntity, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE DlmTableUnitEntity e SET e.status = ?3,e.startTime = ?2 WHERE e.dlmTableUnitId = ?1")
    int updateStatusAndStartTimeByDlmTableUnitId(String dlmTableUnitId, Date startTime, TaskStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE DlmTableUnitEntity e SET e.status = ?3,e.endTime = ?2 WHERE e.dlmTableUnitId = ?1")
    int updateStatusAndEndTimeByDlmTableUnitId(String dlmTableUnitId, Date endTime, TaskStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE DlmTableUnitEntity e SET e.status = ?2 WHERE e.dlmTableUnitId = ?1")
    int updateStatusByDlmTableUnitId(String dlmTableUnitId, TaskStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE DlmTableUnitEntity e SET e.statistic = ?2 WHERE e.dlmTableUnitId = ?1")
    int updateStatisticByDlmTableUnitId(String dlmTableUnitId, String statistic);

    List<DlmTableUnitEntity> findByScheduleTaskId(Long scheduleTaskId);
}
