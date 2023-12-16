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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.JobScheduleRepository;
import com.oceanbase.odc.service.task.executor.executor.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.listener.TaskResultUploadEvent;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-30
 * @since 4.2.4
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class StdTaskFrameworkService implements TaskFrameworkService {

    @Autowired
    private JobScheduler jobScheduler;
    @Autowired
    private JobScheduleRepository jobScheduleRepository;

    @Autowired(required = false)
    private List<ResultHandleService> resultHandleServices;

    @Setter
    private EventPublisher publisher;

    @Autowired
    @Qualifier(value = "taskResultPublisherExecutor")
    private ThreadPoolTaskExecutor taskResultPublisherExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getJobIdentity() == null || taskResult.getJobIdentity().getId() == null) {
            log.warn("Job identity is null");
            return;
        }
        JobEntity je = find(taskResult.getJobIdentity().getId());

        if (je.getStatus() == TaskStatus.DESTROYED) {
            log.warn("task is terminated, ignore upload result.{}", JsonUtils.toJson(taskResult));
            return;
        }
        if (taskResult.getProgress() == je.getProgressPercentage() && taskResult.getTaskStatus() == je.getStatus()) {
            log.warn("task progress is not changed, ignore upload result.{}", JsonUtils.toJson(taskResult));
            return;
        }
        updateJobScheduleEntity(taskResult);
        if (resultHandleServices != null) {
            resultHandleServices.forEach(r -> r.handle(taskResult));
        }
        if (publisher != null) {
            taskResultPublisherExecutor.execute(() -> publisher.publishEvent(new TaskResultUploadEvent(taskResult)));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobEntity save(JobDefinition jd) {
        JobEntity jse = new JobEntity();
        jse.setJobDataJson(JsonUtils.toJson(jd.getJobData()));
        jse.setScheduleTimes(0);
        jse.setExecutionTimes(0);
        jse.setJobClass(jd.getJobClass().getCanonicalName());
        jse.setJobType(jd.getJobType());
        jse.setStatus(TaskStatus.PREPARING);
        return jobScheduleRepository.save(jse);
    }


    @Override
    public JobEntity find(Long id) {
        return jobScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JobDefinition getJobDefinition(Long id) {
        JobEntity entity = find(id);
        Class<? extends Task> cl;
        try {
            cl = (Class<? extends Task>) Class.forName(entity.getJobClass());
        } catch (ClassNotFoundException e) {
            throw new TaskRuntimeException(e);
        }
        return DefaultJobDefinition.builder()
                .jobData(JsonUtils.fromJson(entity.getJobDataJson(), new TypeReference<Map<String, String>>() {}))
                .jobClass(cl).build();
    }

    @Override
    public void startSuccess(Long id, String jobName) {
        JobEntity jobEntity = find(id);
        jobEntity.setStatus(TaskStatus.RUNNING);
        jobEntity.setJobName(jobName);
        // increment executionTimes
        jobEntity.setExecutionTimes(jobEntity.getExecutionTimes() + 1);
        // reset scheduleTimes to zero
        jobEntity.setScheduleTimes(0);
        jobScheduleRepository.updateJobNameAndStatus(jobEntity);
    }

    private void updateJobScheduleEntity(TaskResult taskResult) {
        JobEntity jse = find(taskResult.getJobIdentity().getId());
        jse.setResultJson(taskResult.getResultJson());
        jse.setStatus(taskResult.getTaskStatus());
        jse.setProgressPercentage(taskResult.getProgress());
        jse.setExecutor(JsonUtils.toJson(taskResult.getExecutorInfo()));
        jobScheduleRepository.update(jse);
    }

    @Override
    public void updateScheduleTimes(Long id, Integer scheduleTimes) {
        jobScheduleRepository.updateScheduleTimes(id, scheduleTimes);
    }

    @Override
    public void updateStatus(Long id,TaskStatus status) {
        jobScheduleRepository.updateStatus(id,status);
    }

    @Override
    public void update(JobEntity jobEntity) {
        jobScheduleRepository.update(jobEntity);
    }
}
