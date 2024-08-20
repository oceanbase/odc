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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.CHANGE_FILE_NUM_LIMIT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.metadb.worksheet.WorksheetRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstant;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * handle the worksheet in directory: /Worksheets/
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Slf4j
@Service
public class DefaultWorksheetService implements WorksheetService {

    public DefaultWorksheetService(
            TransactionTemplate transactionTemplate,
            ObjectStorageClient objectStorageClient,
            WorksheetRepository worksheetRepository,
            AuthenticationFacade authenticationFacade) {
        this.transactionTemplate = transactionTemplate;
        this.objectStorageClient = objectStorageClient;
        this.worksheetRepository = worksheetRepository;
        this.authenticationFacade = authenticationFacade;
    }

    private final WorksheetRepository worksheetRepository;
    private final ObjectStorageClient objectStorageClient;
    private final TransactionTemplate transactionTemplate;
    private final AuthenticationFacade authenticationFacade;

    private long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, Path path) {
        String objectId = WorksheetUtil.getObjectIdOfWorksheets(path);
        String uploadUrl = objectStorageClient.generateUploadUrl(objectId).toString();
        return GenerateWorksheetUploadUrlResp.builder().uploadUrl(uploadUrl).objectId(objectId).build();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Worksheet createWorksheet(Long projectId, Path createPath, String objectId) {
        Optional<Path> parentPath = createPath.getParentPath();
        if (createPath.isSystemDefine() || !parentPath.isPresent()) {
            throw new IllegalArgumentException(
                    "invalid path, projectId:" + projectId + "path:" + createPath + ",objectId:"
                            + objectId);
        }

        Optional<Worksheet> parentFileOptional =
                worksheetRepository.findWithSubListByProjectIdAndPathWithLock(projectId, parentPath.get());

        // There will be no situation where parentFileOptional is empty here
        Worksheet prarentprojectWorksheet =
                parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + "parent path:" + parentPath));
        Worksheet createdWorksheet = prarentprojectWorksheet.create(createPath, objectId, currentUserId());
        worksheetRepository.batchAdd(Collections.singleton(createdWorksheet));
        return createdWorksheet;
    }

    @Override
    public Worksheet getWorksheetDetails(Long projectId, Path path) {
        Optional<Worksheet> worksheetOptional =
                worksheetRepository.findByProjectIdAndPath(projectId, path);
        if (!worksheetOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }

        Worksheet worksheet = worksheetOptional.get();
        if (path.isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            worksheet.setContentDownloadUrl(
                    objectStorageClient.generateDownloadUrl(worksheetOptional.get().getObjectId(),
                            WorksheetConstant.DOWNLOAD_DURATION_SECONDS).toString());
        }

        return worksheet;
    }

    @Override
    public List<Worksheet> listWorksheets(Long projectId, Path path, Integer depth, String nameLike) {
        if (path == null) {
            path = Path.root();
        }
        Optional<Worksheet> worksheetOptional =
                worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId, path,
                        nameLike);
        if (!worksheetOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        Worksheet worksheet = worksheetOptional.get();
        return worksheet.getSubWorksheetsInDepth(depth, true);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public BatchOperateWorksheetsResult batchUploadWorksheets(Long projectId,
            BatchCreateWorksheets batchCreateWorksheets) {
        Optional<Worksheet> parentFileOptional =
                worksheetRepository.findWithSubListByProjectIdAndPathWithLock(projectId,
                        batchCreateWorksheets.getParentPath());

        // There will be no situation where parentFileOptional is empty here
        Worksheet parentWorksheet =
                parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + "parent path:" + batchCreateWorksheets.getParentPath()));
        Set<Worksheet> innerWorksheets =
                parentWorksheet.batchCreate(batchCreateWorksheets.getCreatePathToObjectIdMap(), currentUserId());
        worksheetRepository.batchAdd(innerWorksheets);

        return BatchOperateWorksheetsResult.ofSuccess(new ArrayList<>(innerWorksheets));
    }

    @Override
    public BatchOperateWorksheetsResult batchDeleteWorksheets(Long projectId, List<Path> paths) {
        List<Worksheet> deleteWorksheets = new ArrayList<>();
        Set<Path> deleteDirectoryPaths = new HashSet<>();
        List<Path> deleteFilePaths = new ArrayList<>();
        for (Path path : paths) {
            if (path.isDirectory()) {
                deleteDirectoryPaths.add(path);
            }
            if (path.isFile()) {
                deleteFilePaths.add(path);
            }
        }
        if (!deleteFilePaths.isEmpty()) {
            List<Worksheet> worksheets = worksheetRepository.listByProjectIdAndInPaths(projectId,
                    deleteFilePaths);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                deleteWorksheets.addAll(worksheets);
            }

        }
        if (!deleteDirectoryPaths.isEmpty()) {
            Path commonParentPath = WorksheetPathUtil.findCommonPath(deleteDirectoryPaths);
            List<Worksheet> worksheets = worksheetRepository.listWithSubListByProjectIdAndPath(projectId,
                    commonParentPath);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                Path[] deleteDirectoryArr = deleteDirectoryPaths.toArray(new Path[0]);
                for (Worksheet worksheet : worksheets) {
                    if (deleteDirectoryPaths.contains(worksheet.getPath()) ||
                            worksheet.getPath().isChildOfAny(deleteDirectoryArr)) {
                        deleteWorksheets.add(worksheet);
                    }
                }
            }
        }
        if (deleteWorksheets.size() > CHANGE_FILE_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_NUM,
                    (double) CHANGE_FILE_NUM_LIMIT, "delete num is over limit " + CHANGE_FILE_NUM_LIMIT);
        }
        if (CollectionUtils.isEmpty(deleteWorksheets)) {
            return new BatchOperateWorksheetsResult();
        }
        int batchSize = 200;
        BatchOperateWorksheetsResult result = new BatchOperateWorksheetsResult();
        Iterables.partition(deleteWorksheets, batchSize).forEach(files -> {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    worksheetRepository.batchDelete(deleteWorksheets.stream()
                            .map(Worksheet::getId).collect(Collectors.toSet()));
                    objectStorageClient.deleteObjects(files.stream()
                            .map(Worksheet::getObjectId).collect(Collectors.toList()));

                    result.addSuccess(files);
                } catch (Throwable e) {
                    result.addFailed(files);
                    log.error("partition batch delete files failed, projectId:{}, paths:{}", projectId, paths);
                }
            });
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Worksheet> renameWorksheet(Long projectId, Path path, Path destinationPath) {
        Optional<Worksheet> worksheetOptional =
                worksheetRepository.findWithSubListByProjectIdAndPathWithLock(projectId, path);
        Worksheet worksheet =
                worksheetOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<Worksheet> renamedWorksheets = worksheet.rename(destinationPath);
        worksheetRepository.batchUpdateById(renamedWorksheets);

        return new ArrayList<>(renamedWorksheets);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public List<Worksheet> editWorksheet(Long projectId, Path path, Path destinationPath,
            String objectId, Long readVersion) {
        Optional<Worksheet> worksheetOptional =
                worksheetRepository.findWithSubListByProjectIdAndPathWithLock(projectId, path);
        Worksheet worksheet =
                worksheetOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<Worksheet> editedWorksheets = worksheet.edit(destinationPath, objectId, readVersion);
        worksheetRepository.batchUpdateById(editedWorksheets);

        return new ArrayList<>(editedWorksheets);
    }

    @Override
    public String getDownloadUrl(Long projectId, Path path) {
        path.canGetDownloadUrlCheck();
        Optional<Worksheet> worksheetOptional =
                worksheetRepository.findByProjectIdAndPath(projectId, path);
        Worksheet worksheet = worksheetOptional.orElseThrow(
                () -> new NotFoundException(ErrorCodes.NotFound,
                        new Object[] {"path", path.toString()},
                        String.format("path not found by %s=%s", "path", path)));
        PreConditions.notBlank(worksheet.getObjectId(), "objectId");
        return objectStorageClient
                .generateDownloadUrl(worksheet.getObjectId(), WorksheetConstant.DOWNLOAD_DURATION_SECONDS).toString();
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, List<Path> paths, Path commParentPath,
            File destinationDirectory) {
        PreConditions.notEmpty(paths, "paths");
        PreConditions.notNull(destinationDirectory, "destinationDirectory");
        PreConditions.notNull(commParentPath, "commParentPath");
        PreConditions.validArgumentState(destinationDirectory.isDirectory(), ErrorCodes.IllegalArgument, null,
                "destinationDirectory is not directory");
        for (Path path : paths) {
            boolean needTooLoadSubFiles = path.isDirectory();
            Optional<Worksheet> worksheetOptional =
                    worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId, path, null);
            // There will be no situation where parentFileOptional is empty here
            Worksheet worksheet =
                    worksheetOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                            + projectId + " path:" + path));
            downloadWorksheetToDirectory(projectId, worksheet, commParentPath, destinationDirectory);
            if (CollectionUtils.isNotEmpty(worksheet.getSubWorksheets())) {
                for (Worksheet subWorksheet : worksheet.getSubWorksheets()) {
                    downloadWorksheetToDirectory(projectId, subWorksheet, commParentPath, destinationDirectory);
                }
            }
        }
    }

    private void downloadWorksheetToDirectory(Long projectId, Worksheet worksheet, Path commParentPath,
            File destinationDirectory) {
        Optional<String> relativePathOptional = worksheet.getPath().stripPrefix(commParentPath);
        if (!relativePathOptional.isPresent()) {
            log.warn("not download worksheet because of relative file path is empty after strip prefix path,"
                    + " projectId:{}, worksheet path:{},commonParentPath:{}", projectId, worksheet, commParentPath);
            return;
        }
        if (worksheet.getPath().isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativePathOptional.get();
            java.nio.file.Path filePath = WorksheetPathUtil.createFileWithParent(absoluteFile, false);
            try {
                objectStorageClient.downloadToFile(worksheet.getObjectId(), filePath.toFile());
            } catch (IOException e) {
                log.error(
                        "download worksheet to file failed, projectId:{}, worksheet:{},commonParentPath:{},objetId{},filePath:{}",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), filePath);
                throw new InternalServerError(String.format(
                        "download worksheet to file failed, projectId:%d, worksheet:%s,commonParentPath:%s,objetId:%s,filePath:%s",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), filePath), e);
            }
        }
        if (worksheet.getPath().isDirectory()) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativePathOptional.get();
            WorksheetPathUtil.createFileWithParent(absoluteFile, true);
        }
    }
}
