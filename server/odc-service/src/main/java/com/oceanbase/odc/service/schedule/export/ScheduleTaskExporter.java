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

import static com.oceanbase.odc.service.exporter.model.ExportConstants.EXPORT_TYPE;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_NAME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.SCHEDULE_EXPORT_TYPE;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.SCHEDULE_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.FileZipper;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.exporter.ExportConfiguration;
import com.oceanbase.odc.service.exporter.Exporter;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataAppender;
import com.oceanbase.odc.service.exporter.model.ExportRowDataMapper;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.schedule.ScheduleExportImportFacade;
import com.oceanbase.odc.service.schedule.export.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.ExportedDataSource;
import com.oceanbase.odc.service.schedule.export.model.ExportedDatabase;
import com.oceanbase.odc.service.schedule.export.model.PartitionPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.export.model.SqlPlanScheduleRowData;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScheduleTaskExporter {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ScheduleExportImportFacade scheduleExportImportFacade;

    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    @Autowired
    private Exporter exporter;

    @Autowired
    private ExportConfiguration exportConfiguration;

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    public static String removeSeparator(String fileName) {
        return fileName.replace(File.separator, "_");
    }

    public ExportedFile exportPartitionPlan(String encryptKey, ExportProperties exportProperties,
            Collection<Long> ids) {
        List<ServiceTaskInstanceEntity> serviceTaskInstanceEntities = serviceTaskRepository.findByFlowInstanceIdIn(ids)
                .stream().filter(s -> s.getTaskType() != TaskType.PRE_CHECK)
                .collect(Collectors.toList());
        Set<Long> taskIds = serviceTaskInstanceEntities.stream().map(ServiceTaskInstanceEntity::getTargetTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<TaskEntity> taskEntities = taskRepository.findByIdIn(taskIds);
        Verify.verify(taskEntities.stream().allMatch(t -> t.getTaskType().equals(TaskType.PARTITION_PLAN)),
                "Id's task type not match");
        ExportProperties properties = generateTypeProperties(ScheduleType.PARTITION_PLAN, exportProperties);
        List<FlowInstanceEntity> flowInstanceEntities = flowInstanceRepository.findByIdIn(ids);
        Map<Long, FlowInstanceEntity> id2FlowInstanceMap = flowInstanceEntities.stream().collect(
                Collectors.toMap(FlowInstanceEntity::getId, f -> f));

        Map<Long, Long> taskId2FlowInstanceIdMap =
                serviceTaskInstanceEntities.stream()
                        .map(s -> new Pair<>(s.getTargetTaskId(), s.getFlowInstanceId()))
                        .collect(Collectors.toSet())
                        .stream()
                        .collect(Collectors.toMap(p -> p.left, p -> p.right));

        try (ExportRowDataAppender exportRowDataAppender = exporter.buildRowDataAppender(properties, encryptKey)) {
            taskEntities.forEach(s -> {
                Long flowInstanceId = taskId2FlowInstanceIdMap.get(s.getId());
                FlowInstanceEntity flowInstanceEntity = id2FlowInstanceMap.get(flowInstanceId);
                doExportPartitionPlan(exportRowDataAppender, flowInstanceEntity, s);
            });
            return exportRowDataAppender.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExportedFile exportSchedule(ScheduleType scheduleType, String encryptKey, ExportProperties exportProperties,
            Collection<Long> ids) {
        Verify.verify(scheduleType == ScheduleType.SQL_PLAN || scheduleType == ScheduleType.DATA_DELETE
                || scheduleType == ScheduleType.DATA_ARCHIVE, "Invalid schedule type");
        List<ScheduleEntity> scheduleEntities = scheduleRepository.findByIdIn(ids);
        Verify.verify(scheduleEntities.stream().allMatch(s -> s.getType().equals(scheduleType)),
                "Ids not match scheduleType");
        ExportProperties properties = generateTypeProperties(scheduleType, exportProperties);
        try (ExportRowDataAppender exportRowDataAppender = exporter.buildRowDataAppender(properties, encryptKey)) {
            scheduleEntities.forEach(s -> export(exportRowDataAppender, s));
            return exportRowDataAppender.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ExportProperties generateTypeProperties(ScheduleType scheduleType,
            ExportProperties exportProperties) {
        // Ensure all archive file in same path
        ExportProperties deepClone = exportProperties.deepClone();
        deepClone.putToMetaData(FILE_NAME, scheduleType.name());
        deepClone.putToMetaData(SCHEDULE_TYPE, scheduleType.name());
        return deepClone;
    }

    public ExportProperties generateExportProperties() {
        ExportProperties exportProperties = new ExportProperties();
        exportProperties.putToMetaData(EXPORT_TYPE, SCHEDULE_EXPORT_TYPE);
        exportProperties.addDefaultMetaData();
        exportProperties.addFilePathProperties(exportConfiguration.getDefaultArchivePath());
        scheduleExportImportFacade.adaptProperties(exportProperties);
        return exportProperties;
    }

    @SneakyThrows
    public ExportedFile mergeToZip(ExportProperties exportProperties, List<ExportedFile> files, String encryptKey) {
        String filePath = exportProperties.acquireFilePath();
        String outputFileName = filePath + File.separator + buildMergedZipName();
        File outputFile = new File(outputFileName);
        outputFile.createNewFile();
        List<File> fs = files.stream().map(ExportedFile::getFile).collect(Collectors.toList());
        FileZipper.mergeToZipFile(fs, outputFile);
        FileZipper.deleteQuietly(fs);
        return new ExportedFile(outputFile, encryptKey);
    }

    private String buildMergedZipName() {
        return removeSeparator(authenticationFacade.currentUserAccountName()) + "_" + LocalDate.now() + ".zip";
    }

    private void export(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        ScheduleType type = scheduleEntity.getType();
        switch (type) {
            case DATA_DELETE:
                doExportDataDelete(appender, scheduleEntity);
                break;
            case DATA_ARCHIVE:
                doExportDataArchive(appender, scheduleEntity);
                break;
            case SQL_PLAN:
                doExportSqlPlan(appender, scheduleEntity);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @SneakyThrows
    private void doExportPartitionPlan(ExportRowDataAppender appender, FlowInstanceEntity flowInstance,
            TaskEntity taskEntity) {
        PartitionPlanConfig originParameter = JsonUtils.fromJson(taskEntity.getParametersJson(),
                PartitionPlanConfig.class);
        PartitionPlanScheduleRowData partitionPlanScheduleRowData =
                ExportRowDataMapper.INSTANCE.toPartitionPlanScheduleRowData(flowInstance, originParameter,
                        getExportedDatabase(
                                originParameter.getDatabaseId()),
                        getProjectName(flowInstance.getProjectId()));
        appender.append(partitionPlanScheduleRowData);
    }

    @SneakyThrows
    private void doExportDataDelete(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        DataDeleteParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DataDeleteParameters.class);
        DataDeleteScheduleRowData dataDeleteRowData =
                ExportRowDataMapper.INSTANCE.toDataDeleteRowData(scheduleEntity, parameters,
                        getExportedDatabase(parameters.getDatabaseId()),
                        getExportedDatabase(parameters.getTargetDatabaseId()),
                        getProjectName(scheduleEntity.getProjectId()));
        appender.append(dataDeleteRowData);
    }

    @SneakyThrows
    private void doExportSqlPlan(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        SqlPlanParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                SqlPlanParameters.class);
        String tempFilePath = appender.getMetaData().acquireFilePath();
        String bucket = "async".concat(File.separator).concat(String.valueOf(scheduleEntity.getCreatorId()));
        addAdditionFile(appender, parameters.getSqlObjectIds(), tempFilePath, bucket);
        addAdditionFile(appender, parameters.getRollbackSqlObjectIds(), tempFilePath, bucket);
        SqlPlanScheduleRowData sqlPlanScheduleRowData =
                ExportRowDataMapper.INSTANCE.toSqlPlanScheduleRowData(scheduleEntity, parameters, getExportedDatabase(
                        parameters.getDatabaseId()), getProjectName(scheduleEntity.getProjectId()));
        appender.append(sqlPlanScheduleRowData);

    }

    private void addAdditionFile(ExportRowDataAppender appender, List<String> sqlObjectIds,
            String tempFilePath, String bucket) throws IOException {
        if (CollectionUtils.isEmpty(sqlObjectIds)) {
            return;
        }
        for (String objectId : sqlObjectIds) {
            File targetFile = new File(tempFilePath + File.separator + removeSeparator(objectId));
            targetFile.createNewFile();
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (OutputStream outputStream = Files.newOutputStream(targetFile.toPath());
                    InputStream inputStream = objectStorageFacade.loadObject(bucket, objectId).getContent();) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            appender.addAdditionFile(removeSeparator(objectId), targetFile);
        }
    }

    @SneakyThrows
    private void doExportDataArchive(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        DataArchiveParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DataArchiveParameters.class);
        DataArchiveScheduleRowData dataDeleteRowData =
                ExportRowDataMapper.INSTANCE.toDataArchiveRowData(scheduleEntity, parameters,
                        getExportedDatabase(parameters.getSourceDatabaseId()),
                        getExportedDatabase(parameters.getTargetDataBaseId()),
                        getProjectName(scheduleEntity.getProjectId()));
        appender.append(dataDeleteRowData);
    }

    private ExportedDatabase getExportedDatabase(Long databaseId) {
        if (databaseId == null) {
            return null;
        }
        Database database = databaseService.detailSkipPermissionCheck(databaseId);
        ConnectionConfig dataSource = database.getDataSource();
        ExportedDataSource exportedDataSource = ExportedDataSource.fromConnectionConfig(dataSource);
        scheduleExportImportFacade.adaptExportDatasource(exportedDataSource);
        return ExportedDatabase.of(exportedDataSource, database.getName());
    }

    private String getProjectName(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findById(projectId).map(ProjectEntity::getName).orElseThrow(NullPointerException::new);
    }
}
