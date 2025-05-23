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
package com.oceanbase.odc.service.schedule.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionEntity_;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.exporter.ImportService;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.ScheduleExportImportFacade;
import com.oceanbase.odc.service.schedule.export.exception.DatabaseNonExistException;
import com.oceanbase.odc.service.schedule.export.model.ExportedDataSource;
import com.oceanbase.odc.service.schedule.export.model.ExportedDatabase;
import com.oceanbase.odc.service.schedule.export.model.ImportDatabaseView;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ScheduleNonImportableType;
import com.oceanbase.odc.service.schedule.export.model.ScheduleRowPreviewDto;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Data;

public class DefaultScheduleFacade implements ScheduleExportImportFacade {

    private final ConnectionConfigRepository connectionConfigRepository;

    private final DatabaseRepository databaseRepository;

    private final AuthenticationFacade authenticationFacade;
    private final ImportService importService;

    public DefaultScheduleFacade(ConnectionConfigRepository connectionConfigRepository,
            DatabaseRepository databaseRepository, AuthenticationFacade authenticationFacade,
            ImportService importService) {
        this.connectionConfigRepository = connectionConfigRepository;
        this.databaseRepository = databaseRepository;
        this.authenticationFacade = authenticationFacade;
        this.importService = importService;
    }

    @Override
    public Set<ScheduleType> supportedScheduleTypes() {
        return ImmutableSet.of(ScheduleType.DATA_DELETE, ScheduleType.DATA_ARCHIVE, ScheduleType.SQL_PLAN,
                ScheduleType.PARTITION_PLAN);
    }

    @Override
    public void adaptProperties(ExportProperties exportProperties) {

    }

    @Override
    public void adaptExportDatasource(ExportedDataSource exportedDataSource) {

    }

    @Override
    public List<ImportScheduleTaskView> preview(ScheduleType scheduleType, @Nullable Long projectId,
            ExportProperties exportProperties, List<ScheduleRowPreviewDto> dtos) {
        List<ImportScheduleTaskView> result = new ArrayList<>();
        for (ScheduleRowPreviewDto dto : dtos) {
            if (importService.imported(exportProperties, dto.getRowId())) {
                result.add(buildFailedTaskView(dto, ScheduleNonImportableType.IMPORTED));
                continue;
            }

            if (!dto.getType().equals(scheduleType)) {
                result.add(buildFailedTaskView(dto, ScheduleNonImportableType.TYPE_NOT_MATCH));
                continue;
            }

            ImportScheduleTaskView importScheduleTaskView = buildImportableTaskView(dto);
            DatabaseHolder matchedDatabase = getMatchedDatabase(projectId, dto.getDatabase());
            if (matchedDatabase == null) {
                result.add(buildFailedTaskView(dto, ScheduleNonImportableType.DATASOURCE_NON_EXIST));
                continue;
            }
            ImportDatabaseView databaseView = importScheduleTaskView.getDatabaseView();
            databaseView.setMatchedDatasourceName(matchedDatabase.getDatasourceName());

            if (dto.getTargetDatabase() != null) {
                DatabaseHolder matchedTargetDatabase = getMatchedDatabase(projectId, dto.getTargetDatabase());
                if (matchedTargetDatabase == null) {
                    result.add(buildFailedTaskView(dto, ScheduleNonImportableType.DATASOURCE_NON_EXIST));
                    continue;
                }
                ImportDatabaseView targetDatabaseView = importScheduleTaskView.getTargetDatabaseView();
                Verify.notNull(targetDatabaseView, "targetDatabaseView");
                targetDatabaseView.setMatchedDatasourceName(matchedTargetDatabase.getDatasourceName());
            }
            result.add(importScheduleTaskView);
        }
        return result;
    }

    @Override
    public void processParamsBeforeImport(ScheduleChangeParams scheduleChangeParams, JsonNode rowData) {

    }

    @Override
    public Long getOrCreateDatabaseId(Long projectId, ExportedDatabase exportedDatabase)
            throws DatabaseNonExistException {
        DatabaseHolder matchedDatabase = getMatchedDatabase(projectId, exportedDatabase);
        if (matchedDatabase == null) {
            throw new DatabaseNonExistException();
        }
        return matchedDatabase.getDatabase().getId();
    }

    @Nullable
    private DatabaseHolder getMatchedDatabase(Long projectId, ExportedDatabase exportedDatabase) {
        ExportedDataSource exportedDataSource = exportedDatabase.getExportedDataSource();
        Specification<ConnectionEntity> specification = Specification.where(
                OdcJpaRepository.eq(ConnectionEntity_.host, exportedDataSource.getHost()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.port, exportedDataSource.getPort()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.username, exportedDataSource.getUsername()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.clusterName, exportedDataSource.getInstanceId()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.tenantName, exportedDataSource.getTenantId()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.type, exportedDataSource.getType()))
                .and(OdcJpaRepository.eq(ConnectionEntity_.organizationId,
                        authenticationFacade.currentOrganizationId()));

        List<ConnectionEntity> matchedConnections = connectionConfigRepository.findAll(specification);
        if (CollectionUtils.isEmpty(matchedConnections)) {
            return null;
        }
        List<Long> matchedConnectionIds =
                matchedConnections.stream().map(ConnectionEntity::getId).collect(Collectors.toList());
        List<DatabaseEntity> databases =
                databaseRepository.findByConnectionIdInAndNameAndExisted(matchedConnectionIds,
                        exportedDatabase.getDatabaseName(), true);
        if (CollectionUtils.isEmpty(databases)) {
            return null;
        }
        DatabaseEntity databaseEntity = databases.stream().filter(d -> Objects.equals(d.getProjectId(), projectId))
                .findFirst()
                .orElse(null);
        if (databaseEntity == null) {
            return null;
        }
        ConnectionEntity connectionEntity = matchedConnections.stream().filter(
                c -> c.getId().equals(databaseEntity.getConnectionId())).findFirst()
                .orElseThrow(NullPointerException::new);
        return new DatabaseHolder(databaseEntity, connectionEntity.getName());
    }


    private ImportScheduleTaskView buildFailedTaskView(ScheduleRowPreviewDto scheduleRowPreviewDto,
            ScheduleNonImportableType nonImportableType) {
        return ExportRowDataMapper.INSTANCE.toImportScheduleTaskView(scheduleRowPreviewDto, false, nonImportableType,
                null, null);
    }

    private ImportScheduleTaskView buildImportableTaskView(ScheduleRowPreviewDto scheduleRowPreviewDto) {
        return ExportRowDataMapper.INSTANCE.toImportScheduleTaskView(scheduleRowPreviewDto, true, null,
                null, null);
    }

    @Data
    @AllArgsConstructor
    private static class DatabaseHolder {
        private DatabaseEntity database;
        private String datasourceName;
    }
}
