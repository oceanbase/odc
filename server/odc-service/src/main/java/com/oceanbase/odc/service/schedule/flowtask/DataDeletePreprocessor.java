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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.ScheduleTaskPreprocessor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/25 17:35
 * @Descripition:
 */
@Slf4j
@ScheduleTaskPreprocessor(type = JobType.DATA_DELETE)
public class DataDeletePreprocessor extends AbstractDlmJobPreprocessor {

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
            DataDeleteParameters dataDeleteParameters =
                    (DataDeleteParameters) parameters.getScheduleTaskParameters();
            // Throw exception when the specified database does not exist or the current user does not have
            // permission to access it.
            Database sourceDb = databaseService.detail(dataDeleteParameters.getDatabaseId());
            checkDatasource(sourceDb.getDataSource());
            ConnectionConfig dataSource = sourceDb.getDataSource();
            dataSource.setDefaultSchema(sourceDb.getName());
            ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
            ConnectionSession connectionSession = connectionSessionFactory.generateSession();
            try {
                checkTableAndCondition(connectionSession, sourceDb, dataDeleteParameters.getTables(),
                        dataDeleteParameters.getVariables());
            } finally {
                connectionSession.expire();
            }
            log.info("QUICK-DELETE job preprocessing has been completed.");
            // pre create
            ScheduleEntity scheduleEntity = buildScheduleEntity(req);
            scheduleEntity.setCreatorId(authenticationFacade.currentUser().id());
            scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
            scheduleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
            scheduleEntity = scheduleService.create(scheduleEntity);
            parameters.setTaskId(scheduleEntity.getId());
            initLimiterConfig(scheduleEntity.getId(), dataDeleteParameters.getRateLimit(), limiterService);
        }
        req.setParentFlowInstanceId(parameters.getTaskId());
    }

}
