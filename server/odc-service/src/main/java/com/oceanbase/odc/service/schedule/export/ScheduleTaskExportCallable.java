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

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.common.task.RouteLogCallable;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.schedule.export.model.FileExportResponse;
import com.oceanbase.odc.service.schedule.export.model.FileExportStatus;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskExportRequest;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

public class ScheduleTaskExportCallable extends RouteLogCallable<FileExportResponse> {

    public static final String WORK_SPACE = "scheduleTaskExport";
    public static final String LOG_NAME = "export";
    private final ScheduleTaskExportRequest request;
    private final User user;
    private final ScheduleTaskExporter scheduleTaskExporter;
    private final String bucketName;
    private final ObjectStorageFacade objectStorageFacade;

    public ScheduleTaskExportCallable(String taskId,
            ScheduleTaskExportRequest request, User user,
            ScheduleTaskExporter scheduleTaskExporter, String bucketName, ObjectStorageFacade objectStorageFacade) {
        super(WORK_SPACE, taskId, LOG_NAME);
        this.request = request;
        this.user = user;
        this.scheduleTaskExporter = scheduleTaskExporter;
        this.bucketName = bucketName;
        this.objectStorageFacade = objectStorageFacade;

    }

    @Override
    public FileExportResponse doCall() {
        log.info("Start to export schedule, request={}", JsonUtils.toJson(request));
        SecurityContextUtils.setCurrentUser(user);
        try {
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
        } catch (Exception e) {
            log.error("Export schedule failed", e);
            return FileExportResponse.failed(e.getMessage());
        }

    }

    private FileExportResponse mapToFileExportResponse(ExportedFile exportedFile) {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setSecret(exportedFile.getSecret());
        fileExportResponse.setStatus(FileExportStatus.SUCCESS);
        fileExportResponse.setFileName(exportedFile.getFile().getName());
        try {
            objectStorageFacade.createBucketIfNotExists(bucketName);
            ObjectMetadata metadata = objectStorageFacade.putTempObject(bucketName, exportedFile.getFile());
            String downloadUrl = objectStorageFacade.getDownloadUrl(metadata.getBucketName(), metadata.getObjectId());
            fileExportResponse.setDownloadUrl(downloadUrl);
        } finally {
            OdcFileUtil.deleteFiles(exportedFile.getFile());
        }
        return fileExportResponse;
    }
}
