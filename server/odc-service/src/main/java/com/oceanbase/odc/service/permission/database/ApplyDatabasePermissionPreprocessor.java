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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
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
    private ConnectionService connectionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ApplyDatabaseParameter parameter = (ApplyDatabaseParameter) req.getParameters();
        Verify.notNull(parameter.getProject(), "project");
        Verify.notEmpty(parameter.getDatabases(), "databases");
        Verify.notEmpty(parameter.getTypes(), "types");
        Verify.notNull(parameter.getApplyReason(), "applyReason");
        // Check resources and permissions
        Long projectId = parameter.getProject().getId();
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        Verify.verify(Boolean.FALSE.equals(projectEntity.getArchived()), "Project is archived");
        if (!projectService.checkPermission(projectId, ResourceRoleName.all())) {
            throw new AccessDeniedException();
        }
        parameter.getProject().setName(projectEntity.getName());
        List<Long> databaseIds =
                parameter.getDatabases().stream().map(ApplyDatabase::getId).collect(Collectors.toList());
        List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
        List<Long> connectionIds = databases.stream().map(e -> e.getDataSource().getId()).collect(Collectors.toList());
        Map<Long, Database> id2Database =
                databases.stream().collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        Map<Long, ConnectionConfig> id2ConnectionEntity = connectionService.innerListByIds(connectionIds).stream()
                .collect(Collectors.toMap(ConnectionConfig::getId, c -> c, (c1, c2) -> c1));
        for (ApplyDatabase database : parameter.getDatabases()) {
            if (!id2Database.containsKey(database.getId())) {
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", database.getId());
            }
            Database d = id2Database.get(database.getId());
            if (d.getProject() == null || !Objects.equals(d.getProject().getId(), projectId)) {
                throw new AccessDeniedException();
            }
            database.setName(d.getName());
            database.setDataSourceId(d.getDataSource().getId());
            database.setDataSourceName(id2ConnectionEntity.get(d.getDataSource().getId()).getName());
        }
        // Fill in other parameters
        req.setProjectId(projectId);
        req.setProjectName(projectEntity.getName());
        Locale locale = LocaleContextHolder.getLocale();
        String i18nKey = "com.oceanbase.odc.builtin-resource.permission-apply.database.description";
        req.setDescription(I18n.translate(
                i18nKey,
                new Object[] {parameter.getTypes().stream().map(DatabasePermissionType::getLocalizedMessage)
                        .collect(Collectors.joining(","))},
                locale));
    }

}
