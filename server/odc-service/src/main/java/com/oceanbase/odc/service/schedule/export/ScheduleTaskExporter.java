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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.util.FileZipper;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
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

    public ExportedFile export(Collection<Long> scheduleIds) {
        Verify.notEmpty(scheduleIds, "scheduleIds");
        String encryptKey = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        ExportProperties exportProperties = generateArchiveProperties();
        List<ExportedFile> exportedFiles = new ArrayList<>();

        Map<ScheduleType, List<ScheduleEntity>> type2ScheduleMap =
                scheduleRepository.findByIdIn(scheduleIds).stream().collect(
                        Collectors.groupingBy(ScheduleEntity::getType));

        for (Map.Entry<ScheduleType, List<ScheduleEntity>> entry : type2ScheduleMap.entrySet()) {
            ExportProperties properties = generateTypeProperties(entry, exportProperties);
            try (ExportRowDataAppender exportRowDataAppender = exporter.buildRowDataAppender(properties, encryptKey)) {
                entry.getValue().forEach(s -> export(exportRowDataAppender, s));
                ExportedFile build = exportRowDataAppender.build();
                exportedFiles.add(build);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (CollectionUtils.isEmpty(exportedFiles)) {
            throw new RuntimeException("Export files is empty");
        }

        if (exportedFiles.size() == 1) {
            return exportedFiles.get(0);
        }

        return mergeToZip(exportProperties, exportedFiles, encryptKey);
    }

    private ExportProperties generateTypeProperties(Entry<ScheduleType, List<ScheduleEntity>> entry,
            ExportProperties exportProperties) {
        // Ensure all archive file in same path
        ExportProperties deepClone = exportProperties.deepClone();
        deepClone.putToMetaData(FILE_NAME, entry.getKey().name());
        deepClone.putToMetaData(SCHEDULE_TYPE, entry.getKey().name());
        return deepClone;
    }

    private ExportProperties generateArchiveProperties() {
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


    public String buildMergedZipName() {
        return removeSeparator(authenticationFacade.currentUserAccountName()) + "_" + LocalDate.now() + ".zip";
    }

    private String removeSeparator(String fileName) {
        return fileName.replace(File.separator, "_");
    }

    public void export(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        ScheduleType type = scheduleEntity.getType();
        switch (type) {
            case DATA_DELETE:
                exportDataDelete(appender, scheduleEntity);
                break;
            case DATA_ARCHIVE:
                exportDataArchive(appender, scheduleEntity);
                break;
            case SQL_PLAN:
                exportSqlPlan(appender, scheduleEntity);
                break;
            case PARTITION_PLAN:
                exportPartitionPlan(appender, scheduleEntity);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @SneakyThrows
    public void exportDataDelete(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
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
    public void exportSqlPlan(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
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

    @SneakyThrows
    public void exportPartitionPlan(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        PartitionPlanConfig parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                PartitionPlanConfig.class);
        Optional<TaskEntity> taskOption = taskRepository.findById(parameters.getTaskId());
        Verify.verify(taskOption.isPresent(), "Can't find task entity");
        TaskEntity taskEntity = taskOption.get();
        PartitionPlanConfig originParameter = JsonUtils.fromJson(taskEntity.getParametersJson(),
                PartitionPlanConfig.class);
        PartitionPlanScheduleRowData partitionPlanScheduleRowData =
                ExportRowDataMapper.INSTANCE.toPartitionPlanScheduleRowData(scheduleEntity, originParameter,
                        getExportedDatabase(
                                originParameter.getDatabaseId()),
                        getProjectName(scheduleEntity.getProjectId()));
        appender.append(partitionPlanScheduleRowData);

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
    public void exportDataArchive(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
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
