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
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.SensitiveDataUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
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
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.server.HeartbeatRequest;
import com.oceanbase.odc.service.task.executor.task.DefaultTaskResult;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.listener.DefaultJobProcessUpdateEvent;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
import com.oceanbase.odc.service.task.processor.DLMResultProcessor;
import com.oceanbase.odc.service.task.processor.LogicalDBChangeResultProcessor;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobDateUtils;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;

import lombok.NonNull;
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
    @Qualifier(value = "taskResultPullerExecutor")
    private ThreadPoolTaskExecutor taskResultPullerExecutor;

    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TaskExecutorClient taskExecutorClient;
    @Autowired
    private ExecutorEndpointManager executorEndpointManager;
    @Autowired
    private DLMResultProcessor dlmResultProcessor;
    @Autowired
    private LogicalDBChangeResultProcessor logicalDBChangeResultProcessor;

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
    public Page<JobEntity> findRunningJobs(int page, int size) {
        Specification<JobEntity> condition = Specification.where(getRecentDaySpec(RECENT_DAY))
                .and(SpecificationUtil.columnEqual(JobEntityColumn.STATUS, JobStatus.RUNNING));
        return page(condition, page, size);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobEntity save(@NonNull JobDefinition jd) {

        // set system job properties
        JobPropertiesUtils.setMonitorMode(jd.getJobProperties(), taskFrameworkProperties.getMonitorMode());
        Integer executorListenPort = taskFrameworkProperties.getK8sProperties().getExecutorListenPort();
        if (taskFrameworkProperties.getRunMode() == TaskRunMode.K8S && executorListenPort > 0) {
            JobPropertiesUtils.setExecutorListenPort(jd.getJobProperties(), executorListenPort);
        }

        // construct job entity
        JobEntity jse = new JobEntity();
        jse.setJobParametersJson(JsonUtils.toJson(jd.getJobParameters()));
        jse.setJobProperties(jd.getJobProperties());
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

    /**
     * TODO: why use taskResultPublisherExecutor here?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(TaskResult taskResult) {
        if (taskResult.getJobIdentity() == null || taskResult.getJobIdentity().getId() == null) {
            log.warn("Job identity is null");
            return;
        }
        if (taskResult.getStatus() == JobStatus.CANCELED) {
            log.warn("Job is canceled by odc server, this result is ignored.");
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
        if (!StringUtils.equals(je.getExecutorEndpoint(), taskResult.getExecutorEndpoint())) {
            log.info("Update executor endpoint, jobId={}, old={}, new={}", je.getId(), je.getExecutorEndpoint(),
                    taskResult.getExecutorEndpoint());
            int ret = updateExecutorEndpoint(je.getId(), taskResult.getExecutorEndpoint(), je);
            if (ret == 0) {
                log.warn("Update executor endpoint failed, jobId={}", je.getId());
            }
        }
        // TODO: update task entity only when progress changed
        int rows = updateTaskResult(taskResult, je);
        if (rows > 0) {
            taskResultPublisherExecutor
                    .execute(() -> publisher.publishEvent(new DefaultJobProcessUpdateEvent(taskResult)));

            if (publisher != null && taskResult.getStatus() != null && taskResult.getStatus().isTerminated()) {
                taskResultPublisherExecutor.execute(() -> publisher
                        .publishEvent(new JobTerminateEvent(taskResult.getJobIdentity(), taskResult.getStatus())));

                if (taskResult.getStatus() == JobStatus.FAILED) {
                    AlarmUtils.alarm(AlarmEventNames.TASK_EXECUTION_FAILED,
                            JsonUtils.createJsonNodeBuilder()
                                    .item("OrganizationId", je.getOrganizationId())
                                    .item("CreatorId", je.getCreatorId())
                                    .item("JobId", taskResult.getJobIdentity().getId())
                                    .item("Message", MessageFormat.format("Job execution failed, resultJson={0}",
                                            SensitiveDataUtils.mask(taskResult.getResultJson())))
                                    .build());
                }
            }
        }
    }

    @Override
    public void refreshResult(Long id) {
        taskResultPullerExecutor.execute(() -> {
            try {
                doRefreshResult(id);
            } catch (Exception e) {
                log.warn("Refresh job result failed, jobId={}, causeReason={}",
                        id, ExceptionUtils.getRootCauseReason(e));
            }
        });
    }

    @Override
    public boolean refreshLogMeta(Long id) {
        JobEntity je = find(id);
        // CANCELING is also a state within the running phase
        if (JobStatus.RUNNING != je.getStatus() && JobStatus.CANCELING != je.getStatus()) {
            log.warn("Job is not running, don't need to refresh log meta, jobId={}, currentStatus={}", id,
                    je.getStatus());
            return true;
        }
        try {
            String executorEndpoint = executorEndpointManager.getExecutorEndpoint(je);
            DefaultTaskResult result = taskExecutorClient.getResult(executorEndpoint, JobIdentity.of(id));
            if (je.getRunMode().isK8s() && MapUtils.isEmpty(result.getLogMetadata())) {
                log.info("Refresh log failed due to log have not uploaded,  jobId={}, currentStatus={}", je.getId(),
                        je.getStatus());
                return false;
            }
            saveOrUpdateLogMetadata(result, je.getId(), je.getStatus());
            if ("DLM".equals(je.getJobType())) {
                dlmResultProcessor.process(result);
            } else if (StringUtils.equalsIgnoreCase("LogicalDatabaseChange", je.getJobType())) {
                logicalDBChangeResultProcessor.process(result);
            }
            return true;
        } catch (Exception exception) {
            log.warn("Refresh log meta failed,errorMsg={}", exception.getMessage());
            return false;
        }
    }

    private void doRefreshResult(Long id) throws JobException {
        JobEntity je = find(id);
        // CANCELING is also a state within the running phase
        if (JobStatus.RUNNING != je.getStatus()) {
            log.info("Job is not running, ignore refresh, jobId={}, currentStatus={}", id, je.getStatus());
            return;
        }

        String executorEndpoint = executorEndpointManager.getExecutorEndpoint(je);
        DefaultTaskResult result = taskExecutorClient.getResult(executorEndpoint, JobIdentity.of(id));
        DefaultTaskResult previous = JsonUtils.fromJson(je.getResultJson(), DefaultTaskResult.class);

        if (!updateHeartbeatTime(id)) {
            log.warn("Update lastHeartbeatTime failed, the job may finished or deleted already, jobId={}", id);
            return;
        }
        if (!result.progressChanged(previous)) {
            log.info("Progress not changed, skip update result to metadb, jobId={}, currentProgress={}",
                    id, result.getProgress());
            return;
        }
        log.info("Progress changed, will update result, jobId={}, currentProgress={}", id, result.getProgress());
        if ("DLM".equals(je.getJobType())) {
            dlmResultProcessor.process(result);
        } else if (StringUtils.equalsIgnoreCase("LogicalDatabaseChange", je.getJobType())) {
            logicalDBChangeResultProcessor.process(result);
        }
        saveOrUpdateLogMetadata(result, je.getId(), je.getStatus());

        if (result.getStatus().isTerminated() && MapUtils.isEmpty(result.getLogMetadata())) {
            log.info("Job is finished but log have not uploaded, continue monitor result, jobId={}, currentStatus={}",
                    je.getId(), je.getStatus());
            return;
        }

        int rows = updateTaskResult(result, je);
        if (rows == 0) {
            log.warn("Update task result failed, the job may finished or deleted already, jobId={}", id);
            return;
        }
        taskResultPublisherExecutor
                .execute(() -> publisher.publishEvent(new DefaultJobProcessUpdateEvent(result)));

        if (publisher != null && result.getStatus() != null && result.getStatus().isTerminated()) {
            taskResultPublisherExecutor.execute(() -> publisher
                    .publishEvent(new JobTerminateEvent(result.getJobIdentity(), result.getStatus())));

            // TODO maybe we can destroy the pod there.
            if (result.getStatus() == JobStatus.FAILED) {
                AlarmUtils.alarm(AlarmEventNames.TASK_EXECUTION_FAILED,
                        JsonUtils.createJsonNodeBuilder()
                                .item("OrganizationId", je.getOrganizationId())
                                .item("CreatorId", je.getCreatorId())
                                .item("JobId", result.getJobIdentity().getId())
                                .item("Message", MessageFormat.format("Job execution failed, resultJson={0}",
                                        SensitiveDataUtils.mask(result.getResultJson())))
                                .build());
            }
        }
    }

    private boolean updateHeartbeatTime(Long id) {
        int rows = jobRepository.updateHeartbeatTime(id, JobStatus.RUNNING);
        if (rows > 0) {
            log.info("Update lastHeartbeatTime success, jobId={}", id);
            return true;
        } else {
            log.warn("Update lastHeartbeatTime failed, jobId={}", id);
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleHeart(@NonNull HeartbeatRequest heart) {
        PreConditions.notNull(heart.getJobIdentity(), "jobIdentity");
        PreConditions.notNull(heart.getJobIdentity().getId(), "jobIdentity.id");
        PreConditions.notBlank(heart.getExecutorEndpoint(), "executorEndpoint");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.LAST_HEART_TIME, JobDateUtils.getCurrentDate());
        update.where(cb.equal(e.get(JobEntityColumn.ID), heart.getJobIdentity().getId()),
                cb.equal(e.get(JobEntityColumn.EXECUTOR_ENDPOINT), heart.getExecutorEndpoint()));

        int affectedRows = entityManager.createQuery(update).executeUpdate();
        if (affectedRows == 0) {
            log.warn("Heartbeat update failed, jobIdentity={}, executorEndpoint={}",
                    heart.getJobIdentity(), heart.getExecutorEndpoint());
        } else {
            log.info("Heartbeat update success, jobIdentity={}, executorEndpoint={}",
                    heart.getJobIdentity(), heart.getExecutorEndpoint());
        }
    }

    private int updateExecutorEndpoint(Long id, String executorEndpoint, JobEntity currentJob) {
        return jobRepository.updateExecutorEndpoint(id, executorEndpoint, currentJob.getStatus());
    }

    private int updateTaskResult(TaskResult taskResult, JobEntity currentJob) {
        JobEntity jse = new JobEntity();
        if ("DLM".equals(currentJob.getJobType())) {
            dlmResultProcessor.process(taskResult);
        } else if (StringUtils.equalsIgnoreCase("LogicalDatabaseChange", currentJob.getJobType())) {
            logicalDBChangeResultProcessor.process(taskResult);
        }
        jse.setResultJson(taskResult.getResultJson());
        jse.setStatus(taskResult.getStatus());
        jse.setProgressPercentage(taskResult.getProgress());
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
        update.set(JobEntityColumn.EXECUTOR_ENDPOINT, null);

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
    @Transactional(rollbackFor = Exception.class)
    public int updateJobParameters(Long id, String jobParametersJson) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.JOB_PARAMETERS_JSON, jobParametersJson);
        update.where(cb.equal(e.get(JobEntityColumn.ID), id));
        return entityManager.createQuery(update).executeUpdate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateExecutorEndpoint(Long id, String executorEndpoint) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JobEntity> update = cb.createCriteriaUpdate(JobEntity.class);
        Root<JobEntity> e = update.from(JobEntity.class);
        update.set(JobEntityColumn.EXECUTOR_ENDPOINT, executorEndpoint);
        update.where(cb.equal(e.get(JobEntityColumn.ID), id),
                cb.equal(e.get(JobEntityColumn.STATUS), JobStatus.RUNNING));
        int ret = entityManager.createQuery(update).executeUpdate();
        if (ret == 0) {
            log.warn("Update executor endpoint failed, jobId={}", id);
        } else {
            log.info("Update executor endpoint success, jobId={}, executorEndpoint={}", id, executorEndpoint);
        }
        return ret;
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
