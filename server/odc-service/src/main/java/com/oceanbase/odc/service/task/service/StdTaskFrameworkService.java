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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.task.JobAttributeEntity;
import com.oceanbase.odc.metadb.task.JobAttributeRepository;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.JobRepository;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobAttributeEntityColumn;
import com.oceanbase.odc.service.task.constants.JobEntityColumn;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.server.HeartRequest;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.listener.DefaultJobProcessUpdateEvent;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
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
@Lazy
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class StdTaskFrameworkService implements TaskFrameworkService {

    private static final int RECENT_DAY = 30;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobAttributeRepository jobAttributeRepository;

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
        return jobRepository.findByIdNative(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_TASK, "id", id));
    }

    @Override
    public JobEntity findWithPessimisticLock(Long id) {
        return entityManager.find(JobEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public Page<JobEntity> find(JobStatus status, int page, int size) {
        return find(Collections.singletonList(status), page, size);
    }

    @Override
    public Page<JobEntity> find(List<JobStatus> status, int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnIn(JobEntityColumn.STATUS, status));
        return page(condition, page, size);
    }

    @Override
    public Page<JobEntity> findCancelingJob(int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnEqual(JobEntityColumn.STATUS, JobStatus.CANCELING))
                .and(getExecutorSpec());
        return page(condition, page, size);
    }

    @Override
    public Page<JobEntity> findTerminalJob(int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnIn(JobEntityColumn.STATUS,
                        Lists.newArrayList(JobStatus.CANCELED, JobStatus.DONE, JobStatus.FAILED)))
                .and(SpecificationUtil.columnIsNull(JobEntityColumn.EXECUTOR_DESTROYED_TIME))
                .and(getExecutorSpec());
        return page(condition, page, size);
    }

    @Override
    public Page<JobEntity> findHeartTimeTimeoutJobs(int timeoutSeconds, int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnEqual(JobEntityColumn.STATUS, JobStatus.RUNNING))
                .and((root, query, cb) -> getHeartTimeoutPredicate(root, cb, timeoutSeconds));
        return page(condition, page, size);
    }

    @Override
    public Page<JobEntity> findIncompleteJobs(int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnIn(JobEntityColumn.STATUS,
                        Lists.newArrayList(JobStatus.PREPARING, JobStatus.RETRYING, JobStatus.RUNNING)));
        return page(condition, page, size);
    }

    @Override
    public long countRunningNeverHeartJobs(int neverHeartSeconds) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        // sql like:
        // select count(*) from job_job where
        // create_time > now() - 30d
        // and last_heart_time is null
        // and status = 'RUNNING'
        // and started_time < now() - neverHeartSeconds
        Root<JobEntity> root = query.from(JobEntity.class);
        query.select(cb.count(root));
        query.where(
                cb.greaterThan(root.get(JobEntityColumn.CREATE_TIME),
                        JobDateUtils.getCurrentDateSubtractDays(RECENT_DAY)),
                cb.isNull(root.get(JobEntityColumn.LAST_HEART_TIME)),
                cb.equal(root.get(JobEntityColumn.STATUS), JobStatus.RUNNING),
                cb.lessThan(root.get(JobEntityColumn.STARTED_TIME),
                        JobDateUtils.getCurrentDateSubtractSeconds(neverHeartSeconds)),
                executorPredicate(root, cb));

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public long countRunningJobs(TaskRunMode runMode) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);

        // sql like:
        // select count(*) from job_job where
        // create_time > now() - 30d
        // and run_mode = 'runMode'
        // and status not in ( 'PREPARING', 'RETRYING')
        // and executor_destroyed_time is null

        Root<JobEntity> root = query.from(JobEntity.class);
        query.select(cb.count(root));
        query.where(
                cb.greaterThan(root.get(JobEntityColumn.CREATE_TIME),
                        JobDateUtils.getCurrentDateSubtractDays(RECENT_DAY)),
                cb.equal(root.get(JobEntityColumn.RUN_MODE), runMode),
                root.get(JobEntityColumn.STATUS).in(JobStatus.PREPARING, JobStatus.RETRYING).not(),
                cb.isNull(root.get(JobEntityColumn.EXECUTOR_DESTROYED_TIME)),
                executorPredicate(root, cb));
        return entityManager.createQuery(query).getSingleResult();
    }


    private Specification<JobEntity> getExecutorSpec() {
        return (root, query, cb) -> executorPredicate(root, cb);
    }

    private Predicate executorPredicate(Root<JobEntity> root, CriteriaBuilder cb) {
        Predicate k8sCondition = cb.equal(root.get(JobEntityColumn.RUN_MODE), TaskRunMode.K8S);

        Predicate processCondition = cb.and(
                cb.equal(root.get(JobEntityColumn.RUN_MODE), TaskRunMode.PROCESS),
                cb.or(cb.like(root.get(JobEntityColumn.EXECUTOR_IDENTIFIER),
                        "%" + StringUtils.escapeLike(SystemUtils.getLocalIpAddress()) + "%"),
                        cb.isNull(root.get(JobEntityColumn.EXECUTOR_IDENTIFIER))));

        return cb.or(processCondition, k8sCondition);
    }

    private Page<JobEntity> page(Specification<JobEntity> specification, int page, int size) {
        return jobRepository.findAll(specification, PageRequest.of(page, size));
    }

    private Specification<JobEntity> getRecentDaySpec(int days) {
        return SpecificationUtil.columnLate(JobEntityColumn.CREATE_TIME, JobDateUtils.getCurrentDateSubtractDays(days));
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
        jse.setRunMode(taskFrameworkProperties.getRunMode());

        jse.setCreatorId(TraceContextHolder.getUserId());
        jse.setOrganizationId(TraceContextHolder.getOrganizationId());
        return jobRepository.save(jse);
    }

    @Override
    public int startSuccess(Long id, String executorIdentifier) {
        JobEntity jobEntity = find(id);
        jobEntity.setExecutorIdentifier(executorIdentifier);
        return jobRepository.updateJobExecutorIdentifierById(jobEntity);
    }

    @Override
    public int beforeStart(Long id) {
        JobEntity jobEntity = find(id);
        Date currentDate = JobDateUtils.getCurrentDate();
        jobEntity.setStatus(JobStatus.RUNNING);
        // increment executionTimes
        jobEntity.setExecutionTimes(jobEntity.getExecutionTimes() + 1);
        jobEntity.setStartedTime(currentDate);
        if (jobEntity.getLastHeartTime() != null) {
            jobEntity.setLastHeartTime(null);
        }
        if (jobEntity.getExecutorDestroyedTime() != null) {
            jobEntity.setExecutorDestroyedTime(null);
        }
        return jobRepository.updateJobStatusAndExecutionTimesById(jobEntity);
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
        saveOrUpdateLogMetadata(taskResult, je.getId(), je.getStatus());

        if (je.getStatus().isTerminated() || je.getStatus() == JobStatus.CANCELING) {
            log.warn("Job is finished, ignore result, jobId={}, currentStatus={}", je.getId(), je.getStatus());
            return;
        }
        int rows = updateJobScheduleEntity(taskResult, je);
        if (rows > 0) {
            taskResultPublisherExecutor
                    .execute(() -> publisher.publishEvent(new DefaultJobProcessUpdateEvent(taskResult)));
            if (publisher != null && taskResult.getStatus() != null && taskResult.getStatus().isTerminated()) {
                taskResultPublisherExecutor.execute(() -> publisher
                        .publishEvent(new JobTerminateEvent(taskResult.getJobIdentity(), taskResult.getStatus())));
                if (taskResult.getStatus() == JobStatus.FAILED) {
                    AlarmUtils.alarm(AlarmEventNames.TASK_EXECUTION_FAILED,
                            MessageFormat.format("Job execution failed, jobId={0}",
                                    taskResult.getJobIdentity().getId()));
                }
            }
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

    private int updateJobScheduleEntity(TaskResult taskResult, JobEntity currentJob) {
        JobEntity jse = new JobEntity();
        jse.setResultJson(taskResult.getResultJson());
        jse.setStatus(taskResult.getStatus());
        jse.setProgressPercentage(taskResult.getProgress());
        jse.setExecutorEndpoint(taskResult.getExecutorEndpoint());
        jse.setLastReportTime(JobDateUtils.getCurrentDate());
        if (taskResult.getStatus() != null && taskResult.getStatus().isTerminated()) {
            jse.setFinishedTime(JobDateUtils.getCurrentDate());
        }
        return jobRepository.updateReportResult(jse, currentJob.getId(), currentJob.getStatus());
    }

    private void saveOrUpdateLogMetadata(TaskResult taskResult, Long jobId, JobStatus currentStatus) {
        if (taskResult.getLogMetadata() != null) {
            log.info("Save or update log metadata, jobId={}, currentStatus={}, taskResult={}",
                    jobId, currentStatus, JsonUtils.toJson(taskResult));
            taskResult.getLogMetadata().forEach((k, v) -> {
                // log key may exist if job is retrying
                Optional<String> logValue = findByJobIdAndAttributeKey(jobId, k);
                if (logValue.isPresent()) {
                    updateJobAttributeValue(jobId, k, v);
                } else {
                    JobAttributeEntity jobAttribute = new JobAttributeEntity();
                    jobAttribute.setJobId(jobId);
                    jobAttribute.setAttributeKey(k);
                    jobAttribute.setAttributeValue(v);
                    jobAttributeRepository.save(jobAttribute);
                }
            });
        }
    }

    private void updateJobAttributeValue(Long id, String key, String value) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobAttributeEntity> update = cb.createCriteriaUpdate(JobAttributeEntity.class);
        Root<JobAttributeEntity> e = update.from(JobAttributeEntity.class);
        update.set(JobAttributeEntityColumn.ATTRIBUTE_VALUE, value);
        update.where(cb.equal(e.get(JobAttributeEntityColumn.ID), id),
                cb.equal(e.get(JobAttributeEntityColumn.ATTRIBUTE_KEY), key));
        entityManager.createQuery(update).executeUpdate();
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
    public int updateStatusToFailedWhenHeartTimeout(Long id, int heartTimeoutSeconds, String description) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> root = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, JobStatus.FAILED);
        update.set(JobEntityColumn.FINISHED_TIME, JobDateUtils.getCurrentDate());
        update.set(JobEntityColumn.DESCRIPTION, description);

        update.where(cb.equal(root.get(JobEntityColumn.ID), id),
                cb.and(getHeartTimeoutPredicate(root, cb, heartTimeoutSeconds)));

        return entityManager.createQuery(update).executeUpdate();
    }


    // condition like:
    // and status = 'RUNNING'
    // and (
    // (last_heart_time is not null and last_heart_time < now()- ?) or
    // (last_heart_time is null and started_time < now()- ?)
    // )
    private Predicate getHeartTimeoutPredicate(Root<JobEntity> root, CriteriaBuilder cb, int heartTimeoutSeconds) {
        return cb.and(
                cb.equal(root.get(JobEntityColumn.STATUS), JobStatus.RUNNING),
                cb.or(
                        cb.and(cb.isNotNull(root.get(JobEntityColumn.LAST_HEART_TIME)),
                                cb.lessThan(root.get(JobEntityColumn.LAST_HEART_TIME),
                                        JobDateUtils.getCurrentDateSubtractSeconds(heartTimeoutSeconds))),

                        cb.and(cb.isNull(root.get(JobEntityColumn.LAST_HEART_TIME)),
                                cb.lessThan(root.get(JobEntityColumn.STARTED_TIME),
                                        JobDateUtils.getCurrentDateSubtractSeconds(heartTimeoutSeconds)))));
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(Long id, JobStatus oldStatus,
            JobStatus newStatus, String description) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.STATUS, newStatus);
        update.set(JobEntityColumn.DESCRIPTION, description);
        update.set(JobEntityColumn.EXECUTOR_DESTROYED_TIME, null);
        update.set(JobEntityColumn.LAST_HEART_TIME, null);

        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.equal(e.get(JobEntityColumn.STATUS), oldStatus),
                cb.isNotNull(e.get(JobEntityColumn.EXECUTOR_DESTROYED_TIME)));

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

    @Override
    public int updateJobParameters(Long id, String jobParametersJson) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.JOB_PARAMETERS_JSON, jobParametersJson);
        update.where(cb.equal(e.get(JobEntityColumn.ID), id));
        return entityManager.createQuery(update).executeUpdate();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateExecutorToDestroyed(Long id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.EXECUTOR_DESTROYED_TIME, JobDateUtils.getCurrentDate());

        update.where(cb.equal(e.get(JobEntityColumn.ID), id));
        return entityManager.createQuery(update).executeUpdate();
    }

    @Override
    public boolean isJobFinished(Long id) {
        JobEntity jse = find(id);
        return jse.getStatus().isTerminated();
    }

    @Override
    public Optional<String> findByJobIdAndAttributeKey(Long jobId, String attributeKey) {
        JobAttributeEntity attributeEntity = jobAttributeRepository.findByJobIdAndAttributeKey(jobId, attributeKey);
        return Objects.isNull(attributeEntity) ? Optional.empty() : Optional.of(attributeEntity.getAttributeValue());
    }

}
