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
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.NAME_LENGTH_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.PATH_LENGTH_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstant.SAME_LEVEL_NUM_LIMIT;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetEntity;
import com.oceanbase.odc.metadb.worksheet.CollaborationWorksheetRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.client.ObjectStorageClient;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstant;
import com.oceanbase.odc.service.worksheet.converter.WorksheetConverter;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheets;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.model.BatchOperateWorksheetsResp;
import com.oceanbase.odc.service.worksheet.model.GenerateWorksheetUploadUrlResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetMetaResp;
import com.oceanbase.odc.service.worksheet.model.WorksheetResp;
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
            CollaborationWorksheetRepository worksheetRepository,
            AuthenticationFacade authenticationFacade) {
        this.transactionTemplate = transactionTemplate;
        this.objectStorageClient = objectStorageClient;
        this.worksheetRepository = worksheetRepository;
        this.authenticationFacade = authenticationFacade;
    }

    private final CollaborationWorksheetRepository worksheetRepository;
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
    public WorksheetMetaResp createWorksheet(Long projectId, Path createPath, String objectId) {
        BatchCreateWorksheets batchCreateWorksheets = new BatchCreateWorksheets(createPath, objectId);
        createCheck(projectId, batchCreateWorksheets);

        CollaborationWorksheetEntity entity = CollaborationWorksheetEntity.builder()
                .projectId(projectId)
                .creatorId(currentUserId())
                .path(createPath.getStandardPath())
                .levelNum(createPath.getLevelNum())
                .objectId(objectId)
                .extension(createPath.getExtension())
                // TODO add totalLength to interface
                .totalLength(0L)
                .build();
        CollaborationWorksheetEntity result = worksheetRepository.saveAndFlush(entity);
        return WorksheetConverter.convertEntityToMetaResp(result);
    }

    private void pathOrNameTooLongCheck(Set<Path> createPaths) {
        createPaths.forEach(createPath -> {
            if (createPath.isExceedPathLengthLimit()) {
                throw new BadRequestException(ErrorCodes.OverLimit, new Object[] {"path", PATH_LENGTH_LIMIT},
                        "path length is over limit " + PATH_LENGTH_LIMIT + ", path:" + createPath);
            }
            if (createPath.isExceedNameLengthLimit()) {
                throw new BadRequestException(ErrorCodes.OverLimit, new Object[] {"name", NAME_LENGTH_LIMIT},
                        "name length is over limit " + NAME_LENGTH_LIMIT + ", path:" + createPath);
            }
        });
    }

    private void createCheck(Long projectId, BatchCreateWorksheets createWorksheets) {
        List<String> queryPaths = createWorksheets.getCreatePathToObjectIdMap().keySet().stream()
                .map(Path::getStandardPath).collect(Collectors.toList());
        queryPaths.add(createWorksheets.getParentPath().getStandardPath());

        pathOrNameTooLongCheck(createWorksheets.getCreatePathToObjectIdMap().keySet());

        // the exist worksheets number + to create worksheets number can't exceed limit
        Integer levelNumOfToCreatePath = createWorksheets.getParentPath().getLevelNum() + 1;
        long existNum = worksheetRepository.countByPathLikeWithFilter(projectId,
                createWorksheets.getParentPath().getStandardPath(), levelNumOfToCreatePath, levelNumOfToCreatePath,
                null);
        int toCreateNum = createWorksheets.getCreatePathToObjectIdMap().size();
        if (existNum + toCreateNum > SAME_LEVEL_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_SAME_LEVEL,
                    (double) SAME_LEVEL_NUM_LIMIT,
                    "create path num exceed limit, create path num: " + toCreateNum
                            + ", same level exist file num: " + existNum);
        }

        // parent exist,create paths not exist
        List<CollaborationWorksheetEntity> worksheets = worksheetRepository.findByProjectIdAndInPaths(
                projectId, queryPaths);
        boolean hasParent = false;
        for (CollaborationWorksheetEntity worksheet : worksheets) {
            Path path = new Path(worksheet.getPath());
            if (path.equals(createWorksheets.getParentPath())) {
                hasParent = true;
            } else if (createWorksheets.getCreatePathToObjectIdMap().containsKey(path)) {
                throw new BadRequestException(ErrorCodes.DuplicatedExists,
                        new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path",
                                path},
                        "create path existed,path:" + path);
            }
        }
        if (!createWorksheets.getParentPath().isSystemDefine() && !hasParent) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path",
                            createWorksheets.getParentPath()},
                    "can't find path, projectId:" + projectId + "path:" + createWorksheets.getParentPath());
        }
    }

    @Override
    public WorksheetResp getWorksheetDetails(Long projectId, Path path) {
        Optional<CollaborationWorksheetEntity> worksheetOptional =
                worksheetRepository.findByProjectIdAndPath(projectId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithNotFoundCheck(projectId, path, worksheetOptional);

        String contentDownloadUrl = null;
        if (path.isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            contentDownloadUrl = objectStorageClient.generateDownloadUrl(worksheet.getObjectId(),
                    WorksheetConstant.DOWNLOAD_DURATION_SECONDS).toString();
        }

        return WorksheetConverter.convertEntityToResp(worksheetOptional.get(), contentDownloadUrl);
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, Path path, Integer depth, String nameLike) {
        if (path == null) {
            path = Path.root();
        }
        Integer minLevelNumberFilter = depth == null || depth <= 0 ? null : path.getLevelNum() + 1;
        Integer maxLevelNumberFilter = depth == null || depth <= 0 ? null : path.getLevelNum() + depth;;
        List<CollaborationWorksheetEntity> entities = worksheetRepository.findByPathLikeWithFilter(
                projectId, path.getStandardPath(),
                minLevelNumberFilter, maxLevelNumberFilter, nameLike);
        return entities.stream().map(WorksheetConverter::convertEntityToMetaResp).collect(Collectors.toList());
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId,
            BatchCreateWorksheets batchCreateWorksheets) {
        createCheck(projectId, batchCreateWorksheets);
        long userId = currentUserId();
        List<CollaborationWorksheetEntity> entities = batchCreateWorksheets.getCreatePathToObjectIdMap().entrySet()
                .stream()
                .map(item -> CollaborationWorksheetEntity.builder()
                        .projectId(projectId)
                        .creatorId(userId)
                        .path(item.getKey().getStandardPath())
                        .levelNum(item.getKey().getLevelNum())
                        .objectId(item.getValue())
                        .extension(item.getKey().getExtension())
                        // TODO add totalLength to interface
                        .totalLength(0L)
                        .build())
                .collect(Collectors.toList());

        List<CollaborationWorksheetEntity> results = worksheetRepository.saveAllAndFlush(entities);

        return BatchOperateWorksheetsResp
                .ofSuccess(results.stream().map(WorksheetConverter::convertEntityToMetaResp).collect(
                        Collectors.toList()));
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, List<Path> paths) {
        List<CollaborationWorksheetEntity> deleteWorksheets = listWithSubListByProjectIdAndPaths(projectId, paths);
        if (deleteWorksheets.size() > CHANGE_FILE_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_NUM,
                    (double) CHANGE_FILE_NUM_LIMIT, "delete number is over limit " + CHANGE_FILE_NUM_LIMIT);
        }
        if (CollectionUtils.isEmpty(deleteWorksheets)) {
            return new BatchOperateWorksheetsResp();
        }
        int batchSize = 200;
        BatchOperateWorksheetsResp result = new BatchOperateWorksheetsResp();
        Iterables.partition(deleteWorksheets, batchSize).forEach(files -> {
            List<WorksheetMetaResp> toDeletes = deleteWorksheets.stream().map(
                    WorksheetConverter::convertEntityToMetaResp).collect(Collectors.toList());
            boolean success = false;
            try {
                worksheetRepository.deleteAllByIdInBatch(deleteWorksheets.stream()
                        .map(CollaborationWorksheetEntity::getId).collect(Collectors.toSet()));
                result.addSuccess(toDeletes);
                success = true;
                deleteByObjectIdsInObjectStorage(files);

            } catch (Throwable e) {
                if (!success) {
                    result.addFailed(toDeletes);
                }
                log.error("partition batch delete files failed, projectId:{}, paths:{}", projectId, paths);
            }
        });
        return result;
    }

    private void deleteByObjectIdsInObjectStorage(List<CollaborationWorksheetEntity> toDeletes) {
        try {
            List<String> deletedObjectIds = objectStorageClient.deleteObjects(toDeletes.stream()
                    .map(CollaborationWorksheetEntity::getObjectId).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(deletedObjectIds)) {
                List<CollaborationWorksheetEntity> notDeletedSuccess = new ArrayList<>();
                for (CollaborationWorksheetEntity entity : toDeletes) {
                    if (deletedObjectIds.contains(entity.getObjectId())) {
                        notDeletedSuccess.add(entity);
                    }
                }
                log.error("deleteByObjectIdsInObjectStorage partial success,notDeletedSuccess: {}",
                        JsonUtils.toJson(notDeletedSuccess));
            }
        } catch (Exception e) {
            log.error("deleteByObjectIdsInObjectStorage has exception,toDeletes: {}",
                    JsonUtils.toJson(toDeletes));
        }
    }

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, Path path, Path destinationPath) {
        pathOrNameTooLongCheck(Collections.singleton(destinationPath));
        WorksheetPathUtil.renameValidCheck(path, destinationPath);
        renameDuplicatedCheck(projectId, path, destinationPath);
        List<CollaborationWorksheetEntity> entities = worksheetRepository.findByPathLikeWithFilter(projectId,
                path.getStandardPath(), null, null, null);
        renamePathNotFoundCheck(projectId, path, entities);

        Set<CollaborationWorksheetEntity> renamedEntities = doRename(path, destinationPath, entities);
        if (CollectionUtils.isEmpty(renamedEntities)) {
            return new ArrayList<>();
        }
        worksheetRepository.batchUpdatePath(renamedEntities.stream()
                .collect(Collectors.toMap(CollaborationWorksheetEntity::getId, CollaborationWorksheetEntity::getPath)));

        return renamedEntities.stream().map(WorksheetConverter::convertEntityToMetaResp)
                .collect(Collectors.toList());
    }

    private Set<CollaborationWorksheetEntity> doRename(Path path, Path destinationPath,
            List<CollaborationWorksheetEntity> renameAndSubEntities) {
        Set<CollaborationWorksheetEntity> changedWorksheets = new HashSet<>();
        if (CollectionUtils.isEmpty(renameAndSubEntities)) {
            return changedWorksheets;
        }
        for (CollaborationWorksheetEntity subFile : renameAndSubEntities) {
            Path subPath = new Path(subFile.getPath());
            if (subPath.rename(path, destinationPath)) {
                subFile.setPath(subPath.getStandardPath());
                changedWorksheets.add(subFile);
            }
            if (changedWorksheets.size() > CHANGE_FILE_NUM_LIMIT - 1) {
                throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_NUM, (double) CHANGE_FILE_NUM_LIMIT,
                        "change num is over limit " + CHANGE_FILE_NUM_LIMIT);
            }
        }
        return changedWorksheets;
    }

    private void renamePathNotFoundCheck(Long projectId, Path renamePath,
            List<CollaborationWorksheetEntity> renameAndSubEntities) {
        boolean hasRenamePath = false;
        for (CollaborationWorksheetEntity worksheet : renameAndSubEntities) {
            Path path = new Path(worksheet.getPath());
            if (path.equals(renamePath)) {
                hasRenamePath = true;
            }
        }
        if (!hasRenamePath) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "renamePath",
                            renamePath},
                    "can't find renamePath, projectId:" + projectId + "renamePath:" + renamePath);
        }
    }

    private void renameDuplicatedCheck(Long projectId, Path path, Path destinationPath) {
        Optional<CollaborationWorksheetEntity> existWorksheets = worksheetRepository.findByProjectIdAndPath(
                projectId, destinationPath.getStandardPath());
        if (existWorksheets.isPresent()) {
            throw new BadRequestException(ErrorCodes.DuplicatedExists,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path", destinationPath},
                    "duplicated path name for rename,projectId:" + projectId + ",path:" + path + ",destinationPath:"
                            + destinationPath);
        }
    }

    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, Path path,
            String objectId, Long totalLength, Long readVersion) {
        PreConditions.notNull(objectId, "objectId");
        PreConditions.notNull(readVersion, "readVersion");
        PreConditions.validArgumentState(path.isFile(), ErrorCodes.Unsupported, null,
                "Unsupported edit path: " + path);

        Optional<CollaborationWorksheetEntity> worksheetOptional = worksheetRepository.findByProjectIdAndPath(
                projectId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithNotFoundCheck(projectId, path, worksheetOptional);
        versionConflictCheck(projectId, path, readVersion, worksheet.getVersion());
        doEdit(worksheet, path, totalLength, objectId);

        return Collections.singletonList(WorksheetConverter.convertEntityToMetaResp(worksheet));
    }

    private CollaborationWorksheetEntity getWithNotFoundCheck(Long projectId, Path path,
            Optional<CollaborationWorksheetEntity> worksheetOptional) {
        if (!worksheetOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path", path},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        return worksheetOptional.get();
    }

    private void versionConflictCheck(Long projectId, Path path, Long readVersion, Long dbVersion) {
        if (readVersion != null && !readVersion.equals(dbVersion)) {
            throw new BadRequestException(ErrorCodes.EditVersionConflict,
                    new Object[] {}, "version conflict,current version:" + dbVersion
                            + ",read version:" + readVersion + ",projectId:" + projectId + ",path:" + path);
        }
    }

    private void doEdit(CollaborationWorksheetEntity editWorksheet, Path path, Long totalLength, String objectId) {
        editWorksheet.setObjectId(objectId);
        editWorksheet.setLevelNum(path.getLevelNum());
        editWorksheet.setTotalLength(totalLength);
        int updateCount = worksheetRepository.updateContentByIdAndVersion(editWorksheet);
        if (updateCount == 0) {
            throw new BadRequestException(ErrorCodes.EditVersionConflict,
                    new Object[] {}, "version conflict"
                            + ",read version:" + editWorksheet.getVersion() + ",projectId:"
                            + editWorksheet.getProjectId() + ",path:" + path);
        }
    }

    @Override
    public String getDownloadUrl(Long projectId, Path path) {
        path.canGetDownloadUrlCheck();
        Optional<CollaborationWorksheetEntity> worksheetOptional =
                worksheetRepository.findByProjectIdAndPath(projectId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithNotFoundCheck(projectId, path, worksheetOptional);
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
        List<CollaborationWorksheetEntity> downloadWorksheets = listWithSubListByProjectIdAndPaths(projectId, paths);
        if (CollectionUtils.isEmpty(downloadWorksheets)) {
            return;
        }
        if (downloadWorksheets.size() > CHANGE_FILE_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_NUM,
                    (double) CHANGE_FILE_NUM_LIMIT, "download number is over limit " + CHANGE_FILE_NUM_LIMIT);
        }
        for (CollaborationWorksheetEntity worksheet : downloadWorksheets) {
            downloadWorksheetToDirectory(projectId, worksheet, commParentPath, destinationDirectory);
        }
    }

    private void downloadWorksheetToDirectory(Long projectId, CollaborationWorksheetEntity worksheet,
            Path commParentPath,
            File destinationDirectory) {
        Path path = new Path(worksheet.getPath());
        Optional<String> relativePathOptional = path.stripPrefix(commParentPath);
        if (!relativePathOptional.isPresent()) {
            log.warn("not download worksheet because of relative file path is empty after strip prefix path,"
                    + " projectId:{}, worksheet path:{},commonParentPath:{}", projectId, worksheet, commParentPath);
            return;
        }
        if (path.isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativePathOptional.get();
            java.nio.file.Path filePath = WorksheetPathUtil.createFileWithParent(absoluteFile, false);
            try {
                objectStorageClient.downloadToFile(worksheet.getObjectId(), filePath.toFile());
            } catch (IOException e) {
                log.error(
                        "download worksheet to file failed, projectId:{}, worksheet:{},commonParentPath:{},objetId{},"
                                + "filePath:{}",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), filePath);
                throw new InternalServerError(String.format(
                        "download worksheet to file failed, projectId:%d, worksheet:%s,commonParentPath:%s,objetId:%s,"
                                + "filePath:%s",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), filePath), e);
            }
        }
        if (path.isDirectory()) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativePathOptional.get();
            WorksheetPathUtil.createFileWithParent(absoluteFile, true);
        }
    }

    public List<CollaborationWorksheetEntity> listWithSubListByProjectIdAndPaths(Long projectId, List<Path> paths) {
        List<CollaborationWorksheetEntity> resultWorksheets = new ArrayList<>();
        Set<Path> deleteDirectoryPaths = new HashSet<>();
        List<String> deleteFilePaths = new ArrayList<>();
        for (Path path : paths) {
            if (path.isDirectory()) {
                deleteDirectoryPaths.add(path);
            }
            if (path.isFile()) {
                deleteFilePaths.add(path.getStandardPath());
            }
        }
        if (!deleteFilePaths.isEmpty()) {
            List<CollaborationWorksheetEntity> worksheets = worksheetRepository.findByProjectIdAndInPaths(projectId,
                    deleteFilePaths);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                resultWorksheets.addAll(worksheets);
            }

        }
        if (!deleteDirectoryPaths.isEmpty()) {
            Path commonParentPath = WorksheetPathUtil.findCommonPath(deleteDirectoryPaths);
            List<CollaborationWorksheetEntity> worksheets = worksheetRepository.findByPathLikeWithFilter(projectId,
                    commonParentPath.getStandardPath(), null, null, null);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                Path[] deleteDirectoryArr = deleteDirectoryPaths.toArray(new Path[0]);
                for (CollaborationWorksheetEntity worksheet : worksheets) {
                    Path worksheetPath = new Path(worksheet.getPath());
                    if (deleteDirectoryPaths.contains(worksheetPath) ||
                            worksheetPath.isChildOfAny(deleteDirectoryArr)) {
                        resultWorksheets.add(worksheet);
                    }
                }
            }
        }
        return resultWorksheets;
    }
}
