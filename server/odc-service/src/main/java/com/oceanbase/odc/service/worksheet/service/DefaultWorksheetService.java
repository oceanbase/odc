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
import java.util.ArrayList;
import java.util.Collections;
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
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.BatchOperateWorksheetsResult;
import com.oceanbase.odc.service.worksheet.domain.DefaultWorksheetRepository;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.domain.WorksheetOssGateway;
import com.oceanbase.odc.service.worksheet.exceptions.ChangeTooMuchException;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;

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
            WorksheetOssGateway projectFileOssGateway,
            DefaultWorksheetRepository normalProjectFilesRepository) {
        this.transactionTemplate = transactionTemplate;
        this.projectFileOssGateway = projectFileOssGateway;
        this.normalProjectFilesRepository = normalProjectFilesRepository;
    }

    private final DefaultWorksheetRepository normalProjectFilesRepository;
    private final WorksheetOssGateway projectFileOssGateway;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Worksheet createWorksheet(Long projectId, Path createPath, String objectId) {
        Optional<Path> parentPath = createPath.getParentPath();
        if (!parentPath.isPresent()) {
            throw new IllegalArgumentException(
                    "invalid path, projectId:" + projectId + "path:" + createPath + ",objectId:"
                            + objectId);
        }
        Optional<Worksheet> parentFileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, parentPath.get(),
                        true, true, true, false);

        // There will be no situation where parentFileOptional is empty here
        Worksheet prarentprojectWorksheet =
                parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + "parent path:" + parentPath));
        Worksheet createdWorksheet = prarentprojectWorksheet.create(createPath, objectId);
        normalProjectFilesRepository.batchAdd(Collections.singleton(createdWorksheet));
        if (createdWorksheet.getPath().isFile()) {
            projectFileOssGateway.copyTo(objectId, createPath);
        }
        return createdWorksheet;
    }

    @Override
    public Worksheet getWorksheetDetails(Long projectId, Path path) {
        Optional<Worksheet> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }

        Worksheet worksheet = fileOptional.get();
        if (path.isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            worksheet.setContent(projectFileOssGateway.getContent(fileOptional.get().getObjectId()));
        }

        return worksheet;
    }

    @Override
    public List<Worksheet> listWorksheets(Long projectId, Path path) {
        Optional<Worksheet> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        Worksheet worksheet = fileOptional.get();

        return worksheet.getNextLevelFiles();
    }

    @Override
    public List<Worksheet> searchWorksheets(Long projectId, String nameLike, int limit) {
        if (StringUtils.isBlank(nameLike)) {
            return new ArrayList<>();
        }
        return normalProjectFilesRepository.listByProjectIdAndPathNameLike(projectId, nameLike, limit);
    }

    @Override
    public BatchOperateWorksheetsResult batchUploadWorksheets(Long projectId,
            BatchCreateWorksheets batchCreateWorksheets) {
        Set<Worksheet> createWorksheets = transactionTemplate.execute(status -> {
            Optional<Worksheet> parentFileOptional =
                    normalProjectFilesRepository.findByProjectAndPath(projectId,
                            batchCreateWorksheets.getParentPath(),
                            true, true, true, false);

            // 这里不会出现parentFileOptional为空的情况
            Worksheet parentWorksheet =
                    parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                            + projectId + "parent path:" + batchCreateWorksheets.getParentPath()));
            Set<Worksheet> innerWorksheets =
                    parentWorksheet.batchCreate(batchCreateWorksheets.getCreatePathToObjectIdMap());
            normalProjectFilesRepository.batchAdd(innerWorksheets);
            return innerWorksheets;
        });
        if (CollectionUtils.isEmpty(createWorksheets)) {
            return new BatchOperateWorksheetsResult();
        }

        BatchOperateWorksheetsResult batchOperateWorksheetsResult = new BatchOperateWorksheetsResult();
        for (Worksheet worksheet : createWorksheets) {
            if (worksheet.getPath().isFile()) {
                try {
                    projectFileOssGateway.copyTo(worksheet.getObjectId(), worksheet.getPath());
                    batchOperateWorksheetsResult.addSuccess(worksheet);
                } catch (Exception e) {
                    batchOperateWorksheetsResult.addFailed(worksheet);
                    log.error("copy file to path failed, projectId:{}, path:{}, objectId:{}", projectId,
                            worksheet.getPath(),
                            worksheet.getObjectId());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(batchOperateWorksheetsResult.getFailed())) {
            normalProjectFilesRepository.batchDelete(batchOperateWorksheetsResult.getFailed()
                    .stream().map(Worksheet::getId).collect(Collectors.toSet()));
        }

        return batchOperateWorksheetsResult;
    }

    @Override
    public BatchOperateWorksheetsResult batchDeleteWorksheets(Long projectId, Set<Path> paths) {
        List<Worksheet> deleteFiles = new ArrayList<>();
        for (Path path : paths) {
            List<Worksheet> files =
                    normalProjectFilesRepository.listByProjectIdAndPath(projectId, path, true);
            deleteFiles.addAll(files);
        }
        if (deleteFiles.size() > CHANGE_FILE_NUM_LIMIT) {
            throw new ChangeTooMuchException("delete num is over limit " + CHANGE_FILE_NUM_LIMIT);
        }
        if (CollectionUtils.isEmpty(deleteFiles)) {
            return new BatchOperateWorksheetsResult();
        }
        int batchSize = 200;
        BatchOperateWorksheetsResult result = new BatchOperateWorksheetsResult();
        Iterables.partition(deleteFiles, batchSize).forEach(files -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    normalProjectFilesRepository.batchDelete(deleteFiles.stream()
                            .map(Worksheet::getId).collect(Collectors.toSet()));
                    projectFileOssGateway.batchDelete(files.stream()
                            .map(Worksheet::getObjectId).collect(Collectors.toSet()));
                });
                result.addSuccess(files);
            } catch (Throwable e) {
                result.addFailed(files);
                log.error("partition batch delete files failed, projectId:{}, paths:{}", projectId, paths);
            }
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Worksheet> renameWorksheet(Long projectId, Path path, Path destinationPath) {
        Optional<Worksheet> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        true, true, true, true);
        Worksheet worksheet =
                fileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<Worksheet> renamedWorksheets = worksheet.rename(destinationPath);
        normalProjectFilesRepository.batchUpdateById(renamedWorksheets, false);

        return new ArrayList<>(renamedWorksheets);
    }

    @Override
    public List<Worksheet> editWorksheet(Long projectId, Path path, Path destinationPath,
            String objectId, Long readVersion) {
        Optional<Worksheet> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        true, true, true, true);
        Worksheet worksheet =
                fileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<Worksheet> editedWorksheets = worksheet.edit(destinationPath, objectId, readVersion);
        normalProjectFilesRepository.batchUpdateById(editedWorksheets, true);

        return new ArrayList<>(editedWorksheets);
    }

    @Override
    public String batchDownloadWorksheets(Long projectId, Set<String> paths) {
        return "";
    }

    @Override
    public String getDownloadUrl(Long projectId, Path path) {
        path.canGetDownloadUrlCheck();
        Optional<Worksheet> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        false, false, false, true);
        Worksheet worksheet = fileOptional.orElseThrow(
                () -> new NotFoundException(ErrorCodes.NotFound,
                        new Object[] {"path", path.toString()},
                        String.format("path not found by %s=%s", "path", path)));
        PreConditions.notBlank(worksheet.getObjectId(), "objectId");
        return projectFileOssGateway.generateDownloadUrl(worksheet.getObjectId());
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, Set<Path> paths, Optional<Path> commParentPath,
            File destinationDirectory) {
        PreConditions.notEmpty(paths, "paths");
        PreConditions.notNull(destinationDirectory, "destinationDirectory");
        PreConditions.assertTrue(destinationDirectory.isDirectory(), "destinationDirectory");
        for (Path path : paths) {
            boolean needTooLoadSubFiles = path.isDirectory();
            Optional<Worksheet> worksheetOptional =
                    normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                            false, true, needTooLoadSubFiles, false);
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

    private void downloadWorksheetToDirectory(Long projectId, Worksheet worksheet, Optional<Path> commParentPath,
            File destinationDirectory) {
        Optional<String> relativFilePathOptional = worksheet.getPath().stripPrefix(commParentPath);
        if (!relativFilePathOptional.isPresent()) {
            log.warn("not download worksheet because of relative file path is empty after strip prefix path,"
                    + " projectId:{}, worksheet path:{},commonParentPath:{}", projectId, worksheet, commParentPath);
            return;
        }
        if (worksheet.getPath().isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativFilePathOptional.get();
            java.nio.file.Path filePath = WorksheetPathUtil.createFileWithParent(absoluteFile, false);
            projectFileOssGateway.downloadToFile(worksheet.getObjectId(), filePath.toFile());
        }
        if (worksheet.getPath().isDirectory()) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativFilePathOptional.get();
            WorksheetPathUtil.createFileWithParent(absoluteFile, true);
        }
    }
}
