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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.BeanInjectedClassDelegate;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeFlowableTask;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.fsm.ActionFsm;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.RateLimiterConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Online schema change FSM base impl It's actually only have OMS impl all task state change should
 * impl in this class
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 11:56
 * @since 4.3.1
 */
@Slf4j
public abstract class OscActionFsmBase extends ActionFsm<OscActionContext, OscActionResult> {
    @Autowired
    protected ConnectionService connectionService;
    @Autowired
    protected ScheduleTaskService scheduleTaskService;
    @Autowired
    protected ScheduleService scheduleService;
    @Autowired
    protected ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    protected DataSourceOpenApiService dataSourceOpenApiService;
    @Autowired
    protected OmsProjectOpenApiService omsProjectOpenApiService;
    @Autowired
    protected OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    @Autowired
    protected DBSessionManageFacade dbSessionManageFacade;
    @Autowired
    protected ActionScheduler actionScheduler;
    // ugly impl, try impl only in scheduler or flow
    @Autowired
    protected FlowInstanceService flowInstanceService;
    // default is 432000 = 5*24*3600
    @Value("${osc-task-expired-after-seconds:432000}")
    protected long oscTaskExpiredAfterSeconds;

    // register state change action
    @PostConstruct
    public abstract void init();

    @Override
    public String resolveState(OscActionContext context) {
        ScheduleTaskEntity scheduleTaskEntity = context.getScheduleTask();
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTaskEntity.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        return parameters.getState();
    }

    /**
     * function entry for job
     * 
     * @param schedulerID
     * @param schedulerTaskID
     */
    public void schedule(Long schedulerID, Long schedulerTaskID) {
        OscActionContext oscActionContext = getOSCContext(schedulerID, schedulerTaskID);
        String state = resolveState(oscActionContext);
        // try process task may in expired or canceled or abnormal state
        // state should not in CLEAN_RESOURCE state
        if (!StringUtils.equals(OscStates.CLEAN_RESOURCE.getState(), state)
                && tryHandleInvalidTask(state, oscActionContext)) {
            return;
        }
        // do schedule
        schedule(oscActionContext);
        // if schedule failed, transfer to abnormal state
        syncFlowInstanceState(oscActionContext);
    }

    /**
     * process task with expired or canceled or abnormal
     * 
     * @param state current state
     * @param oscActionContext osc context
     * @return true if task in expired or canceled state
     */
    protected boolean tryHandleInvalidTask(String state, OscActionContext oscActionContext) {
        // task has expired
        // translate state to clean resource
        // NOTICE: expired should been checked first
        if (isTaskExpired(oscActionContext)) {
            transferTaskStatesWithStates(state, OscStates.CLEAN_RESOURCE.getState(), null,
                    oscActionContext.getScheduleTask(), TaskStatus.FAILED);
            return true;
        }
        // task has been canceled
        // translate state to clean resource
        if (isTaskCanceled(oscActionContext)) {
            transferTaskStatesWithStates(state, OscStates.CLEAN_RESOURCE.getState(), null,
                    oscActionContext.getScheduleTask(), TaskStatus.CANCELED);
            return true;
        }
        // handle flow status with failed or canceled
        FlowStatus flowStatus = getFlowInstanceStatus(oscActionContext);
        if (isFlowStatusInvalid(flowStatus)) {
            transferTaskStatesWithStates(state, OscStates.CLEAN_RESOURCE.getState(), null,
                    oscActionContext.getScheduleTask(), TaskStatus.CANCELED);
            log.info("OSC: flow task has failed, transfer state to clean resources, flow task id={}, status={}",
                    oscActionContext.getParameter().getFlowInstanceId(), flowStatus);
            return true;
        }
        // abnormal task do nothing
        return isTaskAbnormal(oscActionContext);
    }

    /**
     * sync task status with flow instance
     */
    public void syncFlowInstanceState(OscActionContext context) {
        try {
            OnlineSchemaChangeFlowableTask schemaChangeFlowableTask =
                    BeanInjectedClassDelegate
                            .instantiateDelegateWithoutPostConstructInvoke(OnlineSchemaChangeFlowableTask.class);
            OnlineSchemaChangeParameters parameters = context.getParameter();
            schemaChangeFlowableTask.tryCompleteTask(
                    parameters.getFlowInstanceId(), parameters.getFlowTaskID(),
                    context.getSchedule().getId());
        } catch (Throwable e) {
            log.warn("meet unhandled exception: cancel scheduler", e);
            actionScheduler.cancelScheduler(context.getSchedule().getId());
            throw new RuntimeException(e);
        }
    }

    /**
     * flow state in failed or canceled or abnormal
     * 
     * @param oscActionContext
     * @return
     */
    public FlowStatus getFlowInstanceStatus(OscActionContext oscActionContext) {
        Long flowInstanceID = oscActionContext.getParameter().getFlowInstanceId();
        Map<Long, FlowStatus> statusMap = flowInstanceService.getStatus(Collections.singleton(flowInstanceID));
        return statusMap.get(flowInstanceID);
    }

