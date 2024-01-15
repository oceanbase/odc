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

import java.util.Date;

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
    @Query("update JobEntity set "
            + " executorEndpoint=:#{#param.executorEndpoint},status=:#{#param.status},"
            + " progressPercentage=:#{#param.progressPercentage},resultJson=:#{#param.resultJson},"
            + " finishedTime=:#{#param.finishedTime},lastReportTime=:#{#param.lastReportTime}"
            + " where id=:#{#param.id}")
    @Modifying
    int update(@Param("param") JobEntity entity);

    @Transactional
    @Query("update JobEntity set "
            + " executorIdentifier=:#{#param.executorIdentifier},status=:#{#param.status},"
            + " executionTimes=:#{#param.executionTimes},"
            + " startedTime=:#{#param.startedTime},"
            + " executorDestroyedTime=:#{#param.executorDestroyedTime}"
            + " where id=:#{#param.id}")
    @Modifying
    int updateJobExecutorIdentifierAndStatusById(@Param("param") JobEntity entity);

    @Transactional
    @Query("update JobEntity set "
            + " description=:description"
            + " where id=:id")
    @Modifying
    void updateDescription(@Param("id") Long id, @Param("description") String description);

    @Transactional
    @Query("update JobEntity set "
            + " status=:status"
            + " where id=:id")
    @Modifying
    void updateStatusByIdAndOldStatus(@Param("id") Long id, @Param("status") JobStatus status);


    @Transactional
    @Query("update JobEntity set "
            + " status=:newStatus,description=:description"
            + " where id=:id and status=:oldStatus")
    @Modifying
    int updateStatusDescriptionByIdOldStatus(@Param("id") Long id, @Param("oldStatus") JobStatus oldStatus,
            @Param("newStatus") JobStatus newStatus, @Param("description") String description);

    @Transactional
    @Query("update JobEntity set "
            + " status=:newStatus,description=:description"
            + " where id=:id and status=:oldStatus and executorDestroyedTime is null")
    @Modifying
    int updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(@Param("id") Long id,
            @Param("oldStatus") JobStatus oldStatus, @Param("newStatus") JobStatus newStatus,
            @Param("description") String description);

    @Transactional
    @Query("update JobEntity set "
            + " status=:newStatus, cancellingTime=:cancellingTime"
            + " where id=:id and status=:oldStatus")
    @Modifying
    int updateJobToCancelling(@Param("id") Long id, @Param("oldStatus") JobStatus oldStatus,
            @Param("newStatus") JobStatus newStatus, @Param("cancellingTime") Date cancellingTime);

    @Transactional
    @Query("update JobEntity set "
            + " executorDestroyedTime=:executorDestroyedTime"
            + " where id=:id and executorDestroyedTime is not null")
    @Modifying
    int updateExecutorToDestroyed(@Param("id") Long id,
            @Param("executorDestroyedTime") Date executorDestroyedTime);

    @Transactional
    @Query("update JobEntity set "
            + " lastHeartTime=:lastHeartTime"
            + " where id=:id and executorEndpoint=:executorEndpoint")
    @Modifying
    void updateLatestHeartTime(@Param("id") Long id,
            @Param("executorEndpoint") String executorEndpoint,
            @Param("lastHeartTime") Date lastHeartTime);

}
