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
package com.oceanbase.odc.service.permission.table;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter.ApplyTable;

/**
 * ClassName: ApplyTablePermissionPreprocessor Package: com.oceanbase.odc.service.permission.table
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/18 17:38
 * @Version 1.0
 */
@FlowTaskPreprocessor(type = TaskType.APPLY_TABLE_PERMISSION)
public class ApplyTablePermissionPreprocessor implements Preprocessor {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionService connectionService;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ApplyTableParameter parameter = (ApplyTableParameter) req.getParameters();
        Verify.notNull(parameter.getProject(), "project");
        Verify.notEmpty(parameter.getTables(), "tables");
        Verify.notEmpty(parameter.getTypes(), "types");
        Verify.notEmpty(parameter.getApplyReason(), "applyReason");

        Long projectId = parameter.getProject().getId();
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        Verify.verify(Boolean.FALSE.equals(projectEntity.getArchived()), "Project is archived");
        projectPermissionValidator.checkProjectRole(projectId, ResourceRoleName.all());
        parameter.getProject().setName(projectEntity.getName());
        req.setProjectName(projectEntity.getName());
        parameter.setExpireTime(parameter.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(parameter.getExpireTime()));
        Database database = databaseService.detail(req.getDatabaseId());

        List<ConnectionConfig> connectionConfigs = connectionService.innerListByIds(
                (Arrays.asList(database.getDataSource().getId())));
        Verify.notEmpty(connectionConfigs, "dataSourceId");
        for (ApplyTable table : parameter.getTables()) {
            table.setDatabaseName(database.getName());
            table.setDataSourceName(connectionConfigs.get(0).getName());
            table.setDataSourceId(connectionConfigs.get(0).getId());
        }
        Locale locale = LocaleContextHolder.getLocale();
        String i18nKey = "com.oceanbase.odc.builtin-resource.permission-apply.table.description";
        req.setDescription(I18n.translate(
                i18nKey,
                new Object[] {parameter.getTypes().stream().map(DatabasePermissionType::getLocalizedMessage)
                        .collect(Collectors.joining(","))},
                locale));

    }
}
