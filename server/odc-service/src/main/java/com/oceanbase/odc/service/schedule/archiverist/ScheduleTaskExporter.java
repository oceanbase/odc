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
package com.oceanbase.odc.service.schedule.archiverist;

import static com.oceanbase.odc.service.exporter.model.ExportConstants.ARCHIVE_TYPE;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.FILE_NAME;
import static com.oceanbase.odc.service.exporter.model.ExportConstants.SCHEDULE_ARCHIVE_TYPE;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.util.FileZipper;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
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
import com.oceanbase.odc.service.schedule.ScheduleExportFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.archiverist.model.ArchiveDataSource;
import com.oceanbase.odc.service.schedule.archiverist.model.ArchiveDatabase;
import com.oceanbase.odc.service.schedule.archiverist.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.archiverist.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.SneakyThrows;

@Service
public class ScheduleTaskExporter {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ConnectionService connectionService;

    @Autowired
    DatabaseService databaseService;

    @Autowired
    ScheduleExportFacade scheduleExportFacade;

    @Autowired
    Exporter exporter;

    @Autowired
    ExportConfiguration exportConfiguration;

    @Autowired
    AuthenticationFacade authenticationFacade;

    public ExportedFile export(Collection<Long> scheduleIds) {
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

        if (exportedFiles.size() == 1) {
            return exportedFiles.get(0);
        }
        return mergeToZip(exportProperties, exportedFiles, encryptKey);
    }

    private ExportProperties generateTypeProperties(Entry<ScheduleType, List<ScheduleEntity>> entry,
            ExportProperties exportProperties) {
        // Ensure all archive file in same path
        ExportProperties deepClone = JsonUtils.fromJson(JsonUtils.toJson(exportProperties),
                ExportProperties.class);
        deepClone.put(FILE_NAME, entry.getKey().name());
        deepClone.addFilePathProperties(exportConfiguration.getDefaultArchivePath());
        exportProperties.put("taskType", entry.getKey());
        return deepClone;
    }

    private ExportProperties generateArchiveProperties() {
        ExportProperties exportProperties = new ExportProperties();
        exportProperties.put(ARCHIVE_TYPE, SCHEDULE_ARCHIVE_TYPE);
        exportProperties.addDefaultMetaData();
        exportProperties.addFilePathProperties(exportConfiguration.getDefaultArchivePath());
        scheduleExportFacade.adapt(exportProperties);
        return exportProperties;
    }

    public ExportedFile mergeToZip(ExportProperties exportProperties, List<ExportedFile> files, String encryptKey) {
        String filePath = exportProperties.acquireFilePath();
        String outputFileName = filePath + File.separator + buildMergedZipName();
        File outputFile = new File(outputFileName);
        List<File> fs = files.stream().map(ExportedFile::getFile).collect(Collectors.toList());
        FileZipper.mergeToZipFile(fs, outputFile);
        FileZipper.deleteQuietly(fs);
        return new ExportedFile(outputFile, encryptKey);
    }


    public String buildMergedZipName() {
        return authenticationFacade.currentUserAccountName() + "_" + LocalDate.now() + ".zip";
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
                        getArchiveDatabase(parameters.getDatabaseId()),
                        getArchiveDatabase(parameters.getTargetDatabaseId()));
        appender.append(dataDeleteRowData);
    }

    @SneakyThrows
    public void exportDataArchive(ExportRowDataAppender appender, ScheduleEntity scheduleEntity) {
        DataArchiveParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DataArchiveParameters.class);
        DataArchiveScheduleRowData dataDeleteRowData =
                ExportRowDataMapper.INSTANCE.toDataArchiveRowData(scheduleEntity, parameters,
                        getArchiveDatabase(parameters.getSourceDatabaseId()),
                        getArchiveDatabase(parameters.getTargetDataBaseId()));
        appender.append(dataDeleteRowData);
    }

    private ArchiveDatabase getArchiveDatabase(Long databaseId) {
        if (databaseId == null) {
            return null;
        }
        Database database = databaseService.detailSkipPermissionCheck(databaseId);
        ConnectionConfig dataSource = database.getDataSource();
        ArchiveDataSource archiveDataSource = ArchiveDataSource.fromConnectionConfig(dataSource);
        return ArchiveDatabase.of(archiveDataSource, database.getName());
    }
}
