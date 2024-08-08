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
import java.util.Optional;
import java.util.Set;

import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public interface WorksheetService {

    Worksheet createWorksheet(Long projectId, Path createPath, String objectKey);

    Worksheet getWorksheetDetails(Long projectId, Path path);

    List<Worksheet> listWorksheets(Long projectId, Path path);

    List<Worksheet> searchWorksheets(Long projectId, String nameLike, int limit);

    BatchOperateWorksheetsResult batchUploadWorksheets(Long projectId, BatchCreateWorksheets batchCreateWorksheets);

    BatchOperateWorksheetsResult batchDeleteWorksheets(Long projectId, Set<Path> paths);

    List<Worksheet> renameWorksheet(Long projectId, Path path, Path destinationPath);

    List<Worksheet> editWorksheet(Long projectId, Path path, Path destinationPath, String objectKey, Long readVersion);

    String batchDownloadWorksheets(Long projectId, Set<String> paths);

    String getDownloadUrl(Long projectId, Path path);

    void downloadPathsToDirectory(Long projectId, Set<Path> paths, Optional<Path> commParentPath,
            File destinationDirectory);
}
