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

import static com.oceanbase.odc.service.archiver.model.ArchiveConstants.ARCHIVE_TYPE;
import static com.oceanbase.odc.service.archiver.model.ArchiveConstants.FILE_NAME;
import static com.oceanbase.odc.service.archiver.model.ArchiveConstants.SCHEDULE_ARCHIVE_TYPE;
import static com.oceanbase.odc.service.common.util.OdcFileUtil.createFileWithDirectories;

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
import com.oceanbase.odc.service.archiver.ArchiveConfiguration;
import com.oceanbase.odc.service.archiver.Archiver;
import com.oceanbase.odc.service.archiver.model.ArchiveProperties;
import com.oceanbase.odc.service.archiver.model.ArchiveRowDataAppender;
import com.oceanbase.odc.service.archiver.model.ArchiveRowDataMapper;
import com.oceanbase.odc.service.archiver.model.ArchivedFile;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.ScheduleArchiveFacade;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.archiverist.model.ArchiveDataSource;
import com.oceanbase.odc.service.schedule.archiverist.model.ArchiveDatabase;
import com.oceanbase.odc.service.schedule.archiverist.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.archiverist.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.SneakyThrows;

@Service
public class ScheduleTaskArchiver {

    @Autowired
    ScheduleService scheduleService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ConnectionService connectionService;

    @Autowired
    DatabaseService databaseService;

    @Autowired
    ScheduleArchiveFacade scheduleArchiveFacade;

    @Autowired
    Archiver archiver;

    @Autowired
    ArchiveConfiguration archiveConfiguration;

    @Autowired
    AuthenticationFacade authenticationFacade;

    public ArchivedFile archive(Collection<Long> scheduleIds) {
        String encryptKey = new BCryptPasswordEncoder().encode(PasswordUtils.random());
        ArchiveProperties archiveProperties = generateArchiveProperties();
        List<ArchivedFile> archivedFiles = new ArrayList<>();

        Map<ScheduleType, List<ScheduleEntity>> type2ScheduleMap =
                scheduleRepository.findByIdIn(scheduleIds).stream().collect(
                        Collectors.groupingBy(ScheduleEntity::getType));

        for (Map.Entry<ScheduleType, List<ScheduleEntity>> entry : type2ScheduleMap.entrySet()) {
            ArchiveProperties deepClone = generateTypeProperties(entry, archiveProperties);
            try (ArchiveRowDataAppender archiveRowDataAppender = archiver.buildRowDataAppender(deepClone, encryptKey)) {
                entry.getValue().forEach(s -> archive(archiveRowDataAppender, s));
                ArchivedFile build = archiveRowDataAppender.build();
                archivedFiles.add(build);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (archivedFiles.size() == 1) {
            return archivedFiles.get(0);
        }
        return mergeToZip(archiveProperties, archivedFiles, encryptKey);
    }

    private ArchiveProperties generateTypeProperties(Entry<ScheduleType, List<ScheduleEntity>> entry,
            ArchiveProperties archiveProperties) {
        // Ensure all archive file in same path
        ArchiveProperties deepClone = JsonUtils.fromJson(JsonUtils.toJson(archiveProperties),
                ArchiveProperties.class);
        deepClone.put(FILE_NAME, entry.getKey().name());
        deepClone.addDefaultTransientProperties(archiveConfiguration.getDefaultArchivePath());
        archiveProperties.put("taskType", entry.getKey());
        return deepClone;
    }

    private ArchiveProperties generateArchiveProperties() {
        ArchiveProperties archiveProperties = new ArchiveProperties();
        archiveProperties.put(ARCHIVE_TYPE, SCHEDULE_ARCHIVE_TYPE);
        archiveProperties.addDefaultMetaData(archiveConfiguration.getDefaultArchivePath());
        scheduleArchiveFacade.adapt(archiveProperties);
        return archiveProperties;
    }

    public ArchivedFile mergeToZip(ArchiveProperties archiveProperties, List<ArchivedFile> files, String encryptKey) {
        String filePath = archiveProperties.acquireFilePath();
        String outputFileName = filePath + File.separator + buildMergedZipName();
        File outputFile = new File(outputFileName);
        createFileWithDirectories(outputFile);
        List<File> fs = files.stream().map(ArchivedFile::toFile).collect(Collectors.toList());
        FileZipper.mergeToZipFile(fs, outputFile);
        return ArchivedFile.fromFile(outputFile, encryptKey);
    }


    public String buildMergedZipName() {
        return authenticationFacade.currentUserAccountName() + "_" + LocalDate.now() + ".zip";
    }

    public void archive(ArchiveRowDataAppender appender, ScheduleEntity scheduleEntity) {
        ScheduleType type = scheduleEntity.getType();
        switch (type) {
            case DATA_DELETE:
                archiveDataDelete(appender, scheduleEntity);
                break;
            case DATA_ARCHIVE:
                archiveDataArchive(appender, scheduleEntity);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    @SneakyThrows
    public void archiveDataDelete(ArchiveRowDataAppender appender, ScheduleEntity scheduleEntity) {
        DataDeleteParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DataDeleteParameters.class);
        DataDeleteScheduleRowData dataDeleteRowData =
                ArchiveRowDataMapper.INSTANCE.toDataDeleteRowData(scheduleEntity, parameters,
                        getArchiveDatabase(parameters.getDatabaseId()),
                        getArchiveDatabase(parameters.getTargetDatabaseId()));
        appender.append(dataDeleteRowData);
    }

    @SneakyThrows
    public void archiveDataArchive(ArchiveRowDataAppender appender, ScheduleEntity scheduleEntity) {
        DataArchiveParameters parameters = JsonUtils.fromJson(scheduleEntity.getJobParametersJson(),
                DataArchiveParameters.class);
        DataArchiveScheduleRowData dataDeleteRowData =
                ArchiveRowDataMapper.INSTANCE.toDataArchiveRowData(scheduleEntity, parameters,
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
