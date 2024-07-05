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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.compress.utils.Lists;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.schedule.factory.ScheduleResponseMapperFactory;
import com.oceanbase.odc.service.schedule.model.ChangeQuartJobParam;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.processor.ScheduleChangePreprocessor;
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
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    @Autowired
    private ScheduleTaskService scheduleTaskService;

    @Autowired
    private ScheduleResponseMapperFactory scheduleResponseMapperFactory;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private ScheduleChangeLogService scheduleChangeLogService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ScheduleChangePreprocessor preprocessor;

    private final ScheduleMapper scheduleMapper = ScheduleMapper.INSTANCE;


    @Transactional(rollbackFor = Exception.class)
    public Schedule changeSchedule(ScheduleChangeParams req) {

        preprocessor.process(req);
        Schedule targetSchedule;

        // create or load target schedule
        if (req.getOperationType() == OperationType.CREATE) {
            ScheduleEntity entity = new ScheduleEntity();

            entity.setProjectId(req.getCreateScheduleReq().getProjectId());
            entity.setDescription(req.getCreateScheduleReq().getDescription());
            entity.setJobParametersJson(JsonUtils.toJson(req.getCreateScheduleReq().getParameters()));
            entity.setTriggerConfigJson(JsonUtils.toJson(req.getCreateScheduleReq().getTriggerConfig()));
            entity.setType(req.getCreateScheduleReq().getType());

            entity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
            entity.setStatus(ScheduleStatus.CREATING);
            entity.setAllowConcurrent(false);
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setCreatorId(authenticationFacade.currentUserId());
            entity.setModifierId(authenticationFacade.currentUserId());
            entity.setDatabaseId(req.getCreateScheduleReq().getDatabaseId());
            entity.setDatabaseName(req.getCreateScheduleReq().getDatabaseName());
            entity.setDataSourceId(req.getCreateScheduleReq().getConnectionId());

            targetSchedule = scheduleMapper.entityToModel(scheduleRepository.save(entity));
            req.setScheduleId(targetSchedule.getId());
        } else {
            targetSchedule = nullSafeGetByIdWithCheckPermission(req.getScheduleId(), true);
            if (req.getOperationType() == OperationType.UPDATE
                    && (targetSchedule.getStatus() != ScheduleStatus.PAUSE || hasRunningTask(targetSchedule.getId()))) {
                log.warn("Update schedule is not allowed,status={}", targetSchedule.getStatus());
                throw new IllegalStateException("Update schedule is not allowed.");
            }
        }

        Long approvalFlowInstanceId = createApprovalFlow();

        if (approvalFlowInstanceId == null) {
            executeChangeSchedule(req);
        }

        // create change log for this request
        ScheduleChangeLog changeLog = scheduleChangeLogService.createChangeLog(
                ScheduleChangeLog.build(targetSchedule.getId(), req.getOperationType(),
                        JsonUtils.toJson(targetSchedule.getParameters()),
                        req.getOperationType() == OperationType.UPDATE
                                ? JsonUtils.toJson(req.getUpdateScheduleReq().getParameters())
                                : null,
                        approvalFlowInstanceId, approvalFlowInstanceId == null ? ScheduleChangeStatus.SUCCESS
                                : ScheduleChangeStatus.APPROVING));
        log.info("Create change log success,changLog={}", changeLog);
        return targetSchedule;
    }

    public void executeChangeSchedule(ScheduleChangeParams req) {
        Schedule targetSchedule = nullSafeGetModelById(req.getScheduleId());
        // start to change schedule
        switch (req.getOperationType()) {
            case CREATE:
            case RESUME: {
                scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.ENABLED);
                break;
            }
            case UPDATE: {
                scheduleRepository.updateJobParametersById(req.getScheduleId(),
                        JsonUtils.toJson(req.getUpdateScheduleReq().getParameters()));
                scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.ENABLED);
                break;
            }
            case PAUSE: {
                scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.PAUSE);
                break;
            }
            case TERMINATE: {
                scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.TERMINATED);
                break;
            }
            default:
                throw new UnsupportedException();
        }

        // start change quartzJob
        ChangeQuartJobParam quartzJobReq = new ChangeQuartJobParam();
        quartzJobReq.setOperationType(req.getOperationType());
        quartzJobReq.setJobName(targetSchedule.getId().toString());
        quartzJobReq.setJobGroup(targetSchedule.getType().name());
        quartzJobReq.setTriggerConfig(targetSchedule.getTriggerConfig());
        quartzJobService.changeQuartzJob(quartzJobReq);

    }

    // return null if approval is not necessary
    private Long createApprovalFlow() {
        return null;
    }

    public ScheduleEntity create(ScheduleEntity scheduleConfig) {
        return scheduleRepository.save(scheduleConfig);
    }

    public void innerUpdateTriggerData(Long scheduleId, Map<String, Object> triggerDataMap)
            throws SchedulerException {
        ScheduleEntity scheduleConfig = nullSafeGetById(scheduleId);
        Trigger scheduleTrigger = nullSafeGetScheduleTrigger(scheduleConfig);
        scheduleTrigger.getJobDataMap().putAll(triggerDataMap);
        quartzJobService.rescheduleJob(scheduleTrigger.getKey(), scheduleTrigger);
    }

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void enable(ScheduleEntity scheduleConfig) throws SchedulerException, ClassNotFoundException {
        quartzJobService.createJob(buildCreateJobReq(scheduleConfig));
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.ENABLED);
    }

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void innerEnable(Long scheduleId, Map<String, Object> triggerDataMap)
            throws SchedulerException {
        ScheduleEntity scheduleConfig = nullSafeGetById(scheduleId);
        quartzJobService.createJob(buildCreateJobReq(scheduleConfig), new JobDataMap(triggerDataMap));
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.ENABLED);
    }

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void pause(ScheduleEntity scheduleConfig) throws SchedulerException {
        Trigger scheduleTrigger = nullSafeGetScheduleTrigger(scheduleConfig);
        quartzJobService.pauseJob(scheduleTrigger.getJobKey());
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.PAUSE);
    }

    @Deprecated
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

    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void terminate(ScheduleEntity scheduleConfig) throws SchedulerException {
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleConfig);
        quartzJobService.deleteJob(jobKey);
        scheduleRepository.updateStatusById(scheduleConfig.getId(), ScheduleStatus.TERMINATED);
    }

    /**
     * The method detects whether the database required for scheduled task operation exists. It returns
     * true and terminates the scheduled task if the database does not exist. If the database exists, it
     * returns false.
     */
    public boolean terminateIfScheduleInvalid(Long scheduleId) {
        Optional<ScheduleEntity> scheduleEntityOptional = scheduleRepository.findById(scheduleId);
        if (scheduleEntityOptional.isPresent()) {
            ScheduleEntity entity = scheduleEntityOptional.get();
            boolean isInvalid = isInvalidSchedule(entity);
            if (isInvalid) {
                try {
                    log.info(
                            "The project or database for scheduled task operation does not exist, and the schedule is being terminated, scheduleId={}",
                            scheduleId);
                    terminate(entity);
                } catch (Exception e) {
                    log.warn("Terminate schedule failed,scheduleId={}", scheduleId);
                }
            }
            return isInvalid;
        }
        // terminate if schedule not found or invalid.
        return true;

    }

    private boolean isInvalidSchedule(ScheduleEntity schedule) {
        Optional<Organization> organization = organizationService.get(schedule.getOrganizationId());
        // ignore individual space
        if (organization.isPresent() && organization.get().getType() == OrganizationType.INDIVIDUAL) {
            return false;
        }

        try {
            // project archived.
            if (projectService.nullSafeGet(schedule.getProjectId()).getArchived()) {
                return true;
            }
        } catch (NotFoundException e) {
            // project not found.
            return true;
        }
        // schedule is valid.
        return false;
    }

    public void stopTask(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, true);
        scheduleTaskService.stop(scheduleTaskId);
    }

    /**
     * @param scheduleId the task must belong to a valid schedule,so this param is not be null.
     * @param scheduleTaskId the task uid. Start a paused or pending task.
     */
    public void startTask(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, true);
        scheduleTaskService.start(scheduleTaskId);
    }

    public void rollbackTask(Long scheduleId, Long scheduleTaskId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        if (schedule.getType() != ScheduleType.DATA_ARCHIVE) {
            throw new UnsupportedException();
        }
        scheduleTaskService.rollbackTask(scheduleTaskId);
    }

    public void updateStatusById(Long id, ScheduleStatus status) {
        scheduleRepository.updateStatusById(id, status);
    }

    public void refreshScheduleStatus(Long scheduleId) {
        ScheduleEntity scheduleEntity = nullSafeGetById(scheduleId);
        JobKey key = QuartzKeyGenerator.generateJobKey(scheduleEntity);
        ScheduleStatus status = scheduleEntity.getStatus();
        if (status == ScheduleStatus.PAUSE) {
            return;
        }
        int runningTask = scheduleTaskService.listTaskByJobNameAndStatus(scheduleId.toString(),
                TaskStatus.getProcessingStatus()).size();
        if (runningTask > 0) {
            status = ScheduleStatus.ENABLED;
        } else {
            try {
                List<? extends Trigger> jobTriggers = quartzJobService.getJobTriggers(key);
                if (jobTriggers.isEmpty()) {
                    status = ScheduleStatus.COMPLETED;
                }
                for (Trigger trigger : jobTriggers) {
                    if (trigger.mayFireAgain()) {
                        status = ScheduleStatus.ENABLED;
                        break;
                    } else {
                        status = ScheduleStatus.COMPLETED;
                    }
                }
            } catch (SchedulerException e) {
                log.warn("Get job triggers failed and don't update schedule status.scheduleId={}", scheduleId);
                return;
            }
        }
        scheduleRepository.updateStatusById(scheduleId, status);
        log.info("Update schedule status from {} to {} success,scheduleId={}}", scheduleEntity.getStatus(), status,
                scheduleId);
    }

    public void updateStatusByFlowInstanceId(Long id, ScheduleStatus status) {
        Long scheduleId = flowInstanceRepository.findScheduleIdByFlowInstanceId(id);
        if (scheduleId != null) {
            Schedule schedule = nullSafeGetModelById(scheduleId);
            if (schedule.getStatus() == ScheduleStatus.APPROVING) {
                updateStatusById(scheduleId, status);
            }
        }
    }

    public void updateJobParametersById(Long id, String jobParameters) {
        scheduleRepository.updateJobParametersById(id, jobParameters);
    }


    public ScheduleTaskDetailResp detailScheduleTask(Long scheduleId, Long scheduleTaskId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleTaskService.getScheduleTaskDetailResp(scheduleTaskId, schedule.getId());
    }

    public ScheduleDetailResp detailSchedule(Long scheduleId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleResponseMapperFactory.generateScheduleDetailResp(schedule);
    }

    public Page<ScheduleOverview> listScheduleOverview(@NotNull Pageable pageable,
            @NotNull QueryScheduleParams params) {

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
        List<ScheduleOverview> res =
                scheduleResponseMapperFactory.generateScheduleOverviewList(returnValue.getContent());

        return returnValue.isEmpty() ? Page.empty()
                : new PageImpl<>(res, returnValue.getPageable(), returnValue.getTotalPages());
    }

    public Page<ScheduleTaskOverview> listScheduleTaskOverview(@NotNull Pageable pageable, @NotNull Long scheduleId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, false);
        return scheduleTaskService.getScheduleTaskListResp(pageable, scheduleId);
    }

    public List<String> getAsyncDownloadUrl(Long id, List<String> objectIds) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(id);
        List<String> downloadUrls = Lists.newArrayList();
        for (String objectId : objectIds) {
            downloadUrls.add(objectStorageFacade.getDownloadUrl(
                    "async".concat(File.separator).concat(schedule.getCreatorId().toString()),
                    objectId));
        }
        return downloadUrls;
    }

    public String getLog(Long scheduleId, Long taskId, OdcTaskLogLevel logLevel) {
        nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleTaskService.getLogWithoutPermission(taskId, logLevel);
    }

    public Schedule nullSafeGetByIdWithCheckPermission(Long id) {
        return nullSafeGetByIdWithCheckPermission(id, false);
    }

    public Schedule nullSafeGetByIdWithCheckPermission(Long id, boolean isWrite) {
        Schedule schedule = nullSafeGetModelById(id);
        Long projectId = schedule.getProjectId();
        if ((Objects.nonNull(projectId)
                && !projectPermissionValidator.hasProjectRole(projectId, ResourceRoleName.all()))) {
            throw new AccessDeniedException();
        }
        return schedule;
    }

    public Schedule nullSafeGetModelById(Long id) {
        return scheduleMapper.entityToModel(nullSafeGetById(id));
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

    private CreateQuartzJobParam buildCreateJobReq(ScheduleEntity schedule) {
        CreateQuartzJobParam createQuartzJobReq = new CreateQuartzJobParam();
        createQuartzJobReq.setJobKey(QuartzKeyGenerator.generateJobKey(schedule));
        createQuartzJobReq.setTriggerConfig(JsonUtils.fromJson(schedule.getTriggerConfigJson(), TriggerConfig.class));
        if (schedule.getType() == ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE) {
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

    private Trigger getScheduleTrigger(ScheduleEntity schedule) throws SchedulerException {
        return quartzJobService.getTrigger(QuartzKeyGenerator.generateTriggerKey(schedule));
    }

    public ScheduleChangeLog getChangeLog(Long id, Long scheduleChangeLogId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(id, false);
        return scheduleChangeLogService.getByIdAndScheduleId(scheduleChangeLogId, schedule.getId());
    }

    public List<ScheduleChangeLog> listScheduleChangeLog(Long id) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(id, false);
        return scheduleChangeLogService.listByScheduleId(schedule.getId());
    }

    public boolean hasRunningTask(Long id) {
        return false;
    }

}
