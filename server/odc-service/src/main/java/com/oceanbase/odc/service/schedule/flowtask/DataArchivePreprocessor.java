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
package com.oceanbase.odc.service.schedule.flowtask;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMConfiguration;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.ScheduleTaskPreprocessor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/25 17:35
 * @Descripition:
 */
@Slf4j
@ScheduleTaskPreprocessor(type = JobType.DATA_ARCHIVE)
public class DataArchivePreprocessor extends AbstractDlmJobPreprocessor {
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleTaskService scheduleTaskService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DlmLimiterService limiterService;

    @Autowired
    private DLMConfiguration dlmConfiguration;

    @Override
    public void process(CreateFlowInstanceReq req) {
        AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
        if (parameters.getOperationType() == OperationType.CREATE
                || parameters.getOperationType() == OperationType.UPDATE) {
            DataArchiveParameters dataArchiveParameters =
                    (DataArchiveParameters) parameters.getScheduleTaskParameters();
            initDefaultConfig(dataArchiveParameters);
            // Throw exception when the specified database does not exist or the current user does not have
            // permission to access it.
            Database sourceDb = databaseService.detail(dataArchiveParameters.getSourceDatabaseId());
            Database targetDb = databaseService.detail(dataArchiveParameters.getTargetDataBaseId());
            if (!dataArchiveParameters.getSyncTableStructure().isEmpty()
                    && sourceDb.getDataSource().getDialectType() != targetDb.getDataSource().getDialectType()) {
                throw new UnsupportedException(
                        "Different types of databases do not support structural synchronization.");
            }
            dataArchiveParameters.setSourceDatabaseName(sourceDb.getName());
            dataArchiveParameters.setTargetDatabaseName(targetDb.getName());
            dataArchiveParameters.setSourceDataSourceName(sourceDb.getDataSource().getName());
            dataArchiveParameters.setTargetDataSourceName(targetDb.getDataSource().getName());
            ConnectionConfig sourceDs = sourceDb.getDataSource();
            sourceDs.setDefaultSchema(sourceDb.getName());
            ConnectionSessionFactory sourceSessionFactory = new DefaultConnectSessionFactory(sourceDs);
            ConnectionSessionFactory targetSessionFactory = new DefaultConnectSessionFactory(targetDb.getDataSource());
            ConnectionSession sourceSession = sourceSessionFactory.generateSession();
            ConnectionSession targetSession = targetSessionFactory.generateSession();
            try {
                supportDataArchivingLink(sourceSession, targetSession);
                checkTableAndCondition(sourceSession, sourceDb, dataArchiveParameters.getTables(),
                        dataArchiveParameters.getVariables());
            } finally {
                sourceSession.expire();
                targetSession.expire();
            }
            if (parameters.getOperationType() == OperationType.CREATE) {
                // pre create
                ScheduleEntity scheduleEntity = buildScheduleEntity(req);
                scheduleEntity.setCreatorId(authenticationFacade.currentUser().id());
                scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
                scheduleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
                scheduleEntity = scheduleService.create(scheduleEntity);
                parameters.setTaskId(scheduleEntity.getId());
                // create job limit config
                initLimiterConfig(scheduleEntity.getId(), dataArchiveParameters.getRateLimit(), limiterService);
            }
            if (parameters.getOperationType() == OperationType.UPDATE) {
                parameters.setDescription(req.getDescription());
                ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(parameters.getTaskId());
                List<ScheduleTaskEntity> runningTasks = scheduleTaskService.listTaskByJobNameAndStatus(
                        scheduleEntity.getId().toString(), Arrays.asList(TaskStatus.RUNNING, TaskStatus.PREPARING));
                if (scheduleEntity.getStatus() != ScheduleStatus.PAUSE || !runningTasks.isEmpty()) {
                    throw new IllegalStateException(
                            String.format(
                                    "The task can only be edited when it is paused and there are no active subtasks executing."
                                            + "status=%s,subtaskCount=%s",
                                    scheduleEntity.getStatus(), runningTasks.size()));
                }
                // update job limit config
                limiterService.updateByOrderId(scheduleEntity.getId(), dataArchiveParameters.getRateLimit());
            }
            log.info("Data archive preprocessing has been completed.");
        }
        req.setParentFlowInstanceId(parameters.getTaskId());
    }

    private void supportDataArchivingLink(ConnectionSession sourceSession, ConnectionSession targetSession) {
        DialectType sourceDbType = sourceSession.getDialectType();
        DialectType targetDbType = targetSession.getDialectType();
        InformationExtensionPoint sourceInformation = ConnectionPluginUtil.getInformationExtension(sourceDbType);
        InformationExtensionPoint targetInformation = ConnectionPluginUtil.getInformationExtension(targetDbType);
        String sourceDbVersion = sourceSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                sourceInformation::getDBVersion);
        String targetDbVersion = targetSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).execute(
                targetInformation::getDBVersion);
        if (sourceDbType == DialectType.OB_MYSQL) {
            if (targetDbType != DialectType.OB_MYSQL && targetDbType != DialectType.MYSQL) {
                throw new UnsupportedException(
                        String.format("Unsupported data archiving link from %s to %s.", sourceDbType, targetDbType));
            }
        }
        // Cannot supports archive from mysql to ob.
        if (sourceDbType == DialectType.MYSQL) {
            if (targetDbType != DialectType.OB_MYSQL && targetDbType != DialectType.MYSQL) {
                throw new UnsupportedException(
                        String.format("Unsupported data archiving link from %s to %s.", sourceDbType, targetDbType));
            }
        }
        if (sourceDbType == DialectType.OB_ORACLE) {
            if (targetDbType != DialectType.OB_ORACLE) {
                throw new UnsupportedException(
                        String.format("Unsupported data archiving link from %s to %s.", sourceDbType, targetDbType));
            }
            if (VersionUtils.isGreaterThanOrEqualsTo(sourceDbVersion, "4.3.0")) {
                throw new UnsupportedException(
                        String.format("Unsupported OB Version:%s", sourceDbVersion));
            }
            if (VersionUtils.isGreaterThanOrEqualsTo(targetDbVersion, "4.3.0")) {
                throw new UnsupportedException(
                        String.format("Unsupported OB Version:%s", targetDbVersion));
            }
        }
    }

    private void initDefaultConfig(DataArchiveParameters parameters) {
        parameters.setReadThreadCount(
                (int) (dlmConfiguration.getSingleTaskThreadPoolSize() * dlmConfiguration.getReadWriteRatio()
                        / (1 + dlmConfiguration.getReadWriteRatio())));
        parameters
                .setWriteThreadCount(dlmConfiguration.getSingleTaskThreadPoolSize() - parameters.getReadThreadCount());
        parameters.setScanBatchSize(dlmConfiguration.getDefaultScanBatchSize());
        parameters.setQueryTimeout(dlmConfiguration.getTaskConnectionQueryTimeout());
        parameters.setShardingStrategy(dlmConfiguration.getShardingStrategy());
    }
}
