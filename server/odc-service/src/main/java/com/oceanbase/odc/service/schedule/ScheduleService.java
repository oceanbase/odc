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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.compress.utils.Lists;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.schedule.factory.ScheduleResponseMapperFactory;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobReq;
import com.oceanbase.odc.service.schedule.model.DataArchiveClearParameters;
import com.oceanbase.odc.service.schedule.model.DataArchiveRollbackParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp.ScheduleResponseMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskResp;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/11/16 16:52
 * @Descripition:
 */

@Slf4j
@Service
@SkipAuthorize
public class ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private QuartzJobService quartzJobService;
    @Autowired
    private UserService userService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    @Autowired
    private ScheduleTaskService scheduleTaskService;

    @Autowired
    private ScheduleResponseMapperFactory scheduleResponseMapperFactory;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private TaskDispatchChecker dispatchChecker;
    @Autowired
    private RequestDispatcher requestDispatcher;
    private final ScheduleTaskMapper scheduleTaskMapper = ScheduleTaskMapper.INSTANCE;

    public ScheduleEntity create(ScheduleEntity scheduleConfig) {
        return scheduleRepository.save(scheduleConfig);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateJobData(ScheduleEntity scheduleConfig) throws SchedulerException {
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleConfig.getId(), scheduleConfig.getJobType());

        if (quartzJobService.checkExists(jobKey)) {
            quartzJobService.deleteJob(jobKey);
        }
        quartzJobService.createJob(buildCreateJobReq(scheduleConfig));
        scheduleConfig.setStatus(ScheduleStatus.ENABLED);
        scheduleRepository.save(scheduleConfig);
    }

    public void innerUpdateTriggerData(Long scheduleId, Map<String, Object> triggerDataMap)
            throws SchedulerException {
        ScheduleEntity scheduleConfig = nullSafeGetById(scheduleId);
        Trigger scheduleTrigger = nullSafeGetScheduleTrigger(scheduleConfig);
        scheduleTrigger.getJobDataMap().putAll(triggerDataMap);
        quartzJobService.rescheduleJob(scheduleTrigger.getKey(), scheduleTrigger);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(ScheduleEntity scheduleConfig) throws SchedulerException, ClassNotFoundException {
        quartzJobService.createJob(buildCreateJobReq(scheduleConfig));
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.ENABLED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void innerEnable(Long scheduleId, Map<String, Object> triggerDataMap)
            throws SchedulerException {
        ScheduleEntity scheduleConfig = nullSafeGetById(scheduleId);
        quartzJobService.createJob(buildCreateJobReq(scheduleConfig), new JobDataMap(triggerDataMap));
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.ENABLED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void pause(ScheduleEntity scheduleConfig) throws SchedulerException {
        Trigger scheduleTrigger = nullSafeGetScheduleTrigger(scheduleConfig);
        quartzJobService.pauseJob(scheduleTrigger.getJobKey());
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.PAUSE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resume(ScheduleEntity scheduleConfig) throws SchedulerException {
        Trigger scheduleTrigger = getScheduleTrigger(scheduleConfig);
        if (scheduleTrigger != null) {
            quartzJobService.resumeJob(scheduleTrigger.getJobKey());
        } else {
            quartzJobService.createJob(buildCreateJobReq(scheduleConfig));
        }
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.ENABLED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminate(ScheduleEntity scheduleConfig) throws SchedulerException {
        Trigger trigger = getScheduleTrigger(scheduleConfig);
        quartzJobService.deleteJob(trigger.getJobKey());
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.TERMINATION);
    }

    public ScheduleDetailResp triggerJob(Long scheduleId, String jobType) {
        ScheduleEntity entity = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        if (StringUtils.isEmpty(jobType)) {
            jobType = entity.getJobType().name();
        }
        try {
            quartzJobService.triggerJob(new JobKey(scheduleId.toString(), jobType));
        } catch (Exception e) {
            log.warn("Trigger job failed,error={}", e.getMessage());
            throw new RuntimeException("Trigger job failed.");
        }
        return ScheduleDetailResp.withId(scheduleId);
    }

    public ScheduleDetailResp interruptJob(Long scheduleId, Long taskId) {
        ScheduleEntity entity = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(taskId);
        ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
        // Local interrupt task.
        if (dispatchChecker.isThisMachine(executorInfo)) {
            JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.valueOf(taskEntity.getJobGroup()));
            try {
                quartzJobService.interruptJob(jobKey);
                log.info("Local interrupt task succeed,taskId={}", taskId);
            } catch (Exception e) {
                log.warn("Interrupt job failed,error={}", e.getMessage());
                throw new UnexpectedException("Interrupt job failed,please try again.");
            }
            return ScheduleDetailResp.withId(scheduleId);
        }
        // Remote interrupt task.
        try {
            DispatchResponse response =
                    requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            log.info("Remote interrupt task succeed,taskId={}", taskId);
            return response.getContentByType(
                    new TypeReference<SuccessResponse<ScheduleDetailResp>>() {}).getData();
        } catch (Exception e) {
            log.warn("Remote interrupt task failed, taskId={}", taskId, e);
            throw new UnexpectedException(String.format("Remote interrupt task failed, taskId=%s", taskId));
        }
    }

    /**
     * @param scheduleId the task must belong to a valid schedule,so this param is not be null.
     * @param taskId the task uid. Start a paused or pending task.
     */
    public ScheduleTaskResp startTask(Long scheduleId, Long taskId) {
        ScheduleEntity scheduleEntity = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(taskId);
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.valueOf(taskEntity.getJobGroup()));
        if (!taskEntity.getStatus().isRetryAllowed()) {
            log.warn(
                    "The task cannot be restarted because it is currently in progress or has already completed.JobKey={}",
                    jobKey);
            throw new IllegalStateException(
                    "The task cannot be restarted because it is currently in progress or has already completed.");
        }
        try {
            // create a single trigger job if job not found.
            if (!quartzJobService.checkExists(jobKey)) {
                log.info("Job not found and will be recreated,jobKey={},taskId={}", jobKey, taskId);
                CreateQuartzJobReq req = new CreateQuartzJobReq();
                req.setScheduleId(scheduleId);
                req.setType(JobType.valueOf(taskEntity.getJobGroup()));
                TriggerConfig triggerConfig = new TriggerConfig();
                triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
                req.setTriggerConfig(triggerConfig);
                quartzJobService.createJob(req, ScheduleTaskUtils.buildTriggerDataMap(taskId));
                log.info("Job recreated,jobKey={}", jobKey);
                return ScheduleTaskResp.withId(taskId);
            } else {
                return scheduleTaskService.start(taskId);
            }
        } catch (SchedulerException e) {
            log.warn("Unexpected exception while check job!", e);
            throw new IllegalStateException("Unexpected exception while check job!");
        }
    }

    public ScheduleTaskResp dataArchiveDelete(Long scheduleId, Long taskId) {
        ScheduleEntity scheduleEntity = nullSafeGetById(scheduleId);
        if (scheduleEntity.getJobType() != JobType.DATA_ARCHIVE) {
            throw new UnsupportedException();
        }

        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.DATA_ARCHIVE_DELETE);

        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(taskId);
        if (taskEntity.getStatus() != TaskStatus.DONE) {
            log.warn("Delete is not allowed because the data archive job has not succeeded.JobKey={}", jobKey);
            throw new IllegalStateException("Delete is not allowed because the data archive job has not succeeded.");
        }

        try {
            if (quartzJobService.checkExists(jobKey)) {
                log.info("Data archive delete job exists and start delete job,jobKey={}", jobKey);
                quartzJobService.deleteJob(jobKey);
            }
            CreateQuartzJobReq req = new CreateQuartzJobReq();
            req.setScheduleId(scheduleId);
            req.setType(JobType.DATA_ARCHIVE_DELETE);
            DataArchiveClearParameters parameters = new DataArchiveClearParameters();
            parameters.setDataArchiveTaskId(taskId);
            TriggerConfig triggerConfig = new TriggerConfig();
            triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
            req.getJobDataMap().putAll(BeanMap.create(parameters));
            req.setTriggerConfig(triggerConfig);
            quartzJobService.createJob(req);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
        ScheduleTaskResp scheduleTaskResp = ScheduleTaskResp.withId(taskId);
        scheduleTaskResp.setJobName(jobKey.getName());
        scheduleTaskResp.setJobGroup(jobKey.getGroup());
        return scheduleTaskResp;
    }

    public ScheduleTaskResp rollbackTask(Long scheduleId, Long taskId) {
        ScheduleEntity scheduleEntity = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        if (scheduleEntity.getJobType() != JobType.DATA_ARCHIVE) {
            throw new UnsupportedException();
        }

        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.DATA_ARCHIVE_ROLLBACK);
        ScheduleTaskEntity taskEntity = scheduleTaskService.nullSafeGetById(taskId);
        if (!taskEntity.getStatus().isTerminated()) {
            log.warn("Rollback is not allowed because the data archive job is running.JobKey={}", jobKey);
            throw new IllegalStateException("Rollback is not allowed because the data archive job is running.");
        }

        try {
            if (quartzJobService.checkExists(jobKey)) {
                log.info("Data archive rollback job exists and start delete job,jobKey={}", jobKey);
                quartzJobService.deleteJob(jobKey);
            }
            CreateQuartzJobReq req = new CreateQuartzJobReq();
            req.setScheduleId(scheduleId);
            req.setType(JobType.DATA_ARCHIVE_ROLLBACK);
            DataArchiveRollbackParameters parameters = new DataArchiveRollbackParameters();
            parameters.setDataArchiveTaskId(taskId);
            req.getJobDataMap().putAll(BeanMap.create(parameters));
            TriggerConfig triggerConfig = new TriggerConfig();
            triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
            req.setTriggerConfig(triggerConfig);
            quartzJobService.createJob(req);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
        return ScheduleTaskResp.withId(taskId);
    }

    public Page<ScheduleTaskResp> listTask(Pageable pageable, Long scheduleId) {
        ScheduleEntity entity = nullSafeGetByIdWithCheckPermission(scheduleId);
        Page<ScheduleTaskEntity> scheduleTaskEntities = scheduleTaskService.listTask(pageable, entity.getId());
        return scheduleTaskEntities.map(scheduleTaskMapper::entityToModel);
    }

    public void updateStatusById(Long id, ScheduleStatus status) {
        scheduleRepository.updateStatusById(id, status);
    }

    public void updateStatusByFlowInstanceId(Long id, ScheduleStatus status) {
        Long scheduleId = flowInstanceRepository.findScheduleIdByFlowInstanceId(id);
        if (scheduleId != null) {
            ScheduleEntity schedule = nullSafeGetById(scheduleId);
            if (schedule.getStatus() == ScheduleStatus.APPROVING) {
                updateStatusById(scheduleId, status);
            }
        }
    }

    public void updateJobParametersById(Long id, String jobParameters) {
        scheduleRepository.updateJobParametersById(id, jobParameters);
    }

    public ScheduleDetailResp getById(Long id) {
        ScheduleEntity entity = nullSafeGetByIdWithCheckPermission(id);
        ScheduleResponseMapper mapper = scheduleResponseMapperFactory.generate(entity);
        return mapper.map(entity);
    }

    public Page<ScheduleDetailResp> list(@NotNull Pageable pageable, @NotNull QueryScheduleParams params) {
        if (StringUtils.isNotBlank(params.getCreator())) {
            params.setCreatorIds(userService.getUsersByFuzzyNameWithoutPermissionCheck(
                    params.getCreator()).stream().map(User::getId).collect(Collectors.toSet()));
        }
        if (authenticationFacade.currentOrganization().getType() == OrganizationType.TEAM) {
            Set<Long> projectIds = params.getProjectId() == null
                    ? projectService.getMemberProjectIds(authenticationFacade.currentUserId())
                    : Collections.singleton(params.getProjectId());
            if (projectIds.isEmpty()) {
                return Page.empty();
            }
            params.setProjectIds(projectIds);
        }
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        Page<ScheduleEntity> returnValue = scheduleRepository.find(pageable, params);
        return returnValue.isEmpty() ? Page.empty()
                : returnValue.map(scheduleResponseMapperFactory.generate(returnValue.getContent())::map);
    }

    public List<String> getAsyncDownloadUrl(Long id, List<String> objectIds) {
        ScheduleEntity scheduleEntity = nullSafeGetByIdWithCheckPermission(id);
        List<String> downloadUrls = Lists.newArrayList();
        for (String objectId : objectIds) {
            downloadUrls.add(objectStorageFacade.getDownloadUrl(
                    "async".concat(File.separator).concat(scheduleEntity.getCreatorId().toString()),
                    objectId));
        }
        return downloadUrls;
    }

    public String getLog(Long scheduleId, Long taskId, OdcTaskLogLevel logLevel) {
        nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleTaskService.getScheduleTaskLog(taskId, logLevel);
    }

    public boolean hasExecutingAsyncTask(ScheduleEntity schedule) {
        Set<Long> executingTaskIds = serviceTaskRepository.findByScheduleIdAndTaskType(
                schedule.getId(), TaskType.ASYNC).stream()
                .filter(entity -> !FlowNodeStatus.isFinalStatus(entity.getStatus()))
                .map(ServiceTaskInstanceEntity::getTargetTaskId).collect(
                        Collectors.toSet());
        List<TaskEntity> taskEntities = taskRepository.findByIdIn(executingTaskIds);
        for (TaskEntity taskEntity : taskEntities) {
            Long timeoutMillis = JsonUtils.fromJson(taskEntity.getParametersJson(), DatabaseChangeParameters.class)
                    .getTimeoutMillis();
            if (taskEntity.getCreateTime().getTime() + timeoutMillis > System.currentTimeMillis()) {
                return true;
            }
        }
        return false;
    }

    public ScheduleEntity nullSafeGetByIdWithCheckPermission(Long id) {
        return nullSafeGetByIdWithCheckPermission(id, false);
    }

    public ScheduleEntity nullSafeGetByIdWithCheckPermission(Long id, boolean isWrite) {
        ScheduleEntity scheduleEntity = nullSafeGetById(id);
        if (isWrite) {
            List<ResourceRoleName> resourceRoleNames = getApproverRoleNames(scheduleEntity);
            if (!projectService.checkPermission(scheduleEntity.getProjectId(), resourceRoleNames)
                    && authenticationFacade.currentUserId() != scheduleEntity.getCreatorId()) {
                throw new AccessDeniedException();
            }
        } else {
            if (!projectService.checkPermission(scheduleEntity.getProjectId(), ResourceRoleName.all())) {
                throw new AccessDeniedException();
            }
        }
        return scheduleEntity;
    }

    public ScheduleEntity nullSafeGetById(Long id) {
        Optional<ScheduleEntity> scheduleEntityOptional = scheduleRepository.findById(id);
        return scheduleEntityOptional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE, "id", id));
    }


    private Trigger nullSafeGetScheduleTrigger(ScheduleEntity schedule) throws SchedulerException {
        Trigger trigger = getScheduleTrigger(schedule);
        if (trigger == null) {
            throw new NotFoundException(ResourceType.ODC_SCHEDULE_TRIGGER, "scheduleId", schedule.getId());
        }
        return trigger;
    }

    private Trigger getScheduleTrigger(ScheduleEntity schedule) throws SchedulerException {
        return quartzJobService.getTrigger(
                QuartzKeyGenerator.generateTriggerKey(schedule.getId(), schedule.getJobType()));
    }

    private CreateQuartzJobReq buildCreateJobReq(ScheduleEntity schedule) {
        CreateQuartzJobReq createQuartzJobReq = new CreateQuartzJobReq();
        createQuartzJobReq.setScheduleId(schedule.getId());
        createQuartzJobReq.setType(schedule.getJobType());
        createQuartzJobReq.setTriggerConfig(JsonUtils.fromJson(schedule.getTriggerConfigJson(), TriggerConfig.class));
        if (schedule.getJobType() == JobType.ONLINE_SCHEMA_CHANGE_COMPLETE) {
            createQuartzJobReq.getJobDataMap().putAll(JsonUtils.fromJson(schedule.getJobParametersJson(), Map.class));
        } else {
            createQuartzJobReq.getJobDataMap().putAll(BeanMap.create(schedule));
        }
        if (schedule.getAllowConcurrent() != null) {
            createQuartzJobReq.setAllowConcurrent(schedule.getAllowConcurrent());
        }
        if (schedule.getMisfireStrategy() != null) {
            createQuartzJobReq.setMisfireStrategy(schedule.getMisfireStrategy());
        }
        return createQuartzJobReq;
    }

    private List<ResourceRoleName> getApproverRoleNames(ScheduleEntity entity) {
        Database database = databaseService.detail(entity.getDatabaseId());
        RiskLevelDescriber riskLevelDescriber = new RiskLevelDescriber();
        riskLevelDescriber.setDatabaseName(database.getName());
        riskLevelDescriber.setProjectName(database.getProject().getName());
        riskLevelDescriber.setEnvironmentId(database.getEnvironment().getId().toString());
        riskLevelDescriber.setTaskType(TaskType.ALTER_SCHEDULE.name());
        RiskLevel riskLevel = approvalFlowConfigSelector.select(riskLevelDescriber);
        return riskLevel.getApprovalFlowConfig().getNodes().stream().filter(node -> node.getResourceRoleName() != null)
                .map(
                        node -> ResourceRoleName.valueOf(node.getResourceRoleName()))
                .collect(Collectors.toList());
    }

}
