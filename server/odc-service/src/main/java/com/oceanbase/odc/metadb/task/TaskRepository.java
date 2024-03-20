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
package com.oceanbase.odc.metadb.task;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.core.shared.constant.TaskStatus;

/**
 * @author wenniu.ly
 * @date 2022/2/11
 */
public interface TaskRepository extends JpaRepository<TaskEntity, Long>, JpaSpecificationExecutor<TaskEntity> {
    @Transactional
    @Query("update TaskEntity set executionExpirationIntervalSeconds=:#{#param.executionExpirationIntervalSeconds},"
            + "connectionId=:#{#param.connectionId},taskType=:#{#param.taskType},databaseName=:#{#param.databaseName},"
            + "description=:#{#param.description},executor=:#{#param.executor},status=:#{#param.status},"
            + "progressPercentage=:#{#param.progressPercentage},resultJson=:#{#param.resultJson},parametersJson=:#{#param.parametersJson},riskLevelId=:#{#param.riskLevelId}"
            + " where id=:#{#param.id}")
    @Modifying
    int update(@Param("param") TaskEntity entity);

    @Transactional
    @Query("update TaskEntity set parametersJson=:#{#param.parametersJson} where id=:#{#param.id}")
    @Modifying
    int updateParametersJson(@Param("param") TaskEntity entity);

    @Transactional
    @Query(value = "update task_task set executor=:executor where id=:id", nativeQuery = true)
    @Modifying
    int updateExecutorById(@Param("id") Long id, @Param("executor") String executor);

    List<TaskEntity> findByIdIn(Set<Long> taskIds);

    List<TaskEntity> findByJobId(Long jobId);

    @Transactional
    @Query("update TaskEntity set job_id=:jobId where id=:id")
    @Modifying
    void updateJobId(@Param("id") Long id, @Param("jobId") Long jobId);

    @Transactional
    @Modifying
    @Query("update TaskEntity st set st.status = ?2 where st.id = ?1")
    int updateStatusById(Long id, TaskStatus status);

}
