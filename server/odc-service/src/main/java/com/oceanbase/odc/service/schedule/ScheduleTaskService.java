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
package com.oceanbase.odc.service.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.JobRepository;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseChangeService;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.factory.ScheduleResponseMapperFactory;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;
import com.oceanbase.odc.service.schedule.model.DataArchiveClearParameters;
import com.oceanbase.odc.service.schedule.model.DataArchiveRollbackParameters;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.QueryScheduleTaskParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskListOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskListOverviewMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskOverviewMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanAttributes;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanTaskResult;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/30 19:07
 * @Descripition:
 */

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class ScheduleTaskService {

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private QuartzJobService quartzJobService;

    @Autowired
    private DLMService dlmService;

    @Autowired
    private TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private TaskDispatchChecker dispatchChecker;

    @Autowired
    private RequestDispatcher requestDispatcher;

    @Autowired
    private ScheduleResponseMapperFactory scheduleResponseMapperFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogicalDatabaseChangeService logicalDatabaseChangeService;

    @Autowired
    private JobRepository jobRepository;

    private final ScheduleTaskMapper scheduleTaskMapper = ScheduleTaskMapper.INSTANCE;

    public ScheduleTaskDetailResp getScheduleTaskDetailResp(Long id, Long scheduleId) {
        ScheduleTask scheduleTask = nullSafeGetByIdAndScheduleId(id, scheduleId);
        ScheduleTaskDetailResp res = new ScheduleTaskDetailResp();
        res.setId(scheduleTask.getId());
        res.setType(ScheduleTaskType.valueOf(scheduleTask.getJobGroup()));
        res.setStatus(scheduleTask.getStatus());
        res.setFireTime(scheduleTask.getFireTime());
        res.setCreateTime(scheduleTask.getCreateTime());
        res.setUpdateTime(scheduleTask.getUpdateTime());
        switch (res.getType()) {
            case DATA_ARCHIVE:
            case DATA_ARCHIVE_ROLLBACK:
            case DATA_ARCHIVE_DELETE:
            case DATA_DELETE: {
                res.setExecutionDetails(dlmService.getExecutionDetailByScheduleTaskId(scheduleTask.getId()));
                break;
            }
            case SQL_PLAN:
                // sql plan task detail should display sql content
                res.setParameters(JsonUtils.toJson(scheduleTask.getParameters()));
                jobRepository.findByIdNative(scheduleTask.getJobId())
                        .ifPresent(jobEntity -> res.setExecutionDetails(jobEntity.getResultJson()));
                break;
            case LOGICAL_DATABASE_CHANGE:
                res.setExecutionDetails(
                        JsonUtils.toJson(logicalDatabaseChangeService.listSqlExecutionUnits(scheduleTask.getId())));
                break;
            default:
                break;
        }
        return res;
    }

    public ScheduleTaskEntity create(ScheduleTaskEntity taskEntity) {
        return scheduleTaskRepository.save(taskEntity);
    }

    /**
     * Trigger an existing task to retry or resume a terminated task.
     */
    public void start(Long id) {
        ScheduleTask scheduleTask = nullSafeGetModelById(id);
        if (!scheduleTask.getStatus().isRetryAllowed()) {
            log.warn(
                    "The task cannot be restarted because it is currently in progress or has already completed,scheduleTaskId={}",
                    id);
            throw new IllegalStateException(
                    "The task cannot be restarted because it is currently in progress or has already completed.");
        }
        try {
            JobKey jobKey = new JobKey(scheduleTask.getJobName(), scheduleTask.getJobGroup());
            if (quartzJobService.checkExists(jobKey)) {
                quartzJobService.triggerJob(jobKey, ScheduleTaskUtils.buildTriggerDataMap(id));
            } else {
                CreateQuartzJobParam param = new CreateQuartzJobParam();
                param.setJobKey(jobKey);
                param.setAllowConcurrent(false);
                TriggerConfig triggerConfig = new TriggerConfig();
                triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
                param.setTriggerConfig(triggerConfig);
                quartzJobService.createJob(param, ScheduleTaskUtils.buildTriggerDataMap(id));
            }
        } catch (SchedulerException e) {
            log.warn("Trigger schedule task failed,scheduleTaskId={}", id);
            throw new InternalServerError(e.getMessage());
        }
    }

    public void stop(Long id) {
        ScheduleTask scheduleTask = nullSafeGetModelById(id);
        ExecutorInfo executorInfo = JsonUtils.fromJson(scheduleTask.getExecutor(), ExecutorInfo.class);
        if (taskFrameworkEnabledProperties.isEnabled() && scheduleTask.getJobId() != null) {
            try {
                jobScheduler.cancelJob(scheduleTask.getJobId());
                return;
            } catch (JobException e) {
                log.warn("Cancel job failed,jobId={}", scheduleTask.getJobId(), e);
                throw new UnexpectedException("Cancel job failed!", e);
            }
        }
        // Local interrupt task.
        if (dispatchChecker.isThisMachine(executorInfo)) {
            JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleTask.getJobName(), scheduleTask.getJobGroup());
            try {
                quartzJobService.interruptJob(jobKey);
                log.info("Local interrupt task succeed,taskId={}", id);
            } catch (Exception e) {
                log.warn("Interrupt job failed,error={}", e.getMessage());
                throw new UnexpectedException("Interrupt job failed,please try again.");
            }
        }
        // Remote interrupt task.
        try {
            DispatchResponse response =
                    requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            log.info("Remote interrupt task succeed,taskId={}", id);
        } catch (Exception e) {
            log.warn("Remote interrupt task failed, taskId={}", id, e);
            throw new UnexpectedException(String.format("Remote interrupt task failed, taskId=%s", id));
        }
    }



    public void updateStatusById(Long id, TaskStatus status) {
        scheduleTaskRepository.updateStatusById(id, status);
    }

    public void update(ScheduleTask scheduleTask) {
        ScheduleTaskEntity entity = scheduleTaskMapper.modelToEntity(scheduleTask);
        scheduleTaskRepository.update(entity);
    }


    public Page<ScheduleTask> list(Pageable pageable, Long scheduleId) {
        return listEntity(pageable, scheduleId).map(scheduleTaskMapper::entityToModel);
    }

    /**
     * for internal usage
     */
    public Page<ScheduleTaskEntity> listEntity(Pageable pageable, Long scheduleId) {
        Specification<ScheduleTaskEntity> specification =
                Specification.where(ScheduleTaskSpecs.jobNameEquals(scheduleId.toString()));
        return scheduleTaskRepository.findAll(specification, pageable);
    }

    public Page<ScheduleTaskOverview> getScheduleTaskListResp(Pageable pageable, Long scheduleId) {
        return list(pageable, scheduleId).map(ScheduleTaskOverviewMapper::map);
    }

    public Page<ScheduleTaskListOverview> getConditionalScheduleTaskListResp(Pageable pageable,
            QueryScheduleTaskParams params) {
        Map<String, Schedule> scheduleMap = params.getSchedules().stream()
                .collect(Collectors.toMap(schedule -> schedule.getId().toString(), Function.identity()));

        Specification<ScheduleTaskEntity> specification =
                Specification.where(ScheduleTaskSpecs.jobNameIn(scheduleMap.keySet()))
                        .and(ScheduleTaskSpecs.jobIdEquals(params.getScheduleId()))
                        .and(ScheduleTaskSpecs.statusIn(params.getStatuses()))
                        .and(ScheduleTaskSpecs.fireTimeLate(params.getStartTime()))
                        .and(ScheduleTaskSpecs.fireTimeBefore(params.getEndTime()));

        Page<ScheduleTask> scheduleTaskPage = scheduleTaskRepository.findAll(specification, pageable).map(
                scheduleTaskMapper::entityToModel);

        if (scheduleTaskPage.isEmpty()) {
            return Page.empty();
        }

        // get creator info
        Set<Long> creatorIds = params.getSchedules().stream().map(Schedule::getCreatorId).collect(Collectors.toSet());
        Map<Long, List<UserEntity>> users = userRepository.findByIdIn(creatorIds).stream().collect(
                Collectors.groupingBy(UserEntity::getId));

        // get database info
        Set<Long> databaseIds = params.getSchedules().stream().map(Schedule::getDatabaseId).collect(Collectors.toSet());
        Map<Long, Database> databaseMap = scheduleResponseMapperFactory.getDatabaseInfoByIds(databaseIds).stream()
                .collect(Collectors.toMap(Database::getId, Function.identity()));

        // get job result json
        List<Long> jobIds = scheduleTaskPage.getContent().stream().map(ScheduleTask::getJobId).filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, String> resultMap = jobRepository.findAllById(jobIds).stream()
                .filter(jobEntity -> jobEntity.getResultJson() != null)
                .collect(Collectors.toMap(JobEntity::getId, JobEntity::getResultJson));

        return scheduleTaskPage.map(task -> {
            Schedule schedule = scheduleMap.get(task.getJobName());
            ScheduleTaskListOverview overview = ScheduleTaskListOverviewMapper.map(task);
            overview.setScheduleName(schedule.getName());
            overview.setCreator(new InnerUser(users.get(schedule.getCreatorId()).get(0), null));
            if (schedule.getType() == ScheduleType.SQL_PLAN) {
                SqlPlanAttributes attribute = new SqlPlanAttributes();
                attribute.setDatabaseInfo(databaseMap.get(schedule.getDatabaseId()));
                attribute.setTaskResult(JsonUtils.fromJson(resultMap.get(task.getJobId()), SqlPlanTaskResult.class));
                Map<Long, String> id2Attributes = new HashMap<>();
                id2Attributes.put(task.getId(), JsonUtils.toJson(attribute));
                overview.setAttributes(JSON.parseObject(id2Attributes.get(task.getId())));
            }
            return overview;
        });
    }

    public List<ScheduleTask> listByJobNames(Set<String> jobNames) {
        return scheduleTaskRepository.findByJobNames(jobNames).stream()
                .map(scheduleTaskMapper::entityToModel)
                .collect(Collectors.toList());
    }

    public List<ScheduleTaskEntity> listTaskByJobNameAndStatus(String jobName, List<TaskStatus> statuses) {
        return scheduleTaskRepository.findByJobNameAndStatusIn(jobName, statuses);
    }

    public ScheduleTask nullSafeGetByJobId(Long jobId) {
        return findByJobId(jobId)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE_TASK, "jobId", jobId));
    }

    public Optional<ScheduleTask> findByJobId(Long jobId) {
        List<ScheduleTaskEntity> scheduleTasks = scheduleTaskRepository.findByJobId(jobId);
        if (scheduleTasks != null) {
            if (scheduleTasks.size() > 1) {
                throw new IllegalStateException("Query scheduleTask by jobId occur error, except 1 but found "
                        + scheduleTasks.size() + ",jobId=" + jobId);
            } else if (scheduleTasks.size() == 1) {
                return Optional.of(scheduleTaskMapper.entityToModel(scheduleTasks.get(0)));
            }
        }
        return Optional.empty();
    }

    public ScheduleTask nullSafeGetModelById(Long id) {
        return scheduleTaskMapper.entityToModel(nullSafeGetById(id));
    }

    public ScheduleTaskEntity nullSafeGetById(Long id) {
        Optional<ScheduleTaskEntity> scheduleEntityOptional = scheduleTaskRepository.findById(id);
        return scheduleEntityOptional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE_TASK, "id", id));
    }

    public ScheduleTask nullSafeGetByIdAndScheduleId(Long id, Long scheduleId) {
        Optional<ScheduleTaskEntity> scheduleEntityOptional =
                scheduleTaskRepository.findByIdAndJobName(id, scheduleId.toString());
        return scheduleTaskMapper.entityToModel(scheduleEntityOptional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE_TASK, "id", id)));
    }


    public void correctScheduleTaskStatus(Long scheduleId) {
        List<ScheduleTaskEntity> toBeCorrectedList = listTaskByJobNameAndStatus(
                scheduleId.toString(), TaskStatus.getProcessingStatus());
        // For the scenario where the task framework is switched from closed to open, it is necessary to
        // correct
        // the status of tasks that were not completed while in the closed state.
        if (taskFrameworkEnabledProperties.isEnabled()) {
            toBeCorrectedList =
                    toBeCorrectedList.stream().filter(o -> o.getJobId() == null).collect(Collectors.toList());
        }
        toBeCorrectedList.forEach(task -> {
            updateStatusById(task.getId(), TaskStatus.CANCELED);
            log.info("Task status correction successful,scheduleTaskId={}", task.getId());
        });
    }


    @SkipAuthorize("odc internal usage")
    public void triggerDataArchiveDelete(Long scheduleTaskId) {

        ScheduleTask dataArchiveTask = nullSafeGetModelById(scheduleTaskId);


        JobKey jobKey = QuartzKeyGenerator.generateJobKey(dataArchiveTask.getJobName(),
                ScheduleTaskType.DATA_ARCHIVE_DELETE.name());

        if (dataArchiveTask.getStatus() != TaskStatus.DONE) {
            log.warn("Delete is not allowed because the data archive job has not succeeded.JobKey={}", jobKey);
            throw new IllegalStateException("Delete is not allowed because the data archive job has not succeeded.");
        }

        try {
            if (quartzJobService.checkExists(jobKey)) {
                log.info("Data archive delete job exists and start delete job,jobKey={}", jobKey);
                quartzJobService.deleteJob(jobKey);
            }
            CreateQuartzJobParam req = new CreateQuartzJobParam();
            req.setJobKey(jobKey);
            DataArchiveClearParameters parameters = new DataArchiveClearParameters();
            parameters.setDataArchiveTaskId(scheduleTaskId);
            TriggerConfig triggerConfig = new TriggerConfig();
            triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
            req.getJobDataMap().putAll(BeanMap.create(parameters));
            req.setTriggerConfig(triggerConfig);
            quartzJobService.createJob(req);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


    public void rollbackTask(Long scheduleTaskId) {
        ScheduleTask scheduleTask = nullSafeGetModelById(scheduleTaskId);
        if (ScheduleTaskType.DATA_ARCHIVE_DELETE.name().equals(scheduleTask.getJobGroup())) {
            log.warn("Rollback is not allowed for taskType={}", scheduleTask.getJobGroup());
            throw new UnsupportedException("Rollback is not allowed.");
        }
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleTask.getJobName(),
                ScheduleTaskType.DATA_ARCHIVE_ROLLBACK.name());
        if (!scheduleTask.getStatus().isTerminated()) {
            log.warn("Rollback is not allowed because the data archive job is running.JobKey={}", jobKey);
            throw new IllegalStateException("Rollback is not allowed because the data archive job is running.");
        }


        try {
            if (quartzJobService.checkExists(jobKey)) {
                log.info("Data archive rollback job exists and start delete job,jobKey={}", jobKey);
                quartzJobService.deleteJob(jobKey);
            }
            CreateQuartzJobParam req = new CreateQuartzJobParam();
            req.setJobKey(jobKey);
            DataArchiveRollbackParameters parameters = new DataArchiveRollbackParameters();
            parameters.setDataArchiveTaskId(scheduleTaskId);
            req.getJobDataMap().putAll(BeanMap.create(parameters));
            TriggerConfig triggerConfig = new TriggerConfig();
            triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
            req.setTriggerConfig(triggerConfig);
            quartzJobService.createJob(req);
        } catch (SchedulerException e) {
            throw new UnsupportedException(e.getMessage());
        }
    }

    public List<ScheduleTask> findByIds(Set<Long> ids) {
        return scheduleTaskRepository.findByIdIn(ids).stream().map(scheduleTaskMapper::entityToModel)
                .collect(Collectors.toList());
    }

    public Optional<ScheduleTask> findById(Long id) {
        return scheduleTaskRepository.findById(id).map(scheduleTaskMapper::entityToModel);
    }


}
