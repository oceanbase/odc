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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.fsm.ActionFsm;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.RateLimiterConfig;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CleanResourcesAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CreateDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.CreateGhostTableAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ModifyDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.MonitorDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.SwapTableAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.YieldContextAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Online schema change FSM impl It's actually only have OMS impl all task state change should impl
 * in this class
 * 
 * @author longpeng.zlp
 * @date 2024/7/8 11:56
 * @since 4.3.1
 */
@Slf4j
@Component
public class OscActionFsm extends ActionFsm<OscActionContext, OscActionResult> {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private DataSourceOpenApiService dataSourceOpenApiService;
    @Autowired
    private OmsProjectOpenApiService omsProjectOpenApiService;
    @Autowired
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    @Autowired
    private DBSessionManageFacade dbSessionManageFacade;
    @Autowired
    private ActionScheduler actionScheduler;
    // ugly impl, try impl only in scheduler or flow
    @Autowired
    private FlowInstanceService flowInstanceService;
    // default is 432000 = 5*24*3600
    @Value("${osc-task-expired-after-seconds:432000}")
    private long oscTaskExpiredAfterSeconds;

    // terminates code
    private final List<ErrorCode> terminalErrorCodes = Lists.newArrayList(ErrorCodes.BadArgument, ErrorCodes.BadRequest,
            ErrorCodes.OmsConnectivityTestFailed, ErrorCodes.Unexpected,
            ErrorCodes.OmsPreCheckFailed, ErrorCodes.OmsDataCheckInconsistent,
            ErrorCodes.OmsParamError, ErrorCodes.OmsBindTargetNotFound,
            ErrorCodes.OmsProjectExecutingFailed);

