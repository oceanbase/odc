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
package com.oceanbase.odc.service.flow.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.order.utils.DescriptionGenerator;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.flowtask.OperationType;
import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/3/13 15:35
 * @Descripition:
 */
@Slf4j
@Aspect
@Component
public class CreateFlowInstanceProcessAspect implements InitializingBean {


    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private List<Preprocessor> preprocessors;

    private final Map<JobType, Preprocessor> scheduleTaskPreprocessors = new HashMap<>();

    private final Map<TaskType, Preprocessor> flowTaskPreprocessors = new HashMap<>();

    @Pointcut("@annotation(com.oceanbase.odc.service.flow.processor.EnablePreprocess) && args(com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq)")
    public void processBeforeCreateFlowInstance() {}


    @Before("processBeforeCreateFlowInstance()")
    public void preprocess(JoinPoint point) throws Throwable {
        CreateFlowInstanceReq req = (CreateFlowInstanceReq) point.getArgs()[0];
        if (Objects.nonNull(req.getDatabaseId())) {
            adaptCreateFlowInstanceReq(req);
        }
        if (req.getTaskType() != TaskType.ALTER_SCHEDULE) {
            if (flowTaskPreprocessors.containsKey(req.getTaskType())) {
                flowTaskPreprocessors.get(req.getTaskType()).process(req);
            }
        } else {
            AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
            authenticateOperation(parameters);
            if (scheduleTaskPreprocessors.containsKey(parameters.getType())) {
                scheduleTaskPreprocessors.get(parameters.getType()).process(req);
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        preprocessors.forEach(preprocessor -> {
            // Init flow task processor.
            if (preprocessor.getClass().isAnnotationPresent(FlowTaskPreprocessor.class)) {
                FlowTaskPreprocessor annotation = preprocessor.getClass().getAnnotation(FlowTaskPreprocessor.class);
                if (annotation.isEnabled()) {
                    if (flowTaskPreprocessors.containsKey(annotation.type())) {
                        throw new RuntimeException(
                                String.format("The processor has already been defined,type=%s", annotation.type()));
                    }
                    flowTaskPreprocessors.put(annotation.type(), preprocessor);
                }
            }
            // Init schedule task processor.
            if (preprocessor.getClass().isAnnotationPresent(ScheduleTaskPreprocessor.class)) {
                ScheduleTaskPreprocessor annotation =
                        preprocessor.getClass().getAnnotation(ScheduleTaskPreprocessor.class);
                if (annotation.isEnabled()) {
                    if (scheduleTaskPreprocessors.containsKey(annotation.type())) {
                        throw new RuntimeException(
                                String.format("The processor has already been defined,type=%s", annotation.type()));
                    }
                    scheduleTaskPreprocessors.put(annotation.type(), preprocessor);
                }
            }
        });
    }

    private void authenticateOperation(AlterScheduleParameters parameters) {
        if (parameters.getOperationType() == OperationType.CREATE) {
            return;
        }
        PreConditions.notNull(parameters.getTaskId(), "scheduleId");
        ScheduleEntity scheduleEntity =
                scheduleService.nullSafeGetByIdWithCheckPermission(parameters.getTaskId(), true);
        parameters.setType(scheduleEntity.getJobType());
    }

    /**
     * get connectionId and projectId from database.
     */
    private void adaptCreateFlowInstanceReq(CreateFlowInstanceReq req) {
        Database database = databaseService.detail(req.getDatabaseId());
        if (Objects.isNull(database.getProject())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            throw new BadRequestException("Cannot create flow under default project in TEAM organization");
        }
        req.setProjectId(Objects.nonNull(database.getProject()) ? database.getProject().getId() : null);
        req.setProjectName(Objects.nonNull(database.getProject()) ? database.getProject().getName() : null);
        req.setConnectionId(database.getDataSource().getId());
        req.setConnectionName(database.getDataSource().getName());
        req.setDatabaseName(database.getName());
        req.setEnvironmentId(database.getDataSource().getEnvironmentId());
        req.setEnvironmentName(database.getEnvironment().getName());
        DescriptionGenerator.generateDescription(req);
        if (req.getTaskType() == TaskType.EXPORT) {
            DataTransferConfig config = (DataTransferConfig) req.getParameters();
            config.setDatabaseId(req.getDatabaseId());
        }
    }
}
