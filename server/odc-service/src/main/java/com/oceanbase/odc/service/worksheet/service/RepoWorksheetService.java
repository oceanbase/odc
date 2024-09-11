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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheetsPreProcessor;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

/**
 * the handle of worksheets in /Repos/
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Service
public class RepoWorksheetService implements WorksheetService {
    @Autowired
    private ObjectStorageClient objectStorageClient;

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, String groupId, Path path) {
        String objectId = WorksheetUtil.getObjectIdOfRepos(path);
        String uploadUrl = objectStorageClient.generateUploadUrl(objectId).toString();
        return GenerateWorksheetUploadUrlResp.builder().uploadUrl(uploadUrl).objectId(objectId).build();
    }

    @Override
    public WorksheetMetaResp createWorksheet(Long projectId, String groupId, Path createPath, String objectId,
            Long size) {
        return null;
    }

    @Override
    public WorksheetResp getWorksheetDetails(Long projectId, String groupId, Path path) {

        return null;
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, String groupId, Path path, Integer depth,
            String nameLike) {

        return new ArrayList<>();
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId,
            String groupId, BatchCreateWorksheetsPreProcessor batchCreateWorksheetsPreProcessor) {
        return null;
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, String groupId, List<Path> paths) {
        return new BatchOperateWorksheetsResp();
    }

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, String groupId, Path path, Path destinationPath) {
        return Collections.emptyList();
    }

    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, String groupId, Path path, String objectId, Long size,
            Long readVersion) {
        return Collections.emptyList();
    }

    @Override
    public String generateDownloadUrl(Long projectId, String groupId, Path path) {
        return "";
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, String groupId, List<Path> paths, Path commParentPath,
            File destinationDirectory) {

    }
}
