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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMConfiguration;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
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
    private DatabaseService databaseService;

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
            supportDataArchivingLink(sourceDb.getDataSource(), targetDb.getDataSource());
            if (!parameters.getSyncTableStructure().isEmpty()) {
                PreConditions.validArgumentState(sourceDb.getDialectType() != targetDb.getDialectType(),
                        ErrorCodes.UnsupportedSyncTableStructure,
                        new Object[] {sourceDb.getDialectType(), targetDb.getDialectType()}, null);
            }
            ConnectionConfig sourceDs = sourceDb.getDataSource();
            sourceDs.setDefaultSchema(sourceDb.getName());
            ConnectionSessionFactory sourceSessionFactory = new DefaultConnectSessionFactory(sourceDs);
            ConnectionSession sourceSession = sourceSessionFactory.generateSession();
            try {
                if (parameters.isFullDatabase()) {
                    parameters.setTables(getAllTables(sourceSession, sourceDb.getName()));
                }
                if (parameters.getTables().isEmpty()) {
                    throw new IllegalArgumentException("The table list is empty.");
                }
                checkTableAndCondition(sourceSession, sourceDb, parameters.getTables(), parameters.getVariables());
            } finally {
                sourceSession.expire();
            }
            log.info("Data archive preprocessing has been completed.");
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
        if (parameters.getShardingStrategy() == null) {
            parameters.setShardingStrategy(dlmConfiguration.getShardingStrategy());
        }
    }
}
