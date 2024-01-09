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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.JobAttributeEntity;
import com.oceanbase.odc.metadb.task.JobAttributeRepository;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.JobRepository;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.executor.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.listener.TaskResultUploadEvent;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.util.JobDateUtils;

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
    private JobRepository jobRepository;
    @Autowired
    private JobAttributeRepository jobAttributeRepository;

    @Autowired(required = false)
    private List<ResultHandleService> resultHandleServices;

    @Setter
    private EventPublisher publisher;

    @Autowired
    @Qualifier(value = "taskResultPublisherExecutor")
    private ThreadPoolTaskExecutor taskResultPublisherExecutor;

    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getJobIdentity() == null || taskResult.getJobIdentity().getId() == null) {
            log.warn("Job identity is null");
            return;
        }
        JobEntity je = find(taskResult.getJobIdentity().getId());
        if (je.getStatus().isTerminated()) {
            log.warn("Job {} is finished, ignore result", je.getId());
            return;
        }

        updateJobScheduleEntity(taskResult);
        if (resultHandleServices != null) {
            resultHandleServices.forEach(r -> r.handle(taskResult));
        }
        if (publisher != null) {
            taskResultPublisherExecutor
                    .execute(() -> publisher.publishEvent(new TaskResultUploadEvent(taskResult)));
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobEntity save(JobDefinition jd) {
        JobEntity jse = new JobEntity();
        jse.setJobParametersJson(JsonUtils.toJson(jd.getJobParameters()));
        jse.setJobPropertiesJson(JsonUtils.toJson(jd.getJobProperties()));
        jse.setExecutionTimes(0);
        jse.setJobClass(jd.getJobClass().getCanonicalName());
        jse.setJobType(jd.getJobType());
        jse.setStatus(JobStatus.PREPARING);
        jse.setRunMode(taskFrameworkProperties.getRunMode().name());
        return jobRepository.save(jse);
    }


    @Override
    public JobEntity find(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }

    @Override
    public List<JobEntity> find(List<JobStatus> status, int offset, int limit) {
        // fetch recent 30 days jobs
        Calendar cal = Calendar.getInstance();
        cal.setTime(JobDateUtils.getCurrentDate());
        cal.add(Calendar.DATE, -30);

        return entityManager.createQuery("from JobEntity where status in (:status) and createTime > :date "
                + " order by createTime asc", JobEntity.class)
                .setParameter("date", cal.getTime())
                .setParameter("status", status)
                .setFirstResult(offset)
                .setMaxResults(limit).getResultList();
    }

    @Override
    public List<JobEntity> find(JobStatus status, int offset, int limit) {
        return find(Collections.singletonList(status), offset, limit);
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
                .jobParameters(
                        JsonUtils.fromJson(entity.getJobParametersJson(), new TypeReference<Map<String, String>>() {}))
                .jobClass(cl).build();
    }

    @Override
    public void startSuccess(Long id, String executorIdentifier) {
        JobEntity jobEntity = find(id);
        jobEntity.setStatus(JobStatus.RUNNING);
        jobEntity.setExecutorIdentifier(executorIdentifier);
        // increment executionTimes
        jobEntity.setExecutionTimes(jobEntity.getExecutionTimes() + 1);
        jobEntity.setStartedTime(JobDateUtils.getCurrentDate());
        jobRepository.updateJobExecutorIdentifierAndStatus(jobEntity);
    }

    @Override
    public void stopSuccess(Long id) {
        jobRepository.updateStatus(id, JobStatus.CANCELED);
        log.info("Update job {} status to {}", id, JobStatus.CANCELED.name());
    }

    private void updateJobScheduleEntity(TaskResult taskResult) {
        JobEntity jse = find(taskResult.getJobIdentity().getId());
        jse.setResultJson(taskResult.getResultJson());
        jse.setStatus(taskResult.getStatus());
        jse.setProgressPercentage(taskResult.getProgress());
        jse.setExecutorEndpoint(taskResult.getExecutorEndpoint());
        jse.setLastReportTime(JobDateUtils.getCurrentDate());
        if (taskResult.getStatus() != null && taskResult.getStatus().isTerminated()) {
            jse.setFinishedTime(JobDateUtils.getCurrentDate());
        }
        jobRepository.update(jse);

        if (taskResult.getLogMetadata() != null && taskResult.getStatus().isTerminated()) {
            taskResult.getLogMetadata().forEach((k, v) -> {
                JobAttributeEntity jobAttribute = new JobAttributeEntity();
                jobAttribute.setJobId(jse.getId());
                jobAttribute.setAttributeKey(k);
                jobAttribute.setAttributeValue(v);
                jobAttributeRepository.save(jobAttribute);
            });

        }
    }

    @Override
    public void updateDescription(Long id, String description) {
        jobRepository.updateDescription(id, description);
    }

    @Override
    public void updateStatus(Long id, JobStatus status) {
        jobRepository.updateStatus(id, status);
    }

    @Override
    public boolean isJobFinished(Long id) {
        JobEntity jse = find(id);
        return jse.getStatus().isTerminated();
    }

    @Override
    public String findByJobIdAndAttributeKey(Long jobId, String attributeKey) {
        JobAttributeEntity attributeEntity = jobAttributeRepository.findByJobIdAndAttributeKey(jobId, attributeKey);
        Verify.notNull(attributeEntity, attributeKey);
        return attributeEntity.getAttributeValue();
    }
}
