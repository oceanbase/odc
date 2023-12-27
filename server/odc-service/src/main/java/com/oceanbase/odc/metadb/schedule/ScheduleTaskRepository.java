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
package com.oceanbase.odc.metadb.schedule;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.core.shared.constant.TaskStatus;

/**
 * @Author：tinker
 * @Date: 2023/5/16 11:38
 * @Descripition:
 */
@Repository
public interface ScheduleTaskRepository extends JpaRepository<ScheduleTaskEntity, Long>,
        JpaSpecificationExecutor<ScheduleTaskEntity> {

    List<ScheduleTaskEntity> findByJobNameAndStatusIn(String jobName, List<TaskStatus> statuses);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.status = ?2 where st.id = ?1")
    int updateStatusById(Long id, TaskStatus status);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.status = ?2, st.progressPercentage = ?3 where st.id = ?1")
    int updateStatusAndProcessPercentageById(Long id, TaskStatus status, double progressPercentage);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.resultJson = ?2 where st.id = ?1")
    int updateTaskResult(Long id, String resultJson);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.parametersJson = ?2 where st.id = ?1")
    int updateTaskParameters(Long id, String parametersJson);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.executor = ?2 where st.id = ?1")
    int updateExecutor(Long id, String executor);

    @Transactional
    @Modifying
    @Query("update ScheduleTaskEntity st set st.parametersJson=:#{#entity.parametersJson},"
            + "st.status=:#{#entity.status},st.progressPercentage=:#{#entity.progressPercentage},"
            + "st.resultJson=:#{#entity.resultJson} where st.id=:#{#entity.id}")
    int update(@Param("entity") ScheduleTaskEntity entity);

}
