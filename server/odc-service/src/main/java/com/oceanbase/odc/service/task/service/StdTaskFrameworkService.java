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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.jpa.SpecificationUtil;
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
import com.oceanbase.odc.service.task.constants.JobEntityColumn;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.executor.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.task.HeartRequest;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.listener.DestroyExecutorEvent;
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

    private static final int RECENT_DAY = 30;
    private static final String STATUS_COLUMN = "status";
    private static final String LAST_HEART_TIME_COLUMN = "lastHeartTime";

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
    public JobEntity find(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }

    @Override
    public JobEntity findWithLock(Long id) {
        return entityManager.find(JobEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public Page<JobEntity> find(JobStatus status, int page, int size) {
        return find(Collections.singletonList(status), page, size);
    }

    @Override
    public Page<JobEntity> find(List<JobStatus> status, int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnIn(STATUS_COLUMN, status));
        return page(condition, page, size);
    }

    @Override
    public Page<JobEntity> findHeartTimeTimeoutJobs(int timeoutSeconds, int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(getTimeoutOnColumnSpec(LAST_HEART_TIME_COLUMN, timeoutSeconds))
                .and(SpecificationUtil.columnEqual(STATUS_COLUMN, JobStatus.RUNNING));
        return page(condition, page, size);
    }

    private Page<JobEntity> page(Specification<JobEntity> specification, int page, int size) {
        return jobRepository.findAll(specification, PageRequest.of(page, size));
    }

    private Specification<JobEntity> getTimeoutOnColumnSpec(String referenceColumn, int timeoutSeconds) {
        return SpecificationUtil.columnBefore(referenceColumn,
                JobDateUtils.getCurrentDateSubtractSeconds(timeoutSeconds));
    }

    private Specification<JobEntity> getRecentDaySpec(int days) {
        return SpecificationUtil.columnLate("createTime", JobDateUtils.getCurrentDateSubtractDays(days));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JobDefinition getJobDefinition(Long id) {
        JobEntity entity = find(id);
        Class<? extends Task<?>> cl;
        try {
            cl = (Class<? extends Task<?>>) Class.forName(entity.getJobClass());
        } catch (ClassNotFoundException e) {
            throw new TaskRuntimeException(e);
        }
        return DefaultJobDefinition.builder()
                .jobParameters(
                        JsonUtils.fromJson(entity.getJobParametersJson(), new TypeReference<Map<String, String>>() {}))
                .jobClass(cl).build();
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
    public void startSuccess(Long id, String executorIdentifier) {
        JobEntity jobEntity = find(id);
        Date currentDate = JobDateUtils.getCurrentDate();
        jobEntity.setStatus(JobStatus.RUNNING);
        jobEntity.setExecutorIdentifier(executorIdentifier);
        // increment executionTimes
        jobEntity.setExecutionTimes(jobEntity.getExecutionTimes() + 1);
        // set current date as first heart time
        jobEntity.setLastHeartTime(currentDate);
        jobEntity.setStartedTime(currentDate);
        if (jobEntity.getExecutorDestroyedTime() != null) {
            jobEntity.setExecutorDestroyedTime(null);
        }
        jobRepository.updateJobExecutorIdentifierAndStatusById(jobEntity);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getJobIdentity() == null || taskResult.getJobIdentity().getId() == null) {
            log.warn("Job identity is null");
            return;
        }
        JobEntity je = find(taskResult.getJobIdentity().getId());
        if (je == null) {
            log.warn("Job identity is not exists by id {}", taskResult.getJobIdentity().getId());
            return;
        }
        if (je.getStatus().isTerminated()) {
            log.warn("Job {} is finished, ignore result", je.getId());
            return;
        }

        updateJobScheduleEntity(taskResult);
        if (resultHandleServices != null) {
            resultHandleServices.forEach(r -> r.handle(taskResult));
        }
        if (publisher != null && taskResult.getStatus() != null && taskResult.getStatus().isTerminated()) {
            taskResultPublisherExecutor
                    .execute(() -> publisher.publishEvent(new DestroyExecutorEvent(taskResult.getJobIdentity())));
        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleHeart(HeartRequest heart) {
        if (heart.getJobIdentity() == null || heart.getJobIdentity().getId() == null ||
                heart.getExecutorEndpoint() == null) {
            return;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.LAST_HEART_TIME, JobDateUtils.getCurrentDate());
        update.where(cb.equal(e.get(JobEntityColumn.ID), heart.getJobIdentity().getId()),
                cb.equal(e.get(JobEntityColumn.EXECUTOR_ENDPOINT), heart.getExecutorEndpoint()));

        entityManager.createQuery(update).executeUpdate();

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDescription(Long id, String description) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.DESCRIPTION, description);
        update.where(cb.equal(e.get(JobEntityColumn.ID), id));

        entityManager.createQuery(update).executeUpdate();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStatusDescriptionByIdOldStatus(Long id, JobStatus oldStatus, JobStatus newStatus,
            String description) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, newStatus);
        update.set(JobEntityColumn.FINISHED_TIME, JobDateUtils.getCurrentDate());
        update.set(JobEntityColumn.DESCRIPTION, description);

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.equal(e.get(JobEntityColumn.STATUS), oldStatus));

        return entityManager.createQuery(update).executeUpdate();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStatusToCanceledWhenHeartTimeout(Long id, int heartTimeoutSeconds, String description) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, JobStatus.CANCELED);
        update.set(JobEntityColumn.FINISHED_TIME, JobDateUtils.getCurrentDate());
        update.set(JobEntityColumn.DESCRIPTION, description);

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.lessThan(e.get(JobEntityColumn.LAST_HEART_TIME),
                        JobDateUtils.getCurrentDateSubtractSeconds(heartTimeoutSeconds)));

        return entityManager.createQuery(update).executeUpdate();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(Long id, JobStatus oldStatus,
            JobStatus newStatus, String description) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, newStatus);
        if (newStatus.isTerminated()) {
            update.set(JobEntityColumn.FINISHED_TIME, JobDateUtils.getCurrentDate());
        }
        update.set(JobEntityColumn.DESCRIPTION, description);

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.equal(e.get(JobEntityColumn.STATUS), oldStatus),
                cb.isNull(e.get(JobEntityColumn.EXECUTOR_DESTROYED_TIME)));

        return entityManager.createQuery(update).executeUpdate();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateJobToCanceling(Long id, JobStatus oldStatus) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, JobStatus.CANCELING);
        update.set(JobEntityColumn.CANCELLING_TIME, JobDateUtils.getCurrentDate());

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.equal(e.get(JobEntityColumn.STATUS), oldStatus));

        return entityManager.createQuery(update).executeUpdate();

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateExecutorToDestroyed(Long id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.EXECUTOR_DESTROYED_TIME, JobDateUtils.getCurrentDate());

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.isNull(e.get(JobEntityColumn.EXECUTOR_DESTROYED_TIME)));

        return entityManager.createQuery(update).executeUpdate();
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
