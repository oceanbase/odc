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
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.executor.HeartbeatRequest;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobDefinition;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
public interface TaskFrameworkService {

    JobEntity save(JobDefinition jd);

    void handleResult(TaskResult taskResult);

    void refreshResult(Long id);

    boolean refreshLogMetaForCancelJob(Long id);

    void handleHeart(HeartbeatRequest heart);

    JobEntity find(Long id);

    Page<JobEntity> findCancelingJob(int page, int size);

    Page<JobEntity> findTerminalJob(int page, int size);

    Page<ResourceEntity> findAbandonedResource(int page, int size);

    JobEntity findWithPessimisticLock(Long id);

    Page<JobEntity> find(JobStatus status, int page, int size);

    Page<JobEntity> find(List<JobStatus> status, int page, int size);

    Page<JobEntity> findHeartTimeTimeoutJobs(int timeoutSeconds, int page, int size);

    Page<JobEntity> findIncompleteJobs(int page, int size);

    Page<JobEntity> findRunningJobs(int page, int size);

    /**
     * count jobs which process is running
     */
    long countRunningJobs(TaskRunMode runMode);

    // api for start job v1
    int startSuccess(Long id, ResourceID resourceID, String executorIdentifier, JobContext jobContext);

    // api for start job v2
    int startSuccess(Long id, String executorIdentifier, JobContext jobContext);

    int beforeStart(Long id);

    int updateJobToCanceling(Long id, JobStatus oldStatus);

    int updateJobParameters(Long id, String jobParametersJson);

    int updateExecutorEndpoint(Long id, String executorEndpoint);

    int updateExecutorToDestroyed(Long id);

    int updateStatusDescriptionByIdOldStatus(Long id, JobStatus oldStatus, JobStatus newStatus, String description);

    int updateStatusToFailedWhenHeartTimeout(Long id, int heartTimeoutSeconds, String description);

    Optional<String> findByJobIdAndAttributeKey(Long jobId, String attributeKey);

    Map<String, String> getJobAttributes(Long jobId);

    boolean isJobFinished(Long id);

    void propagateTaskResult(String jobType, TaskResult taskResult);

    void saveOrUpdateLogMetadata(TaskResult taskResult, Long jobId, JobStatus currentStatus);

    int updateTaskResult(TaskResult taskResult, JobEntity currentJob, JobStatus expectedStatus);

    void publishEvent(TaskResult result, JobEntity jobEntity, JobStatus expectedJobStatus);

    ThreadPoolTaskExecutor getTaskResultPullerExecutor();

    boolean updateHeartbeatWithExpectStatus(Long id, JobStatus expectStatus);

    Page<JobEntity> findNeedStoppedJobs(int page, int size);

    Page<JobEntity> findNeedPullResultJobs(int page, int size);

    int updateStatusByIdOldStatus(Long id, JobStatus oldStatus, JobStatus newStatus);
}
