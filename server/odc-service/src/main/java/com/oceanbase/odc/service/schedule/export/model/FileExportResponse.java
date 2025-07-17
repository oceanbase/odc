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
package com.oceanbase.odc.service.schedule.export.model;

import lombok.Data;
import lombok.ToString.Exclude;

@Data
public class FileExportResponse {

    private String taskId;

    private FileExportStatus status;

    private String fileName;

    private String downloadUrl;

    @Exclude
    private String secret;

    private String failedReason;

    public static FileExportResponse exporting() {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setStatus(FileExportStatus.EXPORTING);
        return fileExportResponse;
    }

    public static FileExportResponse failed(String failedReason) {
        FileExportResponse fileExportResponse = new FileExportResponse();
        fileExportResponse.setStatus(FileExportStatus.FAILED);
        fileExportResponse.setFailedReason(failedReason);
        return fileExportResponse;
    }
}
