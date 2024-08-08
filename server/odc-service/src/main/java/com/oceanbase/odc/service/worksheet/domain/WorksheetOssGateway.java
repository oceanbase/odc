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
package com.oceanbase.odc.service.worksheet.domain;

import java.io.File;
import java.util.Set;

/**
 * oss interfaces in worksheet
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public interface WorksheetOssGateway {
    String generateUploadUrl(Long projectId);

    void copyTo(String tempObjectId, Path destinationPath);

    String getContent(String objectId);

    void batchDelete(Set<String> objectIds);

    String generateDownloadUrl(String objectId);

    void downloadToFile(String objectName, File toFile);

    String uploadFile(File file, int durationSeconds);
}
