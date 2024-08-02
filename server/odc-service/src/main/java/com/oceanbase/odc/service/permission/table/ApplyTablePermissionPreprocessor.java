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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
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

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ApplyTableParameter parameter = (ApplyTableParameter) req.getParameters();
        Verify.notNull(parameter.getProject(), "project");
        Verify.notEmpty(parameter.getTables(), "tables");
        Verify.notEmpty(parameter.getTypes(), "types");
        Verify.notEmpty(parameter.getApplyReason(), "applyReason");
        // Check project permission and fill in project name
        Long projectId = parameter.getProject().getId();
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        Verify.verify(Boolean.FALSE.equals(projectEntity.getArchived()), "Project is archived");
        projectPermissionValidator.checkProjectRole(projectId, ResourceRoleName.all());
        parameter.getProject().setName(projectEntity.getName());
        // Check table permission and fill in table related information
        List<ApplyTable> tables = parameter.getTables();
        Set<Long> tableIds = tables.stream().map(ApplyTable::getTableId).collect(Collectors.toSet());
        Map<Long, DBObjectEntity> id2TableEntity = dbObjectRepository.findByIdIn(tableIds).stream()
                .collect(Collectors.toMap(DBObjectEntity::getId, t -> t, (t1, t2) -> t1));
        Set<Long> databaseIds =
                id2TableEntity.values().stream().map(DBObjectEntity::getDatabaseId).collect(Collectors.toSet());
        Map<Long, Database> id2Database = databaseService.listDatabasesByIds(databaseIds).stream()
                .collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        Set<Long> dataSourceIds =
                id2Database.values().stream().map(d -> d.getDataSource().getId()).collect(Collectors.toSet());
        Map<Long, ConnectionConfig> id2DataSource = connectionService.innerListByIds(dataSourceIds).stream()
                .collect(Collectors.toMap(ConnectionConfig::getId, d -> d, (d1, d2) -> d1));
        for (ApplyTable table : tables) {
            DBObjectEntity tableEntity = id2TableEntity.get(table.getTableId());
            if (tableEntity == null) {
                throw new NotFoundException(ResourceType.ODC_TABLE, "id", table.getTableId());
            }
            table.setTableName(tableEntity.getName());
            Database database = id2Database.get(tableEntity.getDatabaseId());
            if (database == null) {
                throw new NotFoundException(ResourceType.ODC_DATABASE, "id", tableEntity.getDatabaseId());
            }
            if (database.getProject() == null || !Objects.equals(database.getProject().getId(), projectId)) {
                throw new AccessDeniedException();
            }
            table.setDatabaseId(database.getId());
            table.setDatabaseName(database.getName());
            ConnectionConfig dataSource = id2DataSource.get(database.getDataSource().getId());
            if (dataSource == null) {
                throw new NotFoundException(ResourceType.ODC_CONNECTION, "id", database.getDataSource().getId());
            }
            table.setDataSourceId(dataSource.getId());
            table.setDataSourceName(dataSource.getName());
        }
        parameter.setExpireTime(parameter.getExpireTime() == null ? TimeUtils.getMySQLMaxDatetime()
                : TimeUtils.getEndOfDay(parameter.getExpireTime()));
        // Fill in other parameters
        req.setProjectId(projectId);
        req.setProjectName(projectEntity.getName());
        Locale locale = LocaleContextHolder.getLocale();
        String i18nKey = "com.oceanbase.odc.builtin-resource.permission-apply.table.description";
        req.setDescription(I18n.translate(
                i18nKey,
                new Object[] {parameter.getTypes().stream().map(DatabasePermissionType::getLocalizedMessage)
                        .collect(Collectors.joining(","))},
                locale));
    }

}
