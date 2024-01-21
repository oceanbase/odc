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

package com.oceanbase.odc.service.task.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.server.HeartRequest;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobDefinition;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
public interface TaskFrameworkService {

    JobEntity save(JobDefinition jd);

    void handleResult(TaskResult taskResult);

    void handleHeart(HeartRequest heart);

    JobEntity find(Long id);

    JobEntity findWithPessimisticLock(Long id);

    Page<JobEntity> find(JobStatus status, int page, int size);

    Page<JobEntity> find(List<JobStatus> status, int page, int size);

    Page<JobEntity> findHeartTimeTimeoutJobs(int timeoutSeconds, int page, int size);

    JobDefinition getJobDefinition(Long id);

    int startSuccess(Long id, String executorIdentifier);

    void updateDescription(Long id, String description);

    int updateJobToCanceling(Long id, JobStatus oldStatus);

    int updateExecutorToDestroyed(Long id);

    int updateStatusDescriptionByIdOldStatus(Long id, JobStatus oldStatus, JobStatus newStatus, String description);

    int updateStatusToCanceledWhenHeartTimeout(Long id, int heartTimeoutSeconds, String description);

    int updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(Long id, JobStatus oldStatus, JobStatus newStatus,
            String description);

    String findByJobIdAndAttributeKey(Long jobId, String attributeKey);

    boolean isJobFinished(Long id);
}
