/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeProperties;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.util.DescriptionGenerator;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/5/11
 */
@Slf4j
@FlowTaskPreprocessor(type = TaskType.MULTIPLE_ASYNC)
public class MultipleDatabaseChangePreprocessor implements Preprocessor {
    @Autowired
    private DatabaseService      databaseService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ScheduleService      scheduleService;
    @Autowired
    private List<Preprocessor>   preprocessors;
    @Autowired
    private ProjectService       projectService;
    @Autowired
    private DatabaseRepository   databaseRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;
    @Autowired
    private ProjectPermissionValidator        projectPermissionValidator;

    @Autowired
    private SecurityManager securityManager;
    @Override
    public void process(CreateFlowInstanceReq req) {
        MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
        ProjectEntity projectEntity = projectRepository.findById(parameters.getProjectId()).orElseThrow(
            () -> new NotFoundException(ResourceType.ODC_PROJECT, "id", req.getProjectId()));
        // Check the project permission
        projectPermissionValidator.checkProjectRole(projectEntity.getId(), ResourceRoleName.all());
        List<Long> ids = parameters.getOrderedDatabaseIds().stream().flatMap(List::stream).collect(Collectors.toList());
        // Limit the number of multi-databases change
        if (ids.size() <= DatabaseChangeProperties.MIN_DATABASE_COUNT
            || ids.size() > DatabaseChangeProperties.MAX_DATABASE_COUNT) {
            throw new BadArgumentException(ErrorCodes.IllegalArgument,
                "The number of databases must be greater than " + DatabaseChangeProperties.MIN_DATABASE_COUNT
                + " and not more than " + DatabaseChangeProperties.MAX_DATABASE_COUNT + ".");
        }
        // Databases with the same name are not allowed
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BadArgumentException(ErrorCodes.IllegalArgument,
                "Database cannot be duplicated.");
        }
        List<Database> databases = databaseService.listDatabasesDetailsByIds(ids);
        // All databases must belong to the project
        if(!databases.stream().allMatch(databaseEntity -> databaseEntity.getProject().getId()==parameters.getProjectId())){
            throw new BadArgumentException(ErrorCodes.IllegalArgument, "All databases must belong to the same project："+ projectEntity.getName());
        }
        // must reset the batchid when initiating a multiple database flow again
        parameters.setBatchId(null);
        parameters.setDatabases(databases);
        req.setProjectId(parameters.getProjectId());
        req.setProjectName(projectEntity.getName());
        DescriptionGenerator.generateDescription(req);
    }
}