    // not found or failed
    private boolean isFlowStatusInvalid(FlowStatus flowStatus) {
        if (null == flowStatus) {
            return true;
        }
        return flowStatus == FlowStatus.EXECUTION_FAILED || flowStatus == FlowStatus.CANCELLED;
    }

    /**
     * start or resume FSM jump to create ghost table state for first start
     * 
     * @param schedulerID
     * @param schedulerTaskID
     */
    public void start(Long schedulerID, Long schedulerTaskID) {
        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(schedulerID);
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(schedulerTaskID);
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTaskEntity.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        OnlineSchemaChangeParameters onlineSchemaChangeParameters =
                parseOnlineSchemaChangeParameters(scheduleEntity.getJobParametersJson());
        String currentState = parameters.getState();
        // first yield, jump to create table state to decrease wait time
        if (StringUtils.equals(currentState, OscStates.YIELD_CONTEXT.getState())) {
            transferTaskStatesWithStates(OscStates.YIELD_CONTEXT.getState(), OscStates.CREATE_GHOST_TABLES.getState(),
                    null, scheduleTaskEntity, TaskStatus.RUNNING);
        } else {
            // recover current state
            scheduleTaskRepository.updateStatusById(schedulerTaskID, TaskStatus.RUNNING);
        }
        actionScheduler.submitFMSScheduler(scheduleEntity, schedulerTaskID,
                onlineSchemaChangeParameters.getFlowTaskID());
    }

    /**
     * check if task should continue
     * 
     * @param context
     * @return
     */
    protected boolean isTaskExpired(OscActionContext context) {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        // check task is expired
        Long scheduleId = context.getSchedule().getId();
        Long scheduleTaskId = scheduleTask.getId();
        Duration between = Duration.between(scheduleTask.getCreateTime().toInstant(), Instant.now());
        log.debug("Schedule id={} to check schedule task status with schedule task id={}", scheduleId, scheduleTaskId);

        if (between.toMillis() / 1000 > oscTaskExpiredAfterSeconds) {
            // schedule to clean resource
            // has canceled
            log.info("Schedule task id={} is  expired after {} seconds, so cancel the scheduleTaskId ",
                    scheduleTaskId, oscTaskExpiredAfterSeconds);
            return true;
        } else {
            return false;
        }
    }

    public boolean isTaskAbnormal(OscActionContext context) {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        return TaskStatus.ABNORMAL == scheduleTask.getStatus();
    }

    public boolean isTaskCanceled(OscActionContext context) {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        return TaskStatus.CANCELED == scheduleTask.getStatus();
    }

    @Override
    public void onActionComplete(String currentState, String nextState, String extraInfo, OscActionContext context) {
        if (StringUtils.equals(nextState, OscStates.COMPLETE.getState())) {
            log.info("OCS: complete state reached, delete scheduler, prev state={}, schedule id={}", currentState,
                    context.getSchedule().getId());
            actionScheduler.cancelScheduler(context.getSchedule().getId());
        }
        saveScheduleTaskStates(currentState, nextState, extraInfo, context.getScheduleTask().getId());
    }

    /**
     * correct task status and save
     */
    public void saveScheduleTaskStates(String currentState, String nextState, String extraInfo, Long schedulerTaskID) {
        log.info("OSC: fms state transfer from {} to {}", currentState, nextState);
        // info should only updated here
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(schedulerTaskID);
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTaskEntity.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        parameters.setState(nextState);
        parameters.setExtraInfo(extraInfo);
        // correct task status from Prepare -> Running
        if (scheduleTaskEntity.getStatus() == TaskStatus.PREPARING) {
            transferTaskStatesWithStates(currentState, nextState, extraInfo, scheduleTaskEntity, TaskStatus.RUNNING);
            return;
        }
        // swap table -> clean resource, correct task status to done
        if (StringUtils.equals(currentState, OscStates.SWAP_TABLE.getState())
                && StringUtils.equals(nextState, OscStates.CLEAN_RESOURCE.getState())
                && TaskStatus.RUNNING == scheduleTaskEntity.getStatus()) {
            transferTaskStatesWithStates(currentState, nextState, extraInfo, scheduleTaskEntity, TaskStatus.DONE);
            return;
        }
        // update task parameters only
        scheduleTaskRepository.updateTaskParameters(scheduleTaskEntity.getId(), JsonUtils.toJson(parameters));
    }

