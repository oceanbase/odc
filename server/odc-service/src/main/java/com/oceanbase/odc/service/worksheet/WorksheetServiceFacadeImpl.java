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
package com.oceanbase.odc.service.worksheet;

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectTagging;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstant;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.DivideBatchOperateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.WorkSheetsSearch;
import com.oceanbase.odc.service.worksheet.factory.WorksheetServiceFactory;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.BatchUploadWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlReq;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.ListWorksheetsReq;
import com.oceanbase.odc.service.worksheet.model.UpdateWorksheetReq;
import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
import com.oceanbase.odc.service.worksheet.service.WorksheetService;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

/**
 *
 * @author keyang
 * @date 2024/7/31
 * @since 4.3.2
 */
@Service
public class WorksheetServiceFacadeImpl implements WorksheetServiceFacade {
    private static final Logger log = LoggerFactory.getLogger(WorksheetServiceFacadeImpl.class);
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    private ObjectStorageClient objectStorageClient;
    @Autowired
    private WorksheetServiceFactory worksheetServiceFactory;

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, GenerateWorksheetUploadUrlReq req) {
        Path path = new Path(req.getPath());
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                path.getLocation());
        return projectFileService.generateUploadUrl(projectId, path);
    }


    @Override
    public WorksheetMetaResp createWorksheet(Long projectId, String pathStr, String objectId, Long totalLength) {
        Path createPath = new Path(pathStr);
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                createPath.getLocation());
        return projectFileService.createWorksheet(projectId, createPath, objectId, totalLength);
    }


    @Override
    public WorksheetResp getWorksheetDetail(Long projectId, String pathStr) {
        Path path = new Path(pathStr);
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                path.getLocation());
        return projectFileService.getWorksheetDetails(projectId, path);
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, ListWorksheetsReq req) {
        Integer depth = req.getDepth() == null ? 1 : req.getDepth();
        Path path = null;
        if (StringUtils.isNotBlank(req.getPath())) {
            path = new Path(req.getPath());
        }
        WorkSheetsSearch workSheetsSearch = new WorkSheetsSearch(req.getNameLike());
        if (path == null || path.getLocation() == WorksheetLocation.REPOS) {
            workSheetsSearch.addAll(worksheetServiceFactory.getProjectFileService(WorksheetLocation.REPOS)
                    .listWorksheets(projectId, path, depth, req.getNameLike()));
        }
        if (path == null || path.getLocation() == WorksheetLocation.WORKSHEETS) {
            workSheetsSearch.addAll(worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS)
                    .listWorksheets(projectId, path, depth, req.getNameLike()));
        }
        return workSheetsSearch.searchByNameLike(PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT);
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId, BatchUploadWorksheetsReq req) {
        BatchCreateWorksheets batchCreateWorksheets = new BatchCreateWorksheets(req);
        Path parentPath = batchCreateWorksheets.getParentPath();
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                parentPath.getLocation());
        return projectFileService.batchUploadWorksheets(projectId,
                batchCreateWorksheets);
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<String> paths) {
        DivideBatchOperateWorksheets divideBatchOperateWorksheets = new DivideBatchOperateWorksheets(paths);

        BatchOperateWorksheetsResp batchOperateWorksheetsResult = new BatchOperateWorksheetsResp();
        if (CollectionUtils.isNotEmpty(divideBatchOperateWorksheets.getNormalPaths())) {

            batchOperateWorksheetsResult.addResult(
                    worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS)
                            .batchDeleteWorksheets(projectId, divideBatchOperateWorksheets.getNormalPaths()));
        }
        if (CollectionUtils.isNotEmpty(divideBatchOperateWorksheets.getReposPaths())) {
            batchOperateWorksheetsResult.addResult(
                    worksheetServiceFactory.getProjectFileService(WorksheetLocation.REPOS)
                            .batchDeleteWorksheets(projectId, divideBatchOperateWorksheets.getReposPaths()));
        }
        return batchOperateWorksheetsResult;
    }

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, String pathStr, String destinationPath) {
        Path path = new Path(pathStr);
        Path destPath = new Path(destinationPath);
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                path.getLocation());
        return projectFileService.renameWorksheet(projectId, path, destPath);
    }


    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, String pathStr, UpdateWorksheetReq req) {
        Path path = new Path(pathStr);
        WorksheetService projectFileService = worksheetServiceFactory.getProjectFileService(
                path.getLocation());
        return projectFileService.editWorksheet(projectId, path, req.getObjectId(), req.getTotalLength(),
                req.getPrevVersion());
    }


    @Override
    public String batchDownloadWorksheets(Long projectId, Set<String> paths) {
        DivideBatchOperateWorksheets divideBatchOperateWorksheets = new DivideBatchOperateWorksheets(paths);
        if (divideBatchOperateWorksheets.size() == 1) {
            Path downloadPath = divideBatchOperateWorksheets.findFirst().get();
            return worksheetServiceFactory.getProjectFileService(downloadPath.getLocation())
                    .getDownloadUrl(projectId, downloadPath);
        }
        Path commonParentPath =
                WorksheetPathUtil.findCommonPath(divideBatchOperateWorksheets.all());
        String rootDirectoryName = getRootDirectoryName(projectId, commonParentPath);
        String parentOfDownloadDirectory = WorksheetUtil.getWorksheetDownloadDirectory();
        String downloadDirectoryStr = parentOfDownloadDirectory + rootDirectoryName;
        String zipFileStr = WorksheetUtil.getWorksheetDownloadZipPath(parentOfDownloadDirectory, rootDirectoryName);
        java.nio.file.Path downloadDirectoryPath =
                WorksheetPathUtil.createFileWithParent(downloadDirectoryStr, true);
        File downloadDirectory = downloadDirectoryPath.toFile();
        try {
            if (CollectionUtils.isNotEmpty(divideBatchOperateWorksheets.getNormalPaths())) {
                worksheetServiceFactory.getProjectFileService(WorksheetLocation.WORKSHEETS)
                        .downloadPathsToDirectory(projectId, divideBatchOperateWorksheets.getNormalPaths(),
                                commonParentPath, downloadDirectory);
            }
            if (CollectionUtils.isNotEmpty(divideBatchOperateWorksheets.getReposPaths())) {
                worksheetServiceFactory.getProjectFileService(WorksheetLocation.REPOS)
                        .downloadPathsToDirectory(projectId, divideBatchOperateWorksheets.getReposPaths(),
                                commonParentPath, downloadDirectory);
            }

            try {
                WorksheetPathUtil.createFileWithParent(zipFileStr, false);
                OdcFileUtil.zip(downloadDirectoryStr, zipFileStr);
            } catch (IOException e) {
                throw new InternalServerError("create file error,"
                        + "downloadDirectoryStr: " + downloadDirectoryStr + ",zipFileStr: " + zipFileStr, e);
            }
            String zipOssObjectId = CloudObjectStorageUtil.generateOneDayObjectName(
                    WorksheetUtil.getZipFileName(rootDirectoryName));
            objectStorageClient.putObject(zipOssObjectId, new File(zipFileStr),
                    ObjectTagging.temp());
            return objectStorageClient.generateDownloadUrl(zipOssObjectId,
                    WorksheetConstant.DOWNLOAD_DURATION_SECONDS).toString();
        } catch (IOException e) {
            log.error("batch download worksheets error, projectId: {}, paths: {}", projectId, paths, e);
            throw new InternalServerError(
                    "batch download worksheets error, projectId: " +
                            projectId + ", paths: " + JsonUtils.toJson(paths),
                    e);
        } finally {
            OdcFileUtil.deleteFiles(new File(parentOfDownloadDirectory));
        }
    }

    private String getRootDirectoryName(Long projectId, Path commonParentPath) {
        if (commonParentPath.isGitRepo()) {
            return commonParentPath.getName();
        }
        if (commonParentPath.isSystemDefine()) {
            return getProjectName(projectId);
        }
        return commonParentPath.getName();
    }

    private String getProjectName(Long projectId) {
        ProjectEntity byId = projectRepository.getReferenceById(projectId);
        return byId.getName();
    }
}