    // register state change action
    @PostConstruct
    public void init() {
        OscStatesTransfer statesTransfer = new OscStatesTransfer();
        // YIELD_CONTEXT -> CREATE_GHOST_TABLES
        registerEvent(OscStates.YIELD_CONTEXT.getState(),
                new YieldContextAction(actionScheduler, scheduleTaskRepository), statesTransfer,
                ImmutableSet.of(OscStates.CREATE_GHOST_TABLES.getState(), OscStates.COMPLETE.getState()));

        // CREATE_GHOST_TABLES -> CREATE_GHOST_TABLES | CREATE_DATA_TASK | COMPLETE(cancel)
        registerEvent(OscStates.CREATE_GHOST_TABLES.getState(), new CreateGhostTableAction(), statesTransfer,
                ImmutableSet.of(OscStates.CREATE_GHOST_TABLES.getState(), OscStates.CREATE_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // CREATE_DATA_TASK -> CREATE_DATA_TASK | MONITOR_DATA_TASK| COMPLETE(cnacel)
        registerEvent(OscStates.CREATE_DATA_TASK.getState(),
                CreateDataTaskAction.ofOMSCreateDataTaskAction(dataSourceOpenApiService, omsProjectOpenApiService,
                        onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.CREATE_DATA_TASK.getState(), OscStates.MONITOR_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // MONITOR_DATA_TASK -> MONITOR_DATA_TASK | MODIFY_DATA_TASK| SWAP_TABLE | COMPLETE(cancel)
        registerEvent(OscStates.MONITOR_DATA_TASK.getState(),
                MonitorDataTaskAction
                        .ofOMSMonitorDataTaskAction(omsProjectOpenApiService, onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.MONITOR_DATA_TASK.getState(), OscStates.MODIFY_DATA_TASK.getState(),
                        OscStates.SWAP_TABLE.getState(),
                        OscStates.COMPLETE.getState()));

        // MODIFY_DATA_TASK -> MODIFY_DATA_TASK | MONITOR_DATA_TASK | COMPLETE(cancel)
        registerEvent(OscStates.MODIFY_DATA_TASK.getState(),
                ModifyDataTaskAction.ofOMSModifyDataTaskAction(omsProjectOpenApiService, onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.MONITOR_DATA_TASK.getState(), OscStates.MODIFY_DATA_TASK.getState(),
                        OscStates.COMPLETE.getState()));

        // SWAP_TABLE -> SWAP_TABLE | CLEAN_RESOURCES | COMPLETE(cancel)
        registerEvent(OscStates.SWAP_TABLE.getState(),
                SwapTableAction.ofOMSSwapTableAction(dbSessionManageFacade, omsProjectOpenApiService,
                        onlineSchemaChangeProperties),
                statesTransfer,
                ImmutableSet.of(OscStates.SWAP_TABLE.getState(), OscStates.CLEAN_RESOURCE.getState(),
                        OscStates.COMPLETE.getState()));

        // CLEAN_RESOURCE -> YIELD_CONTEXT | CLEAN_RESOURCES | COMPLETE(cancel)
        registerEvent(OscStates.CLEAN_RESOURCE.getState(),
                CleanResourcesAction.ofOMSCleanResourcesAction(omsProjectOpenApiService), statesTransfer,
                ImmutableSet.of(OscStates.YIELD_CONTEXT.getState(), OscStates.CLEAN_RESOURCE.getState(),
                        OscStates.COMPLETE.getState()));
        // COMPLETE should not be scheduled
    }

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
        // CLEAN_RESOURCE should always schedule
        if (!StringUtils.equals(OscStates.CLEAN_RESOURCE.getState(), state) &&
            (isTaskExpired(oscActionContext) || isFlowInstanceFailed(state, oscActionContext))) {
            // transfer from current state to clean resources
            transferTaskStatesWithStates(state, OscStates.CLEAN_RESOURCE.getState(), null,
                    oscActionContext.getScheduleTask(), TaskStatus.CANCELED);
            return;
        }
        // do schedule
        schedule(oscActionContext);
    }

    public boolean isFlowInstanceFailed(String state, OscActionContext oscActionContext) {
        // only check in monitor data task state
        if (!StringUtils.equals(state, OscStates.MONITOR_DATA_TASK.getState())) {
            return false;
        }
        Long flowInstanceID = oscActionContext.getParameter().getFlowInstanceId();
        Map<Long, FlowStatus> statusMap = flowInstanceService.getStatus(Collections.singleton(flowInstanceID));
        FlowStatus taskStatus = statusMap.get(flowInstanceID);
        // not found or failed
        boolean ret = (null == taskStatus || taskStatus == FlowStatus.EXECUTION_FAILED);
        if (ret) {
            log.info("OSC: flow task has failed, transfer state to clean resources, flow task id {}, status {}",
                    flowInstanceID, taskStatus);
        }
        return ret;
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
        String currentState = parameters.getState();
        // first yield, jump to create table state to decrease wait time
        if (StringUtils.equals(currentState, OscStates.YIELD_CONTEXT.getState())) {
            transferTaskStatesWithStates(OscStates.YIELD_CONTEXT.getState(), OscStates.CREATE_GHOST_TABLES.getState(),
                    null, scheduleTaskEntity, TaskStatus.RUNNING);
        } else {
            // recover current state
            scheduleTaskRepository.updateStatusById(schedulerTaskID, TaskStatus.RUNNING);
        }
        actionScheduler.submitFMSScheduler(scheduleEntity, schedulerTaskID);
    }

    public void cancel(Long schedulerID, Long schedulerTaskID) {
        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(schedulerID);
        ScheduleTaskEntity scheduleTaskEntity = scheduleTaskService.nullSafeGetById(schedulerTaskID);
        OnlineSchemaChangeScheduleTaskParameters parameters = JsonUtils.fromJson(scheduleTaskEntity.getParametersJson(),
                OnlineSchemaChangeScheduleTaskParameters.class);
        String currentState = parameters.getState();
        transferTaskStatesWithStates(null, OscStates.CLEAN_RESOURCE.getState(),
                "CANCEL", scheduleTaskEntity, TaskStatus.CANCELED);
        // try submit scheduler in case task may in failed state
        actionScheduler.submitFMSScheduler(scheduleEntity, schedulerTaskID);
    }

    /**
     * check if task should continue
     * 
     * @param context
     * @return
     */
    private boolean isTaskExpired(OscActionContext context) {
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        OnlineSchemaChangeScheduleTaskParameters parameters =
                parseOnlineSchemaChangeScheduleTaskParameters(scheduleTask.getParametersJson());
        // check task is expired
        Long scheduleId = context.getSchedule().getId();
        Long scheduleTaskId = scheduleTask.getId();
        Duration between = Duration.between(scheduleTask.getCreateTime().toInstant(), Instant.now());
        log.info("Schedule id {} to check schedule task status with schedule task id {}", scheduleId, scheduleTaskId);

        if (between.toMillis() / 1000 > oscTaskExpiredAfterSeconds) {
            // schedule to clean resource
            // has canceled
            log.info("Schedule task id {} is  expired after {} seconds, so cancel the scheduleTaskId ",
                    scheduleTaskId, oscTaskExpiredAfterSeconds);
            return true;
        }
        // task has been canceled, clean resources must has been done
        if (scheduleTask.getStatus() == TaskStatus.CANCELED) {
            return true;
        }
        return false;
    }

    @Override
    public void onActionComplete(String currentState, String nextState, String extraInfo, OscActionContext context) {
        if (StringUtils.equals(nextState, OscStates.COMPLETE.getState())) {
            log.info("OCS: complete state reached, delete scheduler, prev state {}, schedule id {}", currentState,
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
        if (TaskStatus.DONE == taskStatus || TaskStatus.FAILED == taskStatus || TaskStatus.CANCELED == taskStatus) {
            scheduleTaskEntity.setProgressPercentage(100.0);
        }
        log.info("Successfully update schedule task id {} set status {}", scheduleTaskEntity.getId(), taskStatus);
        scheduleTaskRepository.update(scheduleTaskEntity);

    }

    @Override
    public void handleException(OscActionContext context, Throwable e) {
        // we hope in resume mode continue, not drop oms project
        // not do state transfer
        actionScheduler.cancelScheduler(context.getSchedule().getId());
        scheduleTaskRepository.updateStatusById(context.getScheduleTask().getId(), TaskStatus.FAILED);
    }

    private OscActionContext getOSCContext(Long scheduleId, Long scheduleTaskId) {
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

    private OscActionContext createOSCContext(Long scheduleId, Long scheduleTaskId) {
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
            connectionService, scheduleEntity.getConnectionId()));
        return oscContext;
    }

    /**
     * parse and set compatible fields
     *
     * @param jsonStr
     * @return
     */
    private OnlineSchemaChangeParameters parseOnlineSchemaChangeParameters(String jsonStr) {
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = JsonUtils.fromJson(
                jsonStr, OnlineSchemaChangeParameters.class);
        // correct null value to default RateLimiterConfig object
        if (null == onlineSchemaChangeParameters.getRateLimitConfig()) {
            onlineSchemaChangeParameters.setRateLimitConfig(new RateLimiterConfig());
        }
        return onlineSchemaChangeParameters;
    }

    private OnlineSchemaChangeScheduleTaskParameters parseOnlineSchemaChangeScheduleTaskParameters(String jsonStr) {
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
    private static final class DefaultConnectionProvider implements ConnectionProvider {
        private final String dbName;
        private final ConnectionService connectionService;
        private final Long connectionID;
        private volatile ConnectionConfig config;

        public DefaultConnectionProvider(String dbName, ConnectionService connectionService, Long connectionID) {
            this.dbName = dbName;
            this.connectionService = connectionService;
            this.connectionID = connectionID;
        }

        @Override
        public ConnectionConfig connectionConfig() {
            if (null == config) {
                config =  connectionService.getForConnectionSkipPermissionCheck(connectionID);
            }
            return config;
        }

        @Override
        public ConnectionSession createConnectionSession() {
            ConnectionConfig connectionConfig = connectionConfig();
            ConnectionSession connectionSession =
                new DefaultConnectSessionFactory(connectionConfig).generateSession();
            ConnectionSessionUtil.setCurrentSchema(connectionSession,
                dbName);
            return new DefaultConnectSessionFactory(connectionConfig).generateSession();
        }
    }
}
