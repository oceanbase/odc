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
package com.oceanbase.odc.service.worksheet.service;

import java.io.File;
import java.util.List;

import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheetsPreProcessor;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public interface WorksheetService {
    GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, Path path);

    WorksheetMetaResp createWorksheet(Long projectId, Path createPath, String objectId, Long size);

    WorksheetResp getWorksheetDetails(Long projectId, Path path);

    List<WorksheetMetaResp> listWorksheets(Long projectId, Path path, Integer depth, String nameLike);

    BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId,
            BatchCreateWorksheetsPreProcessor batchCreateWorksheetsPreProcessor);

    BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<Path> paths);

    List<WorksheetMetaResp> renameWorksheet(Long projectId, Path path, Path destinationPath);

    List<WorksheetMetaResp> editWorksheet(Long projectId, Path path, String objectId, Long size,
            Long readVersion);

    String generateDownloadUrl(Long projectId, Path path);

    void downloadPathsToDirectory(Long projectId, List<Path> paths, Path commParentPath,
            File destinationDirectory);
}
