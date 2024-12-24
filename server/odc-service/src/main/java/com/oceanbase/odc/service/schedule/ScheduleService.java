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

import static com.oceanbase.odc.core.alarm.AlarmEventNames.SCHEDULING_FAILED;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.SCHEDULING_IGNORE;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.compress.utils.Lists;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONObject;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingEntity;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.QueryConnectionParams;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.schedule.factory.ScheduleResponseMapperFactory;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.flowtask.ApprovalFlowClient;
import com.oceanbase.odc.service.schedule.model.ChangeQuartJobParam;
import com.oceanbase.odc.service.schedule.model.ChangeScheduleResp;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;
import com.oceanbase.odc.service.schedule.model.CreateScheduleReq;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.QueryScheduleTaskParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailRespHist;
import com.oceanbase.odc.service.schedule.model.ScheduleMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleOverviewHist;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailRespHist;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskListOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.schedule.model.UpdateScheduleReq;
import com.oceanbase.odc.service.schedule.processor.ScheduleChangePreprocessor;
import com.oceanbase.odc.service.schedule.util.ScheduleDescriptionGenerator;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.NonNull;
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
    private ScheduleTaskRepository scheduleTaskRepository;
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
    @Lazy
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

    @Autowired
    private LatestTaskMappingRepository latestTaskMappingRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DlmLimiterService dlmLimiterService;

    @Autowired
    private UserService userService;

    @Autowired
    private ScheduledTaskLoggerService scheduledTaskLoggerService;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Autowired
    private ApprovalFlowClient approvalFlowService;

    @Autowired
    private ScheduleDescriptionGenerator descriptionGenerator;

    private final ScheduleMapper scheduleMapper = ScheduleMapper.INSTANCE;

    @Transactional(rollbackFor = Exception.class)
    public List<FlowInstanceDetailResp> dispatchCreateSchedule(CreateFlowInstanceReq createReq) {
        AlterScheduleParameters parameters = (AlterScheduleParameters) createReq.getParameters();
        // adapt history parameters
        if ((parameters.getOperationType() == OperationType.CREATE
                || parameters.getOperationType() == OperationType.UPDATE)
                && parameters.getType() == ScheduleType.SQL_PLAN) {
            SqlPlanParameters sqlPlanParameters = (SqlPlanParameters) parameters.getScheduleTaskParameters();
            sqlPlanParameters.setDatabaseId(createReq.getDatabaseId());
        }
        ScheduleChangeParams scheduleChangeParams;
        switch (parameters.getOperationType()) {
            case CREATE: {
                CreateScheduleReq createScheduleReq = new CreateScheduleReq();
                createScheduleReq.setParameters(parameters.getScheduleTaskParameters());
                createScheduleReq.setTriggerConfig(parameters.getTriggerConfig());
                createScheduleReq.setType(parameters.getType());
                createScheduleReq.setDescription(createReq.getDescription());
                scheduleChangeParams = ScheduleChangeParams.with(createScheduleReq);
                break;
            }
            case UPDATE: {
                UpdateScheduleReq updateScheduleReq = new UpdateScheduleReq();
                updateScheduleReq.setParameters(parameters.getScheduleTaskParameters());
                updateScheduleReq.setTriggerConfig(parameters.getTriggerConfig());
                updateScheduleReq.setType(parameters.getType());
                updateScheduleReq.setDescription(createReq.getDescription());
                scheduleChangeParams = ScheduleChangeParams.with(parameters.getTaskId(), updateScheduleReq);
                break;
            }
            default: {
                scheduleChangeParams =
                        ScheduleChangeParams.with(parameters.getTaskId(), parameters.getOperationType());
            }
        }
        changeSchedule(scheduleChangeParams);
        return Collections.singletonList(FlowInstanceDetailResp.withIdAndType(-1L, TaskType.ALTER_SCHEDULE));
    }



    @Transactional(rollbackFor = Exception.class)
    public ChangeScheduleResp changeSchedule(ScheduleChangeParams req) {

        preprocessor.process(req);
        Schedule targetSchedule;

        // create or load target schedule
        if (req.getOperationType() == OperationType.CREATE) {
            PreConditions.notNull(req.getCreateScheduleReq(), "req.createScheduleReq");
            validateTriggerConfig(req.getCreateScheduleReq().getTriggerConfig());
            ScheduleEntity entity = new ScheduleEntity();

            entity.setName(req.getCreateScheduleReq().getName());
            entity.setProjectId(req.getProjectId());
            if (StringUtils.isEmpty(req.getCreateScheduleReq().getDescription())) {
                descriptionGenerator.generateScheduleDescription(req);
            }
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
            entity.setDatabaseId(req.getDatabaseId());
            entity.setDatabaseName(req.getDatabaseName());
            entity.setDataSourceId(req.getConnectionId());

            targetSchedule = scheduleMapper.entityToModel(scheduleRepository.save(entity));
            req.setScheduleId(targetSchedule.getId());
            if (req.getCreateScheduleReq().getType() == ScheduleType.DATA_ARCHIVE
                    || req.getCreateScheduleReq().getType() == ScheduleType.DATA_DELETE) {
                if (req.getCreateScheduleReq().getParameters() instanceof DataArchiveParameters) {
                    DataArchiveParameters parameters = (DataArchiveParameters) req.getCreateScheduleReq()
                            .getParameters();
                    parameters.getRateLimit().setOrderId(req.getScheduleId());
                    dlmLimiterService.create(parameters.getRateLimit());
                }
                if (req.getCreateScheduleReq().getParameters() instanceof DataDeleteParameters) {
                    DataDeleteParameters parameters = (DataDeleteParameters) req.getCreateScheduleReq()
                            .getParameters();
                    parameters.getRateLimit().setOrderId(req.getScheduleId());
                    dlmLimiterService.create(parameters.getRateLimit());
                }
            }
        } else {
            targetSchedule = nullSafeGetByIdWithCheckPermission(req.getScheduleId(), true);
            if (req.getOperationType() == OperationType.UPDATE) {
                validateTriggerConfig(req.getUpdateScheduleReq().getTriggerConfig());
                PreConditions.validRequestState(targetSchedule.getStatus() == ScheduleStatus.PAUSE,
                        ErrorCodes.UpdateNotAllowed, null, "Update schedule is not allowed.");
            }
            if (req.getOperationType() == OperationType.PAUSE) {
                PreConditions.validRequestState(!hasExecutingTask(targetSchedule.getId()), ErrorCodes.PauseNotAllowed,
                        null, "Pause schedule is not allowed.");
            }
            if (req.getOperationType() == OperationType.DELETE) {
                PreConditions.validRequestState(targetSchedule.getStatus() == ScheduleStatus.TERMINATED
                        || targetSchedule.getStatus() == ScheduleStatus.COMPLETED, ErrorCodes.DeleteNotAllowed, null,
                        "Delete schedule is not allowed.");
            }
        }

        ScheduleChangeLog scheduleChangelog;
        try {
            scheduleChangelog = createScheduleChangelog(req, targetSchedule);
        } catch (InterruptedException e) {
            log.error("Create change log failed", e);
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }

        if (scheduleChangelog.getFlowInstanceId() == null) {
            log.info("No need to create approval flow,changelogId={}", scheduleChangelog.getId());
            executeChangeSchedule(req);
        }

        ChangeScheduleResp returnVal = new ChangeScheduleResp();
        BeanUtils.copyProperties(targetSchedule, returnVal);
        returnVal.setChangeLog(scheduleChangelog);
        return returnVal;
    }

    private ScheduleChangeLog createScheduleChangelog(ScheduleChangeParams req, Schedule targetSchedule)
            throws InterruptedException {
        Lock lock = jdbcLockRegistry.obtain(getScheduleChangeLockKey(req.getScheduleId()));
        // create change log for this request
        if (!lock.tryLock(10, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            if (scheduleChangeLogService.hasApprovingChangeLog(targetSchedule.getId())) {
                log.warn("Concurrent change schedule request is not allowed,scheduleId={}", targetSchedule.getId());
                throw new ConflictException(ErrorCodes.ResourceModifying,
                        "Concurrent change schedule request is not allowed");
            }

            String pre = null;
            String curr = null;
            if (req.getOperationType() == OperationType.UPDATE) {
                JSONObject preJsonObject = new JSONObject();
                preJsonObject.put("triggerConfig", targetSchedule.getTriggerConfig());
                preJsonObject.put("parameters", targetSchedule.getParameters());
                pre = preJsonObject.toJSONString();
                JSONObject currJsonOBject = new JSONObject();
                currJsonOBject.put("triggerConfig", req.getUpdateScheduleReq().getTriggerConfig());
                currJsonOBject.put("parameters", req.getUpdateScheduleReq().getParameters());
                curr = currJsonOBject.toJSONString();
            } else if (req.getOperationType() == OperationType.CREATE) {
                JSONObject currJsonOBject = new JSONObject();
                currJsonOBject.put("triggerConfig", req.getCreateScheduleReq().getTriggerConfig());
                currJsonOBject.put("parameters", req.getCreateScheduleReq().getParameters());
                curr = currJsonOBject.toJSONString();
            }

            ScheduleChangeLog changeLog = scheduleChangeLogService.createChangeLog(
                    ScheduleChangeLog.build(targetSchedule.getId(), req.getOperationType(), pre, curr,
                            ScheduleChangeStatus.APPROVING));
            log.info("Create change log success,changLog={}", changeLog);
            req.setScheduleChangeLogId(changeLog.getId());
            Long approvalFlowInstanceId;
            if (organizationService.get(targetSchedule.getId()).isPresent()
                    && organizationService.get(targetSchedule.getId()).get().getType() == OrganizationType.INDIVIDUAL) {
                approvalFlowInstanceId = null;
            } else {
                approvalFlowInstanceId = approvalFlowService.create(req);
            }
            if (approvalFlowInstanceId != null) {
                changeLog.setFlowInstanceId(approvalFlowInstanceId);
                scheduleChangeLogService.updateFlowInstanceIdById(changeLog.getId(), approvalFlowInstanceId);
                // only update status to approving when create schedule
                if (req.getOperationType() == OperationType.CREATE) {
                    scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.APPROVING);
                }
                log.info("Create approval flow success,changelogId={},flowInstanceId", approvalFlowInstanceId);
            }
            return changeLog;
        } finally {
            lock.unlock();
        }

    }

    private void validateTriggerConfig(TriggerConfig triggerConfig) {
        if (triggerConfig.getTriggerStrategy() == TriggerStrategy.CRON) {
            List<Date> nextFiveFireTimes =
                    QuartzCronExpressionUtils.getNextFiveFireTimes(triggerConfig.getCronExpression(), 2);
            if (nextFiveFireTimes.size() != 2) {
                throw new IllegalArgumentException("Invalid cron expression");
            }
            long intervalMills = nextFiveFireTimes.get(1).getTime() - nextFiveFireTimes.get(0).getTime();
            if (intervalMills / 1000 < 10 * 60) {
                throw new IllegalArgumentException(
                        "The interval between weeks is too short. The minimum interval is 10 minutes.");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
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
                ScheduleEntity entity = nullSafeGetById(req.getScheduleId());
                entity.setJobParametersJson(JsonUtils.toJson(req.getUpdateScheduleReq().getParameters()));
                entity.setTriggerConfigJson(JsonUtils.toJson(req.getUpdateScheduleReq().getTriggerConfig()));
                entity.setDescription(req.getUpdateScheduleReq().getDescription());
                entity.setStatus(ScheduleStatus.ENABLED);
                PreConditions.notNull(req.getUpdateScheduleReq(), "req.updateScheduleReq");
                if (req.getUpdateScheduleReq().getParameters() instanceof DataArchiveParameters) {
                    DataArchiveParameters parameters = (DataArchiveParameters) req.getUpdateScheduleReq()
                            .getParameters();
                    parameters.getRateLimit().setOrderId(req.getScheduleId());
                    dlmLimiterService.updateByOrderId(req.getScheduleId(), parameters.getRateLimit());
                }
                if (req.getUpdateScheduleReq().getParameters() instanceof DataDeleteParameters) {
                    DataDeleteParameters parameters = (DataDeleteParameters) req.getUpdateScheduleReq()
                            .getParameters();
                    parameters.getRateLimit().setOrderId(req.getScheduleId());
                    dlmLimiterService.updateByOrderId(req.getScheduleId(), parameters.getRateLimit());
                }
                targetSchedule = scheduleMapper.entityToModel(scheduleRepository.save(entity));
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
            case DELETE: {
                scheduleRepository.updateStatusById(targetSchedule.getId(), ScheduleStatus.DELETED);
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

        scheduleChangeLogService.updateStatusById(req.getScheduleChangeLogId(), ScheduleChangeStatus.SUCCESS);
        log.info("Change schedule success,scheduleId={},operationType={},changelogId={}", targetSchedule.getId(),
                req.getOperationType(), req.getScheduleChangeLogId());

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

    @Transactional(rollbackFor = Exception.class)
    public void innerTerminate(Long scheduleId) throws SchedulerException {
        ScheduleEntity schedule = nullSafeGetById(scheduleId);
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(schedule);
        quartzJobService.deleteJob(jobKey);
        scheduleRepository.updateStatusById(schedule.getId(), ScheduleStatus.TERMINATED);
    }

    /**
     * The method detects whether the database required for scheduled task operation exists. It returns
     * true and terminates the scheduled task if the database does not exist. If the database exists, it
     * returns false.
     */
    public boolean vetoJobExecution(Trigger trigger) {
        Schedule schedule;
        try {
            schedule = nullSafeGetModelById(Long.parseLong(trigger.getJobKey().getName()));
        } catch (Exception e) {
            log.warn("Get schedule failed,task will not be executed,job key={}", trigger.getJobKey(), e);
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.SCHEDULE_ID_NAME, trigger.getJobKey().getName())
                    .item(AlarmUtils.MESSAGE_NAME,
                            MessageFormat.format(
                                    "Job is misfired due to the failure to get the schedule, scheduleId={0}",
                                    trigger.getJobKey().getName()))
                    .build();
            AlarmUtils.alarm(SCHEDULING_FAILED, eventMessage);
            return true;
        }
        // Only perform automatic termination checks for periodic tasks
        if (trigger instanceof CronTrigger && !isValidSchedule(schedule)) {
            // terminate invalid schedule
            try {
                innerTerminate(schedule.getId());
            } catch (Exception e) {
                log.warn("Terminate invalid schedule failed,scheduleId={}", schedule.getId());
            }
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.ORGANIZATION_NAME, schedule.getOrganizationId().toString())
                    .item(AlarmUtils.SCHEDULE_ID_NAME, trigger.getJobKey().getName())
                    .item(AlarmUtils.MESSAGE_NAME,
                            MessageFormat.format("Job is misfired due to the schedule is invalid, scheduleId={0}",
                                    schedule.getId()))
                    .build();
            AlarmUtils.alarm(SCHEDULING_FAILED, eventMessage);
            return true;
        }
        // skip execution if concurrent scheduling is not allowed
        boolean rejectExecution = !schedule.getAllowConcurrent() && hasExecutingTask(schedule.getId());
        if (rejectExecution) {
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.ORGANIZATION_NAME, schedule.getOrganizationId().toString())
                    .item(AlarmUtils.SCHEDULE_ID_NAME, trigger.getJobKey().getName())
                    .item(AlarmUtils.MESSAGE_NAME, MessageFormat.format(
                            "The Job has reached its trigger time, but the previous task has not yet finished. This scheduling will be ignored, scheduleId={0}",
                            schedule.getId()))
                    .build();
            AlarmUtils.alarm(SCHEDULING_IGNORE, eventMessage);
        }
        return rejectExecution;
    }

    private boolean hasExecutingTask(Long scheduleId) {
        Optional<LatestTaskMappingEntity> optional = latestTaskMappingRepository.findByScheduleId(scheduleId);
        if (optional.isPresent()) {
            Optional<ScheduleTask> taskEntityOptional =
                    scheduleTaskService.findById(optional.get().getLatestScheduleTaskId());
            TaskStatus status = null;
            if (taskEntityOptional.isPresent() && !taskEntityOptional.get().getStatus().isTerminated()) {
                // correct the status of the task
                if (taskEntityOptional.get().getJobId() == null) {
                    scheduleTaskService.updateStatusById(taskEntityOptional.get().getId(), TaskStatus.FAILED);
                    return false;
                }
                status = taskEntityOptional.get().getStatus();
                log.info("Found executing task,scheduleId={},executingTaskId={},taskStatus={}", scheduleId,
                        taskEntityOptional.get().getId(), status);
                return true;
            }
            log.info("LatestTaskMapping={},LatestTaskStatus={}", optional.get(), status);
        }
        return false;
    }

    private boolean isValidSchedule(Schedule schedule) {
        // check project
        if (schedule.getProjectId() != null) {
            try {
                // The project is invalid or archived
                if (projectService.nullSafeGet(schedule.getProjectId()).getArchived()) {
                    return false;
                }
            } catch (NotFoundException e) {
                return false;
            }
        }
        // check database
        if (schedule.getDatabaseId() != null) {
            try {
                Database database = databaseService.getBasicSkipPermissionCheck(
                        schedule.getDatabaseId());
                // The database is invalid or does not belong to the current project
                if (!database.getExisted() || !Objects.equals(database.getProject().getId(), schedule.getProjectId())) {
                    return false;
                }
            } catch (NotFoundException e) {
                // database not found.
                return false;
            }
        }
        // schedule is valid.
        return true;
    }

    public void stopTask(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, true);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
        Lock lock = jdbcLockRegistry.obtain(getScheduleTaskLockKey(scheduleTaskId));
        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
            }
            scheduleTaskService.stop(scheduleTaskId);
        } catch (InterruptedException e) {
            log.error("Stop task failed", e);
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param scheduleId the task must belong to a valid schedule,so this param is not be null.
     * @param scheduleTaskId the task uid. Start a paused or pending task.
     */
    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, true);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
        Lock lock = jdbcLockRegistry.obtain(getScheduleTaskLockKey(scheduleTaskId));
        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
            }
            scheduleTaskService.start(scheduleTaskId);
        } catch (InterruptedException e) {
            log.error("Start task failed", e);
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        } finally {
            lock.unlock();
        }
    }

    public void rollbackTask(Long scheduleId, Long scheduleTaskId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
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
        try {
            ScheduleChangeLog changeLog = scheduleChangeLogService.getByFlowInstanceId(id);
            if (changeLog.getStatus() == ScheduleChangeStatus.APPROVING) {
                scheduleChangeLogService.updateStatusById(changeLog.getId(), ScheduleChangeStatus.SUCCESS);
            }
        } catch (NotFoundException e) {
            log.warn("Change log not found,flowInstanceId={}", id);
        }
    }

    public void updateJobParametersById(Long id, String jobParameters) {
        scheduleRepository.updateJobParametersById(id, jobParameters);
    }


    public ScheduleTaskDetailResp detailScheduleTask(Long scheduleId, Long scheduleTaskId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleTaskService.getScheduleTaskDetailResp(scheduleTaskId, schedule.getId());
    }

    @Deprecated
    public ScheduleTaskDetailRespHist detailScheduleTaskHist(Long scheduleId, Long scheduleTaskId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        ScheduleTaskDetailResp detailResp = scheduleTaskService.getScheduleTaskDetailResp(scheduleTaskId,
                schedule.getId());
        ScheduleTaskDetailRespHist returnValue = new ScheduleTaskDetailRespHist();
        returnValue.setId(detailResp.getId());
        returnValue.setCreateTime(detailResp.getCreateTime());
        returnValue.setStatus(detailResp.getStatus());
        returnValue.setUpdateTime(detailResp.getUpdateTime());
        returnValue.setJobName(schedule.getId().toString());
        returnValue.setJobGroup(detailResp.getType().name());
        returnValue.setExecutionDetails(detailResp.getExecutionDetails());
        return returnValue;
    }

    public ScheduleDetailResp detailSchedule(Long scheduleId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleResponseMapperFactory.generateScheduleDetailResp(schedule);
    }

    @Deprecated
    public ScheduleDetailRespHist detailScheduleHist(Long scheduleId) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId);
        return scheduleResponseMapperFactory.generateHistoryScheduleDetail(schedule);
    }

    public Page<ScheduleOverviewHist> list(@NotNull Pageable pageable, @NotNull QueryScheduleParams params) {
        if (StringUtils.isNotBlank(params.getCreator())) {
            params.setCreatorIds(userService.getUsersByFuzzyNameWithoutPermissionCheck(
                    params.getCreator()).stream().map(User::getId).collect(Collectors.toSet()));
        }
        if (authenticationFacade.currentOrganization().getType() == OrganizationType.TEAM) {
            Set<Long> joinedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
            if (CollectionUtils.isEmpty(joinedProjectIds)) {
                return Page.empty();
            }
            if (CollectionUtils.isEmpty(params.getProjectIds())) {
                params.setProjectIds(joinedProjectIds);
            } else {
                params.getProjectIds().retainAll(joinedProjectIds);
            }
        }
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        Page<ScheduleEntity> returnValue = scheduleRepository.find(pageable, params);
        Map<Long, ScheduleOverviewHist> scheduleId2Overview =
                scheduleResponseMapperFactory.generateHistoryScheduleList(returnValue.getContent());
        return returnValue.isEmpty() ? Page.empty()
                : returnValue.map(o -> scheduleId2Overview.get(o.getId()));
    }

    public Page<ScheduleOverviewHist> listUnfinishedSchedulesByProjectId(@NonNull Pageable pageable,
            @NonNull Long projectId) {
        return list(pageable, QueryScheduleParams.builder().projectIds(Collections.singleton(projectId))
                .statuses(ScheduleStatus.listUnfinishedStatus()).build());
    }

    public int getEnabledScheduleCountByProjectId(@NonNull Long projectId) {
        return scheduleRepository.getEnabledScheduleCountByProjectId(projectId);
    }

    public Page<ScheduleOverview> listScheduleOverview(@NotNull Pageable pageable,
            @NotNull QueryScheduleParams params) {
        log.info("List schedule overview req:{}", params);
        if (StringUtils.isNotEmpty(params.getId()) && !StringUtils.isNumeric(params.getId())) {
            return Page.empty();
        }
        if (StringUtils.isNotBlank(params.getCreator())) {
            Set<Long> creatorIds = userService.getUsersByFuzzyNameWithoutPermissionCheck(
                    params.getCreator()).stream().map(User::getId).collect(Collectors.toSet());
            if (creatorIds.isEmpty()) {
                return Page.empty();
            }
            params.setCreatorIds(creatorIds);
        }
        if (!CollectionUtils.isEmpty(params.getDataSourceIds()) || StringUtils.isNotEmpty(params.getClusterId())
                || StringUtils.isNotEmpty(params.getTenantId()) || StringUtils.isNotEmpty(params.getDataSourceName())) {
            QueryConnectionParams datasourceParams = QueryConnectionParams.builder()
                    .ids(params.getDataSourceIds())
                    .clusterNames(Collections.singletonList(params.getClusterId()))
                    .tenantNames(Collections.singletonList(params.getTenantId()))
                    .name(params.getDataSourceName())
                    .build();
            Set<Long> datasourceIds = connectionService.listSkipPermissionCheck(datasourceParams).stream().map(
                    ConnectionConfig::getId).collect(
                            Collectors.toSet());
            if (datasourceIds.isEmpty()) {
                return Page.empty();
            }
            params.setDataSourceIds(datasourceIds);
        }
        // load project by unique identifier if project id is null
        if (params.getProjectId() == null && StringUtils.isNotEmpty(params.getProjectUniqueIdentifier())) {
            Project project = projectService.getByIdentifier(params.getProjectUniqueIdentifier());
            if (project != null) {
                params.setProjectId(project.getId());
            }
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
        List<ScheduleEntity> schedules = returnValue.getContent();

        Map<Long, ScheduleOverview> id2Overview =
                scheduleResponseMapperFactory.generateScheduleOverviewListMapper(schedules);

        return returnValue.map(o -> id2Overview.get(o.getId()));
    }

    public Page<Schedule> listScheduleWithParameterSkipPermissionCheck(@NotNull Pageable pageable,
            @NotNull QueryScheduleParams params) {
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        return scheduleRepository.find(pageable, params).map(scheduleMapper::entityToModel);
    }

    public Page<ScheduleTaskOverview> listScheduleTaskOverview(@NotNull Pageable pageable, @NotNull Long scheduleId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, false);
        return scheduleTaskService.getScheduleTaskListResp(pageable, scheduleId);
    }

    public Page<ScheduleTaskListOverview> listScheduleTaskListOverview(@NotNull Pageable pageable,
            @NotNull QueryScheduleTaskParams params) {
        log.info("List schedule task overview req, params={}", params);
        if (StringUtils.isNotEmpty(params.getId()) && !StringUtils.isNumeric(params.getId())) {
            return Page.empty();
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
        QueryScheduleParams scheduleParams = QueryScheduleParams.builder()
                .id(params.getScheduleId())
                .name(params.getScheduleName())
                .dataSourceIds(params.getDataSourceIds())
                .databaseName(params.getDatabaseName())
                .type(params.getScheduleType())
                .creatorIds(params.getCreatorIds())
                .projectIds(params.getProjectIds())
                .organizationId(authenticationFacade.currentOrganizationId())
                .build();
        Set<Long> scheduleIds = scheduleRepository.find(Pageable.unpaged(), scheduleParams).getContent()
                .stream().map(ScheduleEntity::getId).collect(Collectors.toSet());
        if (scheduleIds.isEmpty()) {
            return Page.empty();
        }
        params.setScheduleIds(scheduleIds);
        Page<ScheduleTaskEntity> returnValue = scheduleTaskRepository.find(pageable, params);
        Map<Long, ScheduleTaskListOverview> taskId2Overview =
                scheduleResponseMapperFactory.generateScheduleTaskOverviewListMapper(returnValue.getContent());
        return returnValue.map(o -> taskId2Overview.get(o.getId()));
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

    public String getFullLogDownloadUrl(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId, false);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
        return scheduledTaskLoggerService.getFullLogDownloadUrl(scheduleId, scheduleTaskId, OdcTaskLogLevel.ALL);
    }

    public String getFullLogDownloadUrlWithoutPermission(Long scheduleId, Long scheduleTaskId) {
        return scheduledTaskLoggerService.getFullLogDownloadUrl(scheduleId, scheduleTaskId, OdcTaskLogLevel.ALL);
    }

    public String getLog(Long scheduleId, Long scheduleTaskId, OdcTaskLogLevel logLevel) {
        nullSafeGetByIdWithCheckPermission(scheduleId, false);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
        return scheduledTaskLoggerService.getLogContent(scheduleId, scheduleTaskId, logLevel);
    }

    public String getLogWithoutPermission(Long scheduleId, Long scheduleTaskId, OdcTaskLogLevel logLevel) {
        return scheduledTaskLoggerService.getLogContent(scheduleId, scheduleTaskId, logLevel);
    }

    public InputStreamResource downloadLog(Long scheduleId, Long scheduleTaskId) {
        nullSafeGetByIdWithCheckPermission(scheduleId);
        scheduleTaskService.nullSafeGetByIdAndScheduleId(scheduleTaskId, scheduleId);
        return scheduledTaskLoggerService.downloadLog(scheduleId, scheduleTaskId, OdcTaskLogLevel.ALL);
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

    public Optional<ScheduleTask> getLatestTask(Long id) {
        Optional<LatestTaskMappingEntity> optional = latestTaskMappingRepository.findByScheduleId(id);
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        List<ScheduleTask> res = scheduleTaskService.findByIds(
                Collections.singleton(optional.get().getLatestScheduleTaskId()));
        if (res.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(res.get(0));
    }

    public void terminateByDatasourceIds(Set<Long> datasourceIds) {
        Set<Long> scheduleIds = scheduleRepository.getEnabledScheduleByConnectionIds(datasourceIds).stream()
                .map(ScheduleEntity::getId).collect(
                        Collectors.toSet());
        if (scheduleIds.isEmpty()) {
            return;
        }
        scheduleIds.forEach(v -> {
            try {
                executeChangeSchedule(ScheduleChangeParams.with(v, OperationType.TERMINATE));
            } catch (Exception e) {
                log.warn("Terminate schedule failed,scheduleId={}", v, e);
            }
        });
    }

    public Map<ScheduleType, Long> countRunningScheduleByDatasourceIds(Set<Long> datasourceIds) {
        Map<Long, ScheduleEntity> id2Schedule = scheduleRepository.getEnabledScheduleByConnectionIds(
                datasourceIds).stream().collect(Collectors.toMap(ScheduleEntity::getId, o -> o));
        if (id2Schedule.isEmpty()) {
            return Collections.emptyMap();
        }

        List<LatestTaskMappingEntity> latestTaskMapping =
                latestTaskMappingRepository.findByScheduleIdIn(id2Schedule.keySet());

        Set<Long> hasRunningTaskScheduleId = scheduleTaskService.findByIds(
                latestTaskMapping.stream().map(LatestTaskMappingEntity::getLatestScheduleTaskId)
                        .collect(Collectors.toSet()))
                .stream().filter(o -> !o.getStatus().isTerminated()).map(o -> Long.parseLong(o.getJobName())).collect(
                        Collectors.toSet());
        Map<ScheduleType, Long> type2RunningTaskCount = new HashMap<>();
        hasRunningTaskScheduleId.forEach(scheduleId -> {
            if (id2Schedule.containsKey(scheduleId)) {
                ScheduleType type = id2Schedule.get(scheduleId).getType();
                if (type2RunningTaskCount.containsKey(type)) {
                    type2RunningTaskCount.put(type, type2RunningTaskCount.get(type) + 1);
                } else {
                    type2RunningTaskCount.put(type, 1L);
                }
            }
        });
        return type2RunningTaskCount;
    }

    public RateLimitConfiguration updateDlmRateLimit(Long scheduleId, RateLimitConfiguration rateLimit) {
        Schedule schedule = nullSafeGetByIdWithCheckPermission(scheduleId, true);
        RateLimitConfiguration rateLimitConfiguration = dlmLimiterService.updateByOrderId(schedule.getId(), rateLimit);
        syncRateLimitToRunningTask(scheduleId, rateLimit);
        return rateLimitConfiguration;
    }

    public void syncActionsToLogicalDatabaseTask(@NonNull Long scheduleTaskId, @NonNull String action,
            @NonNull String executionUnitId)
            throws InterruptedException, JobException {
        Lock lock = jdbcLockRegistry.obtain(getLogicalDatabaseChangeActionLockKey(executionUnitId));
        if (!lock.tryLock(5, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            Optional<ScheduleTask> taskOpt = scheduleTaskService.findById(scheduleTaskId);
            if (taskOpt.isPresent() && taskOpt.get().getStatus() == TaskStatus.RUNNING
                    && taskOpt.get().getJobId() != null) {
                ScheduleTask task = taskOpt.get();
                Map<String, String> map = new HashMap<>();
                map.put(action, executionUnitId);
                SpringContextUtil.getBean(JobScheduler.class).modifyJobParameters(task.getJobId(), map);
                log.info("Sync actions to executor success:{}", map);
            }
        } finally {
            lock.unlock();
        }
    }

    private void syncRateLimitToRunningTask(Long scheduleId, RateLimitConfiguration rateLimit) {
        Optional<ScheduleTask> latestTask = getLatestTask(scheduleId);
        if (latestTask.isPresent() && latestTask.get().getStatus() == TaskStatus.RUNNING
                && latestTask.get().getJobId() != null) {
            Map<String, String> map = new HashMap<>();
            map.put(JobParametersKeyConstants.DLM_RATE_LIMIT_CONFIG, JsonUtils.toJson(rateLimit));
            try {
                SpringContextUtil.getBean(JobScheduler.class).modifyJobParameters(latestTask.get().getJobId(), map);
                log.info("Sync rate limit to executor success:{}", map);
            } catch (JobException e) {
                log.warn("Sync limit config failed,jobId={}", latestTask.get().getJobId(), e);
            }
        }
    }

    private String getLogicalDatabaseChangeActionLockKey(@NonNull String executionId) {
        return "logical-database-change-action-execution-" + executionId;
    }

    private String getScheduleChangeLockKey(@NonNull Long scheduleId) {
        return "schedule-change-" + scheduleId;
    }

    private String getScheduleTaskLockKey(@NonNull Long scheduleTaskId) {
        return "schedule-task-" + scheduleTaskId;
    }
}
