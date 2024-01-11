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
package com.oceanbase.odc.service.permission.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.QueryDatabaseParams;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter.ApplyDatabase;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

/**
 * @author gaoda.xy
 * @date 2024/1/8 15:31
 */
@FlowTaskPreprocessor(type = TaskType.APPLY_DATABASE_PERMISSION)
public class ApplyDatabasePermissionPreprocessor implements Preprocessor {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ApplyDatabaseParameter parameter = (ApplyDatabaseParameter) req.getParameters();
        Verify.notNull(parameter.getProject(), "project");
        Verify.notEmpty(parameter.getDatabases(), "databases");
        Verify.notEmpty(parameter.getPermissionTypes(), "permissionTypes");
        Verify.notNull(parameter.getApplyReason(), "applyReason");
        // Check resources and permissions
        Project project = projectService.detail(parameter.getProject().getId());
        Verify.verify(!project.getArchived(), "Project is archived");
        parameter.getProject().setName(project.getName());
        List<Database> allProjectDatabases =
                databaseService.list(QueryDatabaseParams.builder().projectId(project.getId()).containsUnassigned(false)
                        .existed(true).build(), Pageable.unpaged()).getContent();
        Map<Long, Database> id2Database =
                allProjectDatabases.stream().collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        List<ApplyDatabase> applyDatabases = new ArrayList<>();
        for (ApplyDatabase applyDatabase : parameter.getDatabases()) {
            if (!id2Database.containsKey(applyDatabase.getId())) {
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", applyDatabase.getId());
            }
            applyDatabases.add(ApplyDatabase.from(id2Database.get(applyDatabase.getId())));
        }
        parameter.setDatabases(applyDatabases);
        // Fill in other parameters
        req.setProjectId(project.getId());
        req.setProjectName(project.getName());
        Locale locale = LocaleContextHolder.getLocale();
        String i18nKey = "com.oceanbase.odc.builtin-resource.permission-apply.database.description";
        req.setDescription(I18n.translate(
                i18nKey,
                new Object[] {parameter.getPermissionTypes().stream().map(DatabasePermissionType::getLocalizedMessage)
                        .collect(Collectors.joining(","))},
                locale));
    }

}
