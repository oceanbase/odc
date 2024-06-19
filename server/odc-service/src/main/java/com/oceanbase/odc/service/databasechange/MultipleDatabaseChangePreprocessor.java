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
package com.oceanbase.odc.service.databasechange;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeProject;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.util.DescriptionGenerator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/5/11
 */
@Slf4j
@FlowTaskPreprocessor(type = TaskType.MULTIPLE_ASYNC)
public class MultipleDatabaseChangePreprocessor implements Preprocessor {
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;
    @Autowired
    private DatabaseChangeChangingOrderTemplateService templateService;
    @Autowired
    private FlowTaskProperties flowTaskProperties;

    @Override
    public void process(CreateFlowInstanceReq req) {
        MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
        Project project = projectService.detail(parameters.getProjectId());
        // Check the project permission
        List<Long> ids = parameters.getOrderedDatabaseIds().stream().flatMap(List::stream).collect(Collectors.toList());
        templateService.validateSizeAndNotDuplicated(ids);
        List<Database> databases = databaseService.listDatabasesDetailsByIds(ids);
        if (databases.size() < ids.size()) {
            throw new BadArgumentException(ErrorCodes.BadArgument, "some of these databases do not exist");
        }
        // All databases must belong to the project
        if (!databases.stream()
                .allMatch(databaseEntity -> databaseEntity.getProject() != null && Objects.equals(
                        databaseEntity.getProject().getId(), parameters.getProjectId()))) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    String.format("All databases must belong to the same project: %s", project.getName()));
        }
        PreConditions.maxLength(parameters.getSqlContent(), "sql content",
                flowTaskProperties.getSqlContentMaxLength());
        // must reset the batchId when initiating a multiple database flow again
        parameters.setBatchId(null);
        parameters.setProject(new DatabaseChangeProject(project));
        parameters.setDatabases(databases.stream().map(DatabaseChangeDatabase::new).collect(Collectors.toList()));
        parameters.setFlowTaskExecutionStrategy(req.getExecutionStrategy());
        req.setProjectId(parameters.getProjectId());
        req.setProjectName(project.getName());
        DescriptionGenerator.generateDescription(req);
    }

}
