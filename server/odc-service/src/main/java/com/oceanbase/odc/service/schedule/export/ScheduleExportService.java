/*
 * Copyright (c) 2025 OceanBase.
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
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.core.shared.PreConditions;
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
    public static final String ASYNC_TASK_BASE_BUCKET = "async";
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

    public ExportedFile export(ScheduleTaskExportRequest request) {
        ExportProperties properties = scheduleTaskExporter.generateExportProperties();
        String encryptKey = new BCryptPasswordEncoder().encode(PasswordUtils.random());

        if (request.getScheduleType().equals(ScheduleType.PARTITION_PLAN)) {
            return scheduleTaskExporter.exportPartitionPlan(encryptKey, properties,
                    request.getIds());
        }
        return scheduleTaskExporter.exportSchedule(request.getScheduleType(), encryptKey, properties, request.getIds());
    }

    private FileExportResponse mapToFileExportResponse(ExportedFile exportedFile) {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setSecret(exportedFile.getSecret());
        ObjectMetadata objectMetadata = saveToObjectStorage(exportedFile);
        fileExportResponse.setBucket(objectMetadata.getBucketName());
        fileExportResponse.setFileId(objectMetadata.getObjectId());
        return fileExportResponse;
    }


    private ObjectMetadata saveToObjectStorage(ExportedFile exportedFile) {
        File file = exportedFile.getFile();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String bucketName = getPersonalBucketName(authenticationFacade.currentUserIdStr());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            return objectStorageFacade.putTempObject(bucketName, file.getName(),
                    authenticationFacade.currentUserId(), file.length(), fileInputStream);
        } catch (IOException e) {
            log.info("Upload file to object storage failed.", e);
            throw new RuntimeException(e);
        }
    }

}
