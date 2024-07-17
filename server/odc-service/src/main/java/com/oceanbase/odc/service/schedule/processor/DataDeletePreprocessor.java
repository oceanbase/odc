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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMConfiguration;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
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
@ScheduleTaskPreprocessor(type = ScheduleType.DATA_DELETE)
public class DataDeletePreprocessor extends AbstractDlmPreprocessor {

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
            DataDeleteParameters parameters = req.getOperationType() == OperationType.CREATE
                    ? (DataDeleteParameters) req.getCreateScheduleReq().getParameters()
                    : (DataDeleteParameters) req.getUpdateScheduleReq().getParameters();
            initDefaultConfig(parameters);
            // Throw exception when the specified database does not exist or the current user does not have
            // permission to access it.
            // if check before delete, need verify target database.
            if (parameters.getNeedCheckBeforeDelete()) {
                if (Objects.isNull(parameters.getTargetDatabaseId())) {
                    throw new IllegalArgumentException("target database id can not be null");
                }
                Database targetDb = databaseService.detail(parameters.getTargetDatabaseId());
            }
            Database sourceDb = databaseService.detail(parameters.getDatabaseId());
            ConnectionConfig dataSource = sourceDb.getDataSource();
            dataSource.setDefaultSchema(sourceDb.getName());
            ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
            ConnectionSession connectionSession = connectionSessionFactory.generateSession();
            try {
                if (parameters.isFullDatabase()) {
                    parameters.setTables(getAllTables(connectionSession, sourceDb.getName()));
                }
                checkTableAndCondition(connectionSession, sourceDb, parameters.getTables(),
                        parameters.getVariables());
            } finally {
                connectionSession.expire();
            }
            log.info("QUICK-DELETE job preprocessing has been completed.");
        }
    }

    private void initDefaultConfig(DataDeleteParameters parameters) {
        parameters.setReadThreadCount(
                (int) (dlmConfiguration.getSingleTaskThreadPoolSize() * dlmConfiguration.getReadWriteRatio()
                        / (1 + dlmConfiguration.getReadWriteRatio())));
        parameters
                .setWriteThreadCount(dlmConfiguration.getSingleTaskThreadPoolSize() - parameters.getReadThreadCount());
        parameters.setScanBatchSize(dlmConfiguration.getDefaultScanBatchSize());
        parameters.setQueryTimeout(dlmConfiguration.getTaskConnectionQueryTimeout());
    }

}
