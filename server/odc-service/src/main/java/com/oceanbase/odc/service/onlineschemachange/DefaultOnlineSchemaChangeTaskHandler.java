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
package com.oceanbase.odc.service.onlineschemachange;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.OdcException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBObjectOperators;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.model.LinkType;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.pipeline.BaseCreateOmsProjectValve;
import com.oceanbase.odc.service.onlineschemachange.pipeline.DefaultLinkPipeline;
import com.oceanbase.odc.service.onlineschemachange.pipeline.OscValveContext;
import com.oceanbase.odc.service.onlineschemachange.pipeline.Pipeline;
import com.oceanbase.odc.service.onlineschemachange.pipeline.ScheduleCheckOmsProjectValve;
import com.oceanbase.odc.service.onlineschemachange.pipeline.SwapTableNameValve;
import com.oceanbase.odc.service.onlineschemachange.subtask.OmsResourceCleanHandler;
import com.oceanbase.odc.service.onlineschemachange.subtask.OscTaskCompleteHandler;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * DefaultOnlineSchemaChangeTaskHandler
 *
 * @author yaobin
 * @date 2023-06-08
 * @since 4.2.0
 */
@Slf4j
@Component
public class DefaultOnlineSchemaChangeTaskHandler implements OnlineSchemaChangeTaskHandler {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ProjectOpenApiService projectOpenApiService;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private OscTaskCompleteHandler completeHandler;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private QuartzJobService quartzJobService;
    @Autowired
    private OmsResourceCleanHandler omsResourceCleanHandler;

    // default is 432000 = 5*24*3600
    @Value("${osc-task-expired-after-seconds:432000}")
    private long oscTaskExpiredAfterSeconds;

    private Map<LinkType, Pipeline> preparePipelineMap;
    private Map<LinkType, Pipeline> completePipelineMap;

    private List<ErrorCode> terminalErrorCodes;
    private List<TaskStatus> expectedTaskStatus;

    @PostConstruct
    public void init() {
        Pipeline preparePipeline = new DefaultLinkPipeline();
        preparePipeline.setBasic(SpringContextUtil.getBean(BaseCreateOmsProjectValve.class));
        Map<LinkType, Pipeline> prepareLinks = new HashMap<>();
        prepareLinks.put(LinkType.OMS, preparePipeline);
        this.preparePipelineMap = Collections.unmodifiableMap(prepareLinks);

        Pipeline completePipeline = new DefaultLinkPipeline();
        completePipeline.addValve(SpringContextUtil.getBean(ScheduleCheckOmsProjectValve.class));
        completePipeline.setBasic(SpringContextUtil.getBean(SwapTableNameValve.class));
        Map<LinkType, Pipeline> completeLinks = new HashMap<>();
        completeLinks.put(LinkType.OMS, completePipeline);
        this.completePipelineMap = Collections.unmodifiableMap(completeLinks);

        terminalErrorCodes = Lists.newArrayList(ErrorCodes.BadArgument, ErrorCodes.BadRequest,
                ErrorCodes.OmsConnectivityTestFailed, ErrorCodes.Unexpected,
                ErrorCodes.OmsPreCheckFailed, ErrorCodes.OmsDataCheckInconsistent,
                ErrorCodes.OmsParamError);

        expectedTaskStatus = Lists.newArrayList(TaskStatus.DONE, TaskStatus.FAILED,
                TaskStatus.CANCELED, TaskStatus.RUNNING);
    }

    @Override
    public void start(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        log.info("Start execute {}, to start schedule task id {}", getClass().getSimpleName(), scheduleTaskId);
        OscValveContext valveContext = null;
        try {
            valveContext = getOscValveContext(scheduleId, scheduleTaskId);
        } catch (Exception ex) {
            scheduleTaskRepository.updateStatusById(scheduleTaskId, TaskStatus.FAILED);
            throw new IllegalArgumentException("Failed to start osc job with scheduleTaskId " + scheduleTaskId, ex);
        }
        scheduleTaskRepository.updateStatusById(scheduleTaskId, TaskStatus.RUNNING);
        doStart(scheduleTaskId, valveContext);
    }

