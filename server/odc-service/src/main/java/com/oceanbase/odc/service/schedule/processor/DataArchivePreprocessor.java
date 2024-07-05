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
package com.oceanbase.odc.service.schedule.processor;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMConfiguration;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/25 17:35
 * @Descripition:
 */
@Slf4j
@ScheduleTaskPreprocessor(type = ScheduleType.DATA_ARCHIVE)
public class DataArchivePreprocessor extends AbstractDlmPreprocessor {
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DlmLimiterService limiterService;

    @Autowired
    private DLMConfiguration dlmConfiguration;

    @Override
    public void process(ScheduleChangeParams req) {
        if (req.getOperationType() == OperationType.CREATE || req.getOperationType() == OperationType.UPDATE) {
            DataArchiveParameters parameters = req.getOperationType() == OperationType.CREATE
                    ? (DataArchiveParameters) req.getCreateScheduleReq().getParameters()
                    : (DataArchiveParameters) req.getUpdateScheduleReq().getParameters();

            initDefaultConfig(parameters);
            // Throw exception when the specified database does not exist or the current user does not have
            // permission to access it.
            Database sourceDb = databaseService.detail(parameters.getSourceDatabaseId());
            Database targetDb = databaseService.detail(parameters.getTargetDataBaseId());
            parameters.setSourceDatabase(sourceDb);
            parameters.setTargetDatabase(targetDb);
            if (!parameters.getSyncTableStructure().isEmpty()
                    && sourceDb.getDataSource().getDialectType() != targetDb.getDataSource().getDialectType()) {
                throw new UnsupportedException(
                        "Different types of databases do not support structural synchronization.");
            }
            ConnectionConfig sourceDs = sourceDb.getDataSource();
            sourceDs.setDefaultSchema(sourceDb.getName());
            ConnectionSessionFactory sourceSessionFactory = new DefaultConnectSessionFactory(sourceDs);
            ConnectionSessionFactory targetSessionFactory = new DefaultConnectSessionFactory(targetDb.getDataSource());
            ConnectionSession sourceSession = sourceSessionFactory.generateSession();
            ConnectionSession targetSession = targetSessionFactory.generateSession();
            try {
                supportDataArchivingLink(sourceSession, targetSession);
                checkTableAndCondition(sourceSession, sourceDb, parameters.getTables(),
                        parameters.getVariables());
            } finally {
                sourceSession.expire();
                targetSession.expire();
            }
            log.info("Data archive preprocessing has been completed.");
        }
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
