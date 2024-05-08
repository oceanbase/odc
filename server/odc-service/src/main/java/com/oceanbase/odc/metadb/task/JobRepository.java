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

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.service.task.enums.JobStatus;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long>,
        JpaSpecificationExecutor<JobEntity> {

    @Transactional
    @Query(value = "update job_job set "
            + " executor_endpoint=:#{#param.executorEndpoint},status=:#{#param.status.name()},"
            + " progress_percentage=:#{#param.progressPercentage},result_json=:#{#param.resultJson},"
            + " finished_time=:#{#param.finishedTime},last_report_time=:#{#param.lastReportTime}"
            + " where id=:id and status =:#{#oldStatus.name()}", nativeQuery = true)
    @Modifying
    int updateReportResult(@Param("param") JobEntity entity, @Param("id") Long id,
            @Param("oldStatus") JobStatus oldStatus);

    @Transactional
    @Query("update JobEntity set "
            + " executorIdentifier=:#{#param.executorIdentifier}"
            + " where id=:#{#param.id}")
    @Modifying
    int updateJobExecutorIdentifierById(@Param("param") JobEntity entity);

    @Transactional
    @Query("update JobEntity set "
            + " status=:#{#param.status},"
            + " executionTimes=:#{#param.executionTimes},"
            + " startedTime=:#{#param.startedTime},"
            + " lastHeartTime=:#{#param.lastHeartTime},"
            + " executorDestroyedTime=:#{#param.executorDestroyedTime}"
            + " where id=:#{#param.id}")
    @Modifying
    int updateJobStatusAndExecutionTimesById(@Param("param") JobEntity entity);

    @Query(value = "SELECT * FROM job_job WHERE id = ?1", nativeQuery = true)
    Optional<JobEntity> findByIdNative(Long id);
}