    private void doStart(Long scheduleTaskId, OscValveContext valveContext) {
        ConnectionSession connectionSession = null;
        try {
            connectionSession =
                    new DefaultConnectSessionFactory(valveContext.getConnectionConfig()).generateSession();
            ConnectionSessionUtil.setCurrentSchema(connectionSession,
                    valveContext.getTaskParameter().getDatabaseName());
            prepareSchema(valveContext.getParameter(), valveContext.getTaskParameter(),
                    connectionSession, scheduleTaskId);
            valveContext.setConnectionSession(connectionSession);
            valveContext.setLinkType(LinkType.OMS);
            preparePipelineMap.get(LinkType.OMS).invoke(valveContext);

        } catch (Exception e) {
            log.warn("Failed to start osc job with taskId={}.", scheduleTaskId, e);
            failedOscTask(valveContext);
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
    }

    @Override
    public void complete(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        ScheduleTaskEntity scheduleTask = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTask.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        // check task is expired
        Duration between = Duration.between(scheduleTask.getCreateTime().toInstant(), Instant.now());
        log.info("Schedule id {} to check schedule task status with schedule task id {}", scheduleId, scheduleTaskId);

        if (between.toMillis() / 1000 > oscTaskExpiredAfterSeconds) {
            completeHandler.onOscScheduleTaskCancel(parameters.getOmsProjectId(),
                    parameters.getUid(), scheduleId, scheduleTaskId);
            log.info("Schedule task id {} is  expired after {} seconds, so cancel the scheduleTaskId ",
                    scheduleTaskId, oscTaskExpiredAfterSeconds);
            deleteQuartzJob(scheduleId);
            return;
        }

        doComplete(scheduleId, scheduleTaskId, scheduleTask, parameters);
    }

    @Override
    public void terminate(@NonNull Long scheduleId, @NonNull Long scheduleTaskId) {
        ScheduleTaskEntity scheduleTask = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTask.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        completeHandler.onOscScheduleTaskCancel(parameters.getOmsProjectId(),
                parameters.getUid(), scheduleId, scheduleTaskId);
    }

    private void doComplete(Long scheduleId, Long scheduleTaskId, ScheduleTaskEntity scheduleTask,
            OnlineSchemaChangeScheduleTaskParameters parameters) {
        PreConditions.validArgumentState(expectedTaskStatus.contains(scheduleTask.getStatus()), ErrorCodes.Unexpected,
                new Object[] {scheduleTask.getStatus()}, "schedule task is not excepted status");
        if (scheduleTask.getStatus() == TaskStatus.RUNNING) {
            continueComplete(scheduleId, scheduleTaskId);
            return;
        }
        // Oms project must be released if current schedule task is not running
        boolean released = checkAndReleaseProject(parameters.getOmsProjectId(), parameters.getUid());
        if (!released) {
            return;
        }

        if (scheduleTask.getStatus() == TaskStatus.DONE) {
            scheduleNextTask(scheduleId, scheduleTaskId);
            return;
        } else if (scheduleTask.getStatus() == TaskStatus.CANCELED) {
            log.info("Because task is canceled, so delete quartz job {}", scheduleId);
            deleteQuartzJob(scheduleId);
            return;
        }

        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(scheduleId);
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                scheduleEntity.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        if (onlineSchemaChangeParameters.getErrorStrategy() == TaskErrorStrategy.CONTINUE) {
            log.info("Because error strategy is continue, so schedule next task");
            // schedule next task
            scheduleNextTask(scheduleId, scheduleTaskId);
        } else {
            log.info("Because error strategy is abort, so delete quartz job {}", scheduleId);
            deleteQuartzJob(scheduleId);
        }

    }

    private void scheduleNextTask(Long scheduleId, Long currentScheduleTaskId) {
        Optional<ScheduleTaskEntity> nextTask = findFirstConditionScheduleTask(scheduleId,
                s -> s.getStatus() == TaskStatus.PREPARING);
        if (!nextTask.isPresent()) {
            log.info("No preparing status schedule task for next schedule, schedule id {}, delete quartz job",
                    scheduleId);
            deleteQuartzJob(scheduleId);
            return;
        }
        Long nextTaskId = nextTask.get().getId();
        try {
            start(scheduleId, nextTaskId);
            log.info("Successfully start next schedule task with id {}", nextTaskId);
        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "Failed to schedule next, schedule id {0}, "
                                    + "current schedule task Id {1}, next schedule task {2}",
                            scheduleId, currentScheduleTaskId, nextTaskId),
                    e);
        }

    }

    private boolean checkAndReleaseProject(String omsProjectId, String uid) {
        if (omsProjectId == null) {
            return true;
        }

        ProjectControlRequest controlRequest = new ProjectControlRequest();
        controlRequest.setId(omsProjectId);
        controlRequest.setUid(uid);
        log.info("Oms project {} has not released, try to release it.", omsProjectId);
        return omsResourceCleanHandler.checkAndReleaseProject(controlRequest);
    }

    private void deleteQuartzJob(Long scheduleId) {
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        try {
            quartzJobService.deleteJob(jobKey);
            log.info("Successfully delete job with jobKey {}", jobKey);
        } catch (SchedulerException e) {
            log.warn("Delete job occur error with jobKey =" + jobKey, e);
        }
    }

    private Optional<ScheduleTaskEntity> findFirstConditionScheduleTask(
            Long scheduleId, Predicate<ScheduleTaskEntity> predicate) {
        Specification<ScheduleTaskEntity> specification = Specification
                .where(ScheduleTaskSpecs.jobNameEquals(scheduleId + ""));
        return scheduleTaskRepository.findAll(specification, Sort.by("id"))
                .stream()
                .filter(s -> predicate == null || predicate.test(s))
                .findFirst();
    }

    private void continueComplete(Long scheduleId, Long scheduleTaskId) {

        OscValveContext valveContext = getOscValveContext(scheduleId, scheduleTaskId);
        try {
            completePipelineMap.get(LinkType.OMS).invoke(valveContext);
            if (valveContext.isSwapSucceedCallBack()) {
                completeHandler.onOscScheduleTaskSuccess(valveContext.getTaskParameter().getOmsProjectId(),
                        valveContext.getTaskParameter().getUid(), valveContext.getSchedule().getId(),
                        valveContext.getScheduleTask().getId());
            }
        } catch (Exception e) {
            log.warn("Failed to complete osc job with scheduleTaskId " + scheduleTaskId, e);
            handleExceptionResult(valveContext, e);
        }
    }

    private void handleExceptionResult(OscValveContext valveContext, Exception e) {
        if (e instanceof OdcException) {
            OdcException actual = (OdcException) e;
            ErrorCode errorCode = actual.getErrorCode();
            if (terminalErrorCodes.contains(errorCode)) {
                failedOscTask(valveContext);
            }
        } else {
            failedOscTask(valveContext);
        }
    }

    private void failedOscTask(OscValveContext valveContext) {
        completeHandler.onOscScheduleTaskFailed(valveContext.getTaskParameter().getOmsProjectId(),
                valveContext.getTaskParameter().getUid(), valveContext.getSchedule().getId(),
                valveContext.getScheduleTask().getId());
        ConnectionSession connectionSession =
                new DefaultConnectSessionFactory(valveContext.getConnectionConfig()).generateSession();
        try {
            dropNewTableIfExits(valveContext.getTaskParameter(), connectionSession);
        } finally {
            if (connectionSession != null) {
                connectionSession.expire();
            }
        }
    }

    private OscValveContext getOscValveContext(Long scheduleId, Long scheduleTaskId) {
        OscValveContext valveContext = null;
        try {
            valveContext = createValveContext(scheduleId, scheduleTaskId);
            ValidatorUtils.verifyField(valveContext.getTaskParameter());
        } catch (Exception e) {
            log.warn("Failed to create valve context, scheduleTaskId " + scheduleTaskId, e);
            throw new IllegalArgumentException("Failed to create valve context, scheduleTaskId " + scheduleTaskId, e);
        }
        return valveContext;
    }

    private OscValveContext createValveContext(Long scheduleId, Long scheduleTaskId) {
        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(scheduleId);
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                scheduleEntity.getJobParametersJson(), OnlineSchemaChangeParameters.class);
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters = JsonUtils.fromJson(
                scheduleTaskEntity.getParametersJson(), OnlineSchemaChangeScheduleTaskParameters.class);

        OscValveContext valveContext = new OscValveContext();
        valveContext.setSchedule(scheduleEntity);
        valveContext.setScheduleTask(scheduleTaskEntity);
        valveContext.setParameter(onlineSchemaChangeParameters);
        valveContext.setTaskParameter(oscScheduleTaskParameters);

        ConnectionConfig config =
                connectionService.getForConnectionSkipPermissionCheck(scheduleEntity.getConnectionId());
        config.setDefaultSchema(valveContext.getTaskParameter().getDatabaseName());
        valveContext.setConnectionConfig(config);
        return valveContext;
    }

    private void prepareSchema(OnlineSchemaChangeParameters param, OnlineSchemaChangeScheduleTaskParameters taskParam,
            ConnectionSession session, Long scheduleTaskId) throws SQLException {

        dropNewTableIfExits(taskParam, session);

        SyncJdbcExecutor executor = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        String finalTableDdl;
        executor.execute(taskParam.getNewTableCreateDdl());
        if (param.getSqlType() == OnlineSchemaChangeSqlType.ALTER) {
            taskParam.getSqlsToBeExecuted().forEach(executor::execute);

            // update new table ddl for display
            finalTableDdl = DdlUtils.queryOriginTableCreateDdl(session, taskParam.getNewTableName());
            String ddlForDisplay = DdlUtils.replaceTableName(finalTableDdl, taskParam.getOriginTableName(),
                    session.getDialectType(), OnlineSchemaChangeSqlType.CREATE);
            taskParam.setNewTableCreateDdlForDisplay(ddlForDisplay);
            scheduleTaskRepository.updateTaskResult(scheduleTaskId,
                    JsonUtils.toJson(new OnlineSchemaChangeScheduleTaskResult(taskParam)));
        } else {
            finalTableDdl = DdlUtils.queryOriginTableCreateDdl(session, taskParam.getNewTableName());
        }
        log.info("Successfully created new table, ddl: {}", finalTableDdl);
        validateColumnDifferent(taskParam, session);
    }

    private void validateColumnDifferent(OnlineSchemaChangeScheduleTaskParameters taskParam,
            ConnectionSession session) {
        List<String> originTableColumns =
                DBSchemaAccessors.create(session).listTableColumns(taskParam.getDatabaseName(),
                        DdlUtils.getUnwrappedName(taskParam.getOriginTableNameUnwrapped())).stream()
                        .map(DBTableColumn::getName).collect(Collectors.toList());

        List<String> newTableColumns =
                DBSchemaAccessors.create(session).listTableColumns(taskParam.getDatabaseName(),
                        DdlUtils.getUnwrappedName(taskParam.getNewTableNameUnwrapped())).stream()
                        .map(DBTableColumn::getName).collect(Collectors.toList());

        if (!CollectionUtils.isEqualCollection(originTableColumns, newTableColumns)) {
            throw new UnsupportedException(ErrorCodes.OscColumnNameInconsistent,
                    null, "Column name of origin table is inconsistent with new table.");
        }
    }

    private void dropNewTableIfExits(OnlineSchemaChangeScheduleTaskParameters taskParam, ConnectionSession session) {
        List<String> list = DBSchemaAccessors.create(session)
                .showTablesLike(taskParam.getDatabaseName(), taskParam.getNewTableNameUnwrapped());
        // Drop new table suffix with _osc_new_ if exists
        if (CollectionUtils.isNotEmpty(list)) {
            DBObjectOperators.create(session)
                    .drop(DBObjectType.TABLE, taskParam.getDatabaseName(), taskParam.getNewTableNameUnwrapped());
        }
    }

}
