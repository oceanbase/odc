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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.schedule.export.model.FileExportResponse;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskExportRequest;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

@Service
public class ScheduleExportService {
    public static final String ASYNC_TASK_BASE_BUCKET = "scheduleexport";
    private static final Logger log = LoggerFactory.getLogger(ScheduleExportService.class);

    @Autowired
    private ScheduleTaskExporter scheduleTaskExporter;

    @Autowired
    private ObjectStorageFacade objectStorageFacade;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private static String getPersonalBucketName(String userIdStr) {
        PreConditions.notEmpty(userIdStr, "userIdStr");
        return ASYNC_TASK_BASE_BUCKET.concat(File.separator).concat(userIdStr);
    }

    public FileExportResponse export(ScheduleTaskExportRequest request) {
        ExportProperties properties = scheduleTaskExporter.generateExportProperties();
        String encryptKey = new BCryptPasswordEncoder().encode(PasswordUtils.random());

        ExportedFile exportedFile = null;
        if (request.getScheduleType().equals(ScheduleType.PARTITION_PLAN)) {
            exportedFile = scheduleTaskExporter.exportPartitionPlan(encryptKey, properties,
                    request.getIds());
        } else {
            exportedFile = scheduleTaskExporter.exportSchedule(request.getScheduleType(), encryptKey,
                    properties, request.getIds());
        }
        return mapToFileExportResponse(exportedFile);
    }

    private FileExportResponse mapToFileExportResponse(ExportedFile exportedFile) {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setSecret(exportedFile.getSecret());
        try {
            String bucketName = getPersonalBucketName(authenticationFacade.currentUserIdStr());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            ObjectMetadata metadata = objectStorageFacade.putTempObject(bucketName, exportedFile.getFile().getName(),
                    exportedFile.getFile().length(),
                    Files.newInputStream(exportedFile.getFile().toPath()));
            String downloadUrl = objectStorageFacade.getDownloadUrl(metadata.getBucketName(), metadata.getObjectId());
            fileExportResponse.setDownloadUrl(downloadUrl);
        } catch (IOException e) {
            log.info("Get download url failed", e);
            throw new RuntimeException(e);
        } finally {
            OdcFileUtil.deleteFiles(exportedFile.getFile());
        }
        return fileExportResponse;
    }

}
