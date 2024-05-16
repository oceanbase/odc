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

import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.factory.FlowFactory;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OscLockDatabaseUserInfo;
import com.oceanbase.odc.service.onlineschemachange.model.OscSwapTableVO;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;
import com.oceanbase.odc.service.onlineschemachange.rename.OscDBUserUtil;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-06
 * @since 4.2.3
 */
@Service
@Slf4j
public class OscService {

    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;
    @Autowired
    private FlowFactory flowFactory;
    @Autowired
    private DatabaseService databaseService;


    @SkipAuthorize("internal authenticated")
    public OscLockDatabaseUserInfo getOscDatabaseInfo(@NonNull Long id) {

        Database database = databaseService.detail(id);
        OscLockDatabaseUserInfo oscDatabase = new OscLockDatabaseUserInfo();
        oscDatabase.setDatabaseId(database.getDatabaseId());
        oscDatabase.setLockDatabaseUserRequired(getLockUserIsRequired(database.getDataSource().getId()));
        return oscDatabase;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public OscSwapTableVO swapTable(@PathVariable Long scheduleTaskId) {

        Optional<ScheduleTaskEntity> scheduleTaskOptional = scheduleTaskRepository.findById(scheduleTaskId);
        PreConditions.validExists(ResourceType.ODC_SCHEDULE_TASK, "schedule task",
                scheduleTaskId, scheduleTaskOptional::isPresent);

        ScheduleTaskEntity scheduleTask = scheduleTaskOptional.get();

        Long scheduleId = Long.parseLong(scheduleTask.getJobName());
        Optional<ScheduleEntity> scheduleEntity = scheduleRepository.findById(scheduleId);

        PreConditions.validExists(ResourceType.ODC_SCHEDULE, "schedule ",
                scheduleId, scheduleEntity::isPresent);

        OnlineSchemaChangeParameters oscParameters = JsonUtils.fromJson(scheduleEntity.get().getJobParametersJson(),
                OnlineSchemaChangeParameters.class);

        Optional<FlowInstance> optional = flowFactory.getFlowInstance(oscParameters.getFlowInstanceId());
        FlowInstance flowInstance = optional.orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "id", oscParameters.getFlowInstanceId()));
        try {
            permissionValidator.checkCurrentOrganization(flowInstance);
        } finally {
            flowInstance.dealloc();
        }

        // check user permission, only creator can swap table manual
        PreConditions.validHasPermission(
                Objects.equals(authenticationFacade.currentUserId(), scheduleEntity.get().getCreatorId()),
                ErrorCodes.AccessDenied,
                "no permission swap table.");

        OnlineSchemaChangeScheduleTaskResult result = JsonUtils.fromJson(scheduleTask.getResultJson(),
                OnlineSchemaChangeScheduleTaskResult.class);

        PreConditions.validArgumentState(
                scheduleEntity.get().getJobType() == JobType.ONLINE_SCHEMA_CHANGE_COMPLETE,
                ErrorCodes.BadArgument, new Object[] {scheduleEntity.get().getJobType()},
                "Task type is not " + TaskType.ONLINE_SCHEMA_CHANGE.name());

        SwapTableType swapTableType = oscParameters.getSwapTableType();
        PreConditions.validArgumentState(swapTableType == SwapTableType.MANUAL,
                ErrorCodes.BadArgument, new Object[] {oscParameters.getSwapTableType()},
                "Swap table type is not " + SwapTableType.MANUAL.name());

        PreConditions.validArgumentState(!result.isManualSwapTableStarted(),
                ErrorCodes.OscSwapTableStarted, new Object[] {},
                "Swap table has started");

        PreConditions.validArgumentState(result.isManualSwapTableEnabled(),
                ErrorCodes.BadRequest, new Object[] {result.isManualSwapTableEnabled()},
                "Manual Swap table type is not enable ");

        // close manual swap table
        result.setManualSwapTableEnabled(false);
        // open start manual swap table
        result.setManualSwapTableStarted(true);
        scheduleTaskRepository.updateTaskResult(scheduleTaskId, JsonUtils.toJson(result));
        OscSwapTableVO oscSwapTable = new OscSwapTableVO();
        oscSwapTable.setScheduleTaskId(scheduleTaskId);
        return oscSwapTable;
    }

    private boolean getLockUserIsRequired(Long connectionId) {
        ConnectionConfig decryptedConnConfig =
                connectionService.getForConnectionSkipPermissionCheck(connectionId);
        return OscDBUserUtil.isLockUserRequired(decryptedConnConfig.getDialectType(),
                () -> {
                    ConnectionSessionFactory factory = new DefaultConnectSessionFactory(decryptedConnConfig);
                    String version = null;
                    ConnectionSession connSession = null;
                    try {
                        connSession = factory.generateSession();
                        version = ConnectionSessionUtil.getVersion(connSession);
                    } catch (Exception ex) {
                        log.info("Get connection occur error", ex);
                    } finally {
                        if (connSession != null) {
                            connSession.expire();
                        }
                    }
                    return version;
                });
    }


}
