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
package com.oceanbase.odc.service.exporter.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.schedule.export.model.BaseScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.ExportedDatabase;
import com.oceanbase.odc.service.schedule.export.model.ImportDatabaseView;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.PartitionPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.ScheduleNonImportableType;
import com.oceanbase.odc.service.schedule.export.model.ScheduleRowPreviewDto;
import com.oceanbase.odc.service.schedule.export.model.SqlPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

@Mapper
public interface ExportRowDataMapper {

    ExportRowDataMapper INSTANCE = Mappers.getMapper(ExportRowDataMapper.class);

    @Mapping(source = "database", target = "database")
    @Mapping(source = "targetDatabase", target = "targetDatabase")
    @Mapping(target = "triggerConfig", expression = "java(fromJson(scheduleEntity))")
    @Mapping(target = "name", source = "scheduleEntity.name")
    @Mapping(source = "scheduleEntity.id", target = "originScheduleId")
    @Mapping(source = "projectName", target = "originProjectName")
    DataDeleteScheduleRowData toDataDeleteRowData(ScheduleEntity scheduleEntity, DataDeleteParameters parameters,
            ExportedDatabase database,
            ExportedDatabase targetDatabase, String projectName);

    @Mapping(source = "sourceDatabaseId", target = "databaseId")
    @Mapping(source = "targetDatabaseId", target = "targetDatabaseId")
    DataDeleteParameters toDataDeleteParameters(Long sourceDatabaseId, Long targetDatabaseId,
            DataDeleteScheduleRowData dataDeleteScheduleRowData);

    @Mapping(target = "triggerConfig", expression = "java(fromJson(scheduleEntity))")
    @Mapping(target = "name", source = "scheduleEntity.name")
    @Mapping(source = "scheduleEntity.id", target = "originScheduleId")
    @Mapping(source = "projectName", target = "originProjectName")
    @Mapping(source = "targetDatabase", target = "targetDatabase")
    DataArchiveScheduleRowData toDataArchiveRowData(ScheduleEntity scheduleEntity, DataArchiveParameters parameters,
            ExportedDatabase database,
            ExportedDatabase targetDatabase, String projectName);

    @Mapping(source = "sourceDatabaseId", target = "sourceDatabaseId")
    @Mapping(source = "targetDataBaseId", target = "targetDataBaseId")
    DataArchiveParameters toDataArchiveParameters(Long sourceDatabaseId, Long targetDataBaseId,
            DataArchiveScheduleRowData rowData);


    @Mapping(target = "triggerConfig", expression = "java(fromJson(scheduleEntity))")
    @Mapping(target = "name", source = "scheduleEntity.name")
    @Mapping(source = "targetDatabase", target = "targetDatabase")
    @Mapping(source = "scheduleEntity.id", target = "originScheduleId")
    @Mapping(source = "projectName", target = "originProjectName")
    SqlPlanScheduleRowData toSqlPlanScheduleRowData(ScheduleEntity scheduleEntity, SqlPlanParameters parameters,
            ExportedDatabase targetDatabase, String projectName);

    @Mapping(source = "databasesId", target = "databaseId")
    SqlPlanParameters toSqlPlanParameters(Long databasesId, SqlPlanScheduleRowData sqlPlanScheduleRowData);

    @Mapping(source = "scheduleEntity.id", target = "originScheduleId")
    @Mapping(source = "projectName", target = "originProjectName")
    PartitionPlanScheduleRowData toPartitionPlanScheduleRowData(ScheduleEntity scheduleEntity,
            PartitionPlanConfig partitionPlanConfig, ExportedDatabase database, String projectName);


    PartitionPlanConfig toPartitionPlanConfig(Long databaseId,
            PartitionPlanScheduleRowData partitionPlanScheduleRowData);


    @Mapping(target = "datasourceView",
            expression = "java(toImportDatabaseView(scheduleRowPreviewDto.getDatabase(),matchedDatasourceName))")
    @Mapping(target = "targetDatasourceView",
            expression = "java(toImportDatabaseView(scheduleRowPreviewDto.getTargetDatabase(),matchedTargetDatasourceName))")
    @Mapping(target = "exportRowId", source = "scheduleRowPreviewDto.rowId")
    ImportScheduleTaskView toImportScheduleTaskView(ScheduleRowPreviewDto scheduleRowPreviewDto, Boolean importable,
            ScheduleNonImportableType nonImportableType, String matchedDatasourceName,
            String matchedTargetDatasourceName);

    @Mapping(target = "cloudProvider", source = "exportedDatabase.exportedDataSource.cloudProvider")
    @Mapping(target = "type", source = "exportedDatabase.exportedDataSource.type")
    @Mapping(target = "instanceId", source = "exportedDatabase.exportedDataSource.instanceId")
    @Mapping(target = "tenantId", source = "exportedDatabase.exportedDataSource.tenantId")
    @Mapping(target = "host", source = "exportedDatabase.exportedDataSource.host")
    @Mapping(target = "port", source = "exportedDatabase.exportedDataSource.port")
    @Mapping(target = "username", source = "exportedDatabase.exportedDataSource.username")
    @Mapping(target = "name", source = "exportedDatabase.exportedDataSource.name")
    ImportDatabaseView toImportDatabaseView(ExportedDatabase exportedDatabase, String matchedDatasourceName);


    AlterScheduleParameters toAlterScheduleParameters(OperationType operationType,
            BaseScheduleRowData baseScheduleRowData, ScheduleTaskParameters scheduleTaskParameters);

    CreateFlowInstanceReq toCreateFlowInstanceReq(Long projectId, Long databaseId, TaskType taskType,
            TaskParameters parameters,
            String description);

    default TriggerConfig fromJson(ScheduleEntity scheduleEntity) {
        return JsonUtils.fromJson(scheduleEntity.getTriggerConfigJson(), TriggerConfig.class);
    }
}
