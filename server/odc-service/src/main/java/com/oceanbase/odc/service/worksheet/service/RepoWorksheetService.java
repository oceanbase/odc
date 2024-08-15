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
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.domain.WorksheetObjectStorageGateway;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
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
    @Resource
    private WorksheetObjectStorageGateway objectStorageGateway;

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, Path path) {
        String bucket = WorksheetUtil.getBucketNameOfRepos(projectId);
        String objectId = WorksheetUtil.getObjectIdOfRepos(path);
        String uploadUrl = objectStorageGateway.generateUploadUrl(bucket, objectId);
        return GenerateWorksheetUploadUrlResp.builder().uploadUrl(uploadUrl).objectId(objectId).build();
    }

    @Override
    public Worksheet createWorksheet(Long projectId, Path createPath, String objectId) {
        return null;
    }

    @Override
    public Worksheet getWorksheetDetails(Long projectId, Path path) {

        return null;
    }

    @Override
    public List<Worksheet> listWorksheets(Long projectId, Path path, Integer depth, String nameLike) {

        return new ArrayList<>();
    }

    @Override
    public BatchOperateWorksheetsResult batchUploadWorksheets(Long projectId,
            BatchCreateWorksheets batchCreateWorksheets) {
        return null;
    }

    @Override
    public BatchOperateWorksheetsResult batchDeleteWorksheets(Long projectId, Set<Path> paths) {
        return new BatchOperateWorksheetsResult();
    }

    @Override
    public List<Worksheet> renameWorksheet(Long projectId, Path path, Path destinationPath) {
        return Collections.emptyList();
    }

    @Override
    public List<Worksheet> editWorksheet(Long projectId, Path path, Path destinationPath, String objectId,
            Long readVersion) {
        return Collections.emptyList();
    }

    @Override
    public String batchDownloadWorksheets(Long projectId, Set<String> paths) {
        return "";
    }

    @Override
    public String getDownloadUrl(Long projectId, Path path) {
        return "";
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, Set<Path> paths, Path commParentPath,
            File destinationDirectory) {

    }
}