    /**
     * do terminate state transfer scheduler should invoked for resource release
     */
    public void transferTaskStatesWithStates(String currentState, String nextState, String extraInfo,
            ScheduleTaskEntity scheduleTaskEntity, TaskStatus taskStatus) {
        // info should only updated here
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTaskEntity.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        parameters.setState(nextState);
        parameters.setExtraInfo(extraInfo);
        if (TaskStatus.CANCELED == taskStatus) {
            // CANCEL should clean resource for force update
            parameters.setState(OscStates.CLEAN_RESOURCE.getState());
        }
        scheduleTaskEntity.setParametersJson(JsonUtils.toJson(parameters));
        scheduleTaskEntity.setStatus(taskStatus);
        if (TaskStatus.DONE == taskStatus || TaskStatus.CANCELED == taskStatus) {
            scheduleTaskEntity.setProgressPercentage(100.0);
        }
        log.info("Successfully update schedule task id={} set status={}", scheduleTaskEntity.getId(), taskStatus);
        scheduleTaskRepository.update(scheduleTaskEntity);
    }

    @Override
    public void handleException(OscActionContext context, Throwable e) {
        // we hope in resume mode continue, not drop oms project
        // not do state transfer
        scheduleTaskRepository.updateStatusById(context.getScheduleTask().getId(), TaskStatus.ABNORMAL);
    }

    protected OscActionContext getOSCContext(Long scheduleId, Long scheduleTaskId) {
        OscActionContext oscContext = null;
        try {
            oscContext = createOSCContext(scheduleId, scheduleTaskId);
            ValidatorUtils.verifyField(oscContext.getTaskParameter());
        } catch (Exception e) {
            getLogger().warn("Failed to create valve context, scheduleTaskId " + scheduleTaskId, e);
            throw new IllegalArgumentException("Failed to create valve context, scheduleTaskId " + scheduleTaskId, e);
        }
        return oscContext;
    }

    protected OscActionContext createOSCContext(Long scheduleId, Long scheduleTaskId) {
        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(scheduleId);
        OnlineSchemaChangeParameters onlineSchemaChangeParameters =
                parseOnlineSchemaChangeParameters(scheduleEntity.getJobParametersJson());
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(scheduleTaskId);
        OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters =
                parseOnlineSchemaChangeScheduleTaskParameters(scheduleTaskEntity.getParametersJson());

        OscActionContext oscContext = new OscActionContext();
        oscContext.setSchedule(scheduleEntity);
        oscContext.setScheduleTask(scheduleTaskEntity);
        oscContext.setParameter(onlineSchemaChangeParameters);
        oscContext.setTaskParameter(oscScheduleTaskParameters);
        oscContext.setScheduleTaskRepository(scheduleTaskRepository);
        oscContext.setConnectionProvider(new DefaultConnectionProvider(oscScheduleTaskParameters.getDatabaseName(),
                connectionService, scheduleEntity.getDataSourceId()));
        return oscContext;
    }

    /**
     * parse and set compatible fields
     *
     * @param jsonStr
     * @return
     */
    protected OnlineSchemaChangeParameters parseOnlineSchemaChangeParameters(String jsonStr) {
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                jsonStr, OnlineSchemaChangeParameters.class);
        // correct null value to default RateLimiterConfig object
        if (null == onlineSchemaChangeParameters.getRateLimitConfig()) {
            onlineSchemaChangeParameters.setRateLimitConfig(new RateLimiterConfig());
        }
        return onlineSchemaChangeParameters;
    }

    protected OnlineSchemaChangeScheduleTaskParameters parseOnlineSchemaChangeScheduleTaskParameters(String jsonStr) {
        OnlineSchemaChangeScheduleTaskParameters OnlineSchemaChangeScheduleTaskParameters = JsonUtils.fromJson(
                jsonStr, OnlineSchemaChangeScheduleTaskParameters.class);
        // correct null value to default RateLimiterConfig object
        if (null == OnlineSchemaChangeScheduleTaskParameters.getRateLimitConfig()) {
            OnlineSchemaChangeScheduleTaskParameters.setRateLimitConfig(new RateLimiterConfig());
        }
        return OnlineSchemaChangeScheduleTaskParameters;
    }

    /**
     * default connection provider wrapped connection service
     */
    protected static final class DefaultConnectionProvider implements ConnectionProvider {
        private final String dbName;
        private final ConnectionService connectionService;
        private final Long dataSourceID;
        private volatile ConnectionConfig config;

        public DefaultConnectionProvider(String dbName, ConnectionService connectionService, Long dataSourceID) {
            this.dbName = dbName;
            this.connectionService = connectionService;
            this.dataSourceID = dataSourceID;
        }

        @Override
        public ConnectionConfig connectionConfig() {
            if (null == config) {
                config = connectionService.getForConnectionSkipPermissionCheck(dataSourceID);
            }
            return config;
        }

        @Override
        public ConnectionSession createConnectionSession() {
            ConnectionConfig connectionConfig = connectionConfig();
            ConnectionSession connectionSession =
                    new DefaultConnectSessionFactory(connectionConfig, null, null, false, false).generateSession();
            ConnectionSessionUtil.setCurrentSchema(connectionSession,
                    dbName);
            return connectionSession;
        }
    }
}
