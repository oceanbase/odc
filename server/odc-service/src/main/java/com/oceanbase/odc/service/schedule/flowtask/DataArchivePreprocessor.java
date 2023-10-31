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

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.checker.AbstractDLMChecker;
import com.oceanbase.odc.service.dlm.checker.DLMCheckerFactory;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.ScheduleTaskPreprocessor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.DlmEnvironment;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;

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
    private DlmEnvironment dlmEnvironment;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DlmLimiterService limiterService;

    @Override
    public void process(CreateFlowInstanceReq req) {
        AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
        if (parameters.getOperationType() == OperationType.CREATE) {
            DataArchiveParameters dataArchiveParameters =
                    (DataArchiveParameters) parameters.getScheduleTaskParameters();
            // Throw exception when the specified database does not exist or the current user does not have
            // permission to access it.
            Database sourceDb = databaseService.detail(dataArchiveParameters.getSourceDatabaseId());
            Database targetDb = databaseService.detail(dataArchiveParameters.getTargetDataBaseId());
            dataArchiveParameters.setSourceDatabaseName(sourceDb.getName());
            dataArchiveParameters.setTargetDatabaseName(targetDb.getName());
            dataArchiveParameters.setSourceDataSourceName(sourceDb.getDataSource().getName());
            dataArchiveParameters.setTargetDataSourceName(targetDb.getDataSource().getName());
            AbstractDLMChecker sourceDbChecker = DLMCheckerFactory.create(sourceDb);
            AbstractDLMChecker targetDbChecker = DLMCheckerFactory.create(targetDb);
            sourceDbChecker.checkTargetDbType(targetDb.getDataSource().getDialectType());
            if (dlmEnvironment.isSysTenantUserRequired()) {
                sourceDbChecker.checkSysTenantUser();
                targetDbChecker.checkSysTenantUser();
            }
            sourceDbChecker.checkSysTenantUser();
            sourceDbChecker.checkTablesPrimaryKey(dataArchiveParameters.getTables());
            sourceDbChecker.checkDLMTableCondition(dataArchiveParameters.getTables(),
                    dataArchiveParameters.getVariables());
            sourceDbChecker.dealloc();
            targetDbChecker.dealloc();
            log.info("Data archive preprocessing has been completed.");
            // pre create
            ScheduleEntity scheduleEntity = scheduleService.create(buildScheduleEntity(req, authenticationFacade));
            parameters.setTaskId(scheduleEntity.getId());
            // create job limit config
            RateLimitConfiguration limiterConfig = limiterService.getDefaultLimiterConfig();

            limiterService.createAndBindToOrder(scheduleEntity.getId(), limiterConfig);
        }
        req.setParentFlowInstanceId(parameters.getTaskId());
    }

}
