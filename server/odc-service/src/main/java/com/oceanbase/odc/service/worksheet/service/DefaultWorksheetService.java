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

import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.CHANGE_WORKSHEET_NUM_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.NAME_LENGTH_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.PATH_LENGTH_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.PROJECT_WORKSHEET_NUM_LIMIT;
import static com.oceanbase.odc.service.worksheet.constants.WorksheetConstants.SAME_LEVEL_WORKSHEET_NUM_LIMIT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
import com.oceanbase.odc.service.resourcehistory.ResourceLastAccessService;
import com.oceanbase.odc.service.worksheet.constants.WorksheetConstants;
import com.oceanbase.odc.service.worksheet.converter.WorksheetConverter;
import com.oceanbase.odc.service.worksheet.domain.BatchCreateWorksheetsPreProcessor;
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
    CollaborationWorksheetRepository worksheetRepository;
    ObjectStorageClient objectStorageClient;
    AuthenticationFacade authenticationFacade;
    ResourceLastAccessService resourceLastAccessService;

    public DefaultWorksheetService(
            ObjectStorageClient objectStorageClient,
            CollaborationWorksheetRepository worksheetRepository,
            AuthenticationFacade authenticationFacade,
            ResourceLastAccessService resourceLastAccessService) {
        this.objectStorageClient = objectStorageClient;
        this.worksheetRepository = worksheetRepository;
        this.authenticationFacade = authenticationFacade;
        this.resourceLastAccessService = resourceLastAccessService;
    }

    @Override
    public GenerateWorksheetUploadUrlResp generateUploadUrl(Long projectId, String groupId, Path path) {
        String objectId = WorksheetUtil.getObjectIdOfWorksheets(path);
        String uploadUrl = objectStorageClient.generateUploadUrl(objectId).toString();
        return GenerateWorksheetUploadUrlResp.builder().uploadUrl(uploadUrl).objectId(objectId).build();
    }

    @Override
    public WorksheetMetaResp createWorksheet(Long projectId, String groupId, Path createPath, String objectId,
            Long size) {
        Long organizationId = currentOrganizationId();
        BatchCreateWorksheetsPreProcessor batchCreateWorksheetsPreProcessor =
                new BatchCreateWorksheetsPreProcessor(createPath, objectId, size);
        createCheck(organizationId, projectId, groupId, batchCreateWorksheetsPreProcessor);

        CollaborationWorksheetEntity entity = CollaborationWorksheetEntity.builder()
                .organizationId(organizationId)
                .projectId(projectId)
                .groupId(groupId)
                .creatorId(currentUserId())
                .path(createPath.getStandardPath())
                .pathLevel(createPath.getLevelNum())
                .objectId(objectId)
                .extension(createPath.getExtension())
                .size(size)
                .version(0L)
                .build();
        CollaborationWorksheetEntity result = worksheetRepository.saveAndFlush(entity);
        return WorksheetConverter.convertEntityToMetaResp(result);
    }

    @Override
    public WorksheetResp getWorksheetDetails(Long projectId, String groupId, Path path) {
        Optional<CollaborationWorksheetEntity> worksheetOptional =
                worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(currentOrganizationId(),
                        projectId,
                        groupId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithCheckNotFound(projectId, path, worksheetOptional);

        String contentDownloadUrl = null;
        if (path.isFile() && StringUtils.isNotBlank(worksheet.getObjectId())) {
            try {
                contentDownloadUrl = objectStorageClient.generateDownloadUrl(worksheet.getObjectId(),
                        WorksheetConstants.MAX_DURATION_DOWNLOAD_SECONDS).toString();
            } catch (Throwable e) {
                log.warn("generateDownloadUrl in getWorksheetDetails failed, projectId:{}，path:{},objectId:{}",
                        projectId, path, worksheet.getObjectId(), e);
                throw new InternalServerError("generateDownloadUrl in getWorksheetDetails failed, projectId:"
                        + projectId + "，path:" + path + ",objectId:" + worksheet.getObjectId(), e);
            }
        }
        try {
            resourceLastAccessService.add(currentOrganizationId(), projectId, currentUserId(),
                    ResourceType.ODC_WORKSHEET, worksheetOptional.get().getId(), new Date());
        } catch (Throwable e) {
            log.warn("add last access time has exception, projectId:{}，path:{},objectId:{}",
                    projectId, path, worksheet.getObjectId(), e);
        }
        return WorksheetConverter.convertEntityToResp(worksheetOptional.get(), contentDownloadUrl);
    }

    @Override
    public List<WorksheetMetaResp> listWorksheets(Long projectId, String groupId, Path path, Integer depth,
            String nameLike) {
        Integer minLevelNumberFilter = depth == null || depth <= 0 ? null : path.getLevelNum() + 1;
        Integer maxLevelNumberFilter = depth == null || depth <= 0 ? null : path.getLevelNum() + depth;;
        List<CollaborationWorksheetEntity> entities = worksheetRepository.findByPathLikeWithFilter(
                currentOrganizationId(), projectId, groupId, path.getStandardPath(),
                minLevelNumberFilter, maxLevelNumberFilter, nameLike);
        return entities.stream().map(WorksheetConverter::convertEntityToMetaResp).collect(Collectors.toList());
    }

    public Page<WorksheetMetaResp> flatListWorksheets(Long projectId, String groupId, Pageable pageable) {
        PreConditions.notNull(projectId, "projectId");
        groupId = WorksheetUtil.fillGroupWithDefault(groupId);
        long organizationId = authenticationFacade.currentOrganizationId();
        long userId = authenticationFacade.currentUserId();
        Page<CollaborationWorksheetEntity> entities =
                worksheetRepository.leftJoinResourceLastAccess(organizationId, projectId, groupId, userId, pageable);
        return new PageImpl<>(entities.stream().map(WorksheetConverter::convertEntityToMetaResp)
                .collect(Collectors.toList()),
                pageable, entities.getTotalElements());
    }

    @Override
    public BatchOperateWorksheetsResp batchUploadWorksheets(Long projectId,
            String groupId, BatchCreateWorksheetsPreProcessor batchCreateWorksheetsPreProcessor) {
        Long organizationId = currentOrganizationId();
        createCheck(currentOrganizationId(), projectId, groupId, batchCreateWorksheetsPreProcessor);
        long userId = currentUserId();
        List<CollaborationWorksheetEntity> entities =
                batchCreateWorksheetsPreProcessor.getCreatePathToObjectIdMap().entrySet()
                        .stream()
                        .map(item -> CollaborationWorksheetEntity.builder()
                                .organizationId(organizationId)
                                .projectId(projectId)
                                .groupId(groupId)
                                .creatorId(userId)
                                .path(item.getKey().getStandardPath())
                                .pathLevel(item.getKey().getLevelNum())
                                .objectId(item.getValue().getObjectId())
                                .extension(item.getKey().getExtension())
                                .size(item.getValue().getSize())
                                .version(0L)
                                .build())
                        .collect(Collectors.toList());

        List<CollaborationWorksheetEntity> results = worksheetRepository.saveAllAndFlush(entities);

        return BatchOperateWorksheetsResp
                .ofSuccess(results.stream().map(WorksheetConverter::convertEntityToMetaResp).collect(
                        Collectors.toList()));
    }

    @Override
    public BatchOperateWorksheetsResp batchDeleteWorksheets(Long projectId, String groupId, List<Path> paths) {
        Long organizationId = currentOrganizationId();
        List<CollaborationWorksheetEntity> deleteWorksheets =
                listWithSubListByProjectIdAndPaths(organizationId, projectId, groupId, paths);
        if (deleteWorksheets.size() > CHANGE_WORKSHEET_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_COUNT,
                    (double) CHANGE_WORKSHEET_NUM_LIMIT, "delete number is over limit " + CHANGE_WORKSHEET_NUM_LIMIT);
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

    @Override
    public List<WorksheetMetaResp> renameWorksheet(Long projectId, String groupId, Path path, Path destinationPath) {
        return moveWorksheet(currentOrganizationId(), projectId, groupId, path, destinationPath, true);
    }

    @Override
    public List<WorksheetMetaResp> editWorksheet(Long projectId, String groupId, Path path,
            String objectId, Long size, Long readVersion) {
        PreConditions.notNull(objectId, "objectId");
        PreConditions.notNull(size, "size");
        PreConditions.notNull(readVersion, "readVersion");
        PreConditions.validArgumentState(path.isFile(), ErrorCodes.Unsupported, null,
                "Unsupported edit path: " + path);

        Optional<CollaborationWorksheetEntity> worksheetOptional =
                worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(
                        currentOrganizationId(), projectId, groupId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithCheckNotFound(projectId, path, worksheetOptional);
        checkVersionConflict(projectId, path, readVersion, worksheet.getVersion());
        doEdit(worksheet, path, size, objectId);

        return Collections.singletonList(WorksheetConverter.convertEntityToMetaResp(worksheet));
    }

    @Override
    public String generateDownloadUrl(Long projectId, String groupId, Path path) {
        path.canGetDownloadUrlCheck();
        Optional<CollaborationWorksheetEntity> worksheetOptional =
                worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(currentOrganizationId(),
                        projectId,
                        groupId, path.getStandardPath());
        CollaborationWorksheetEntity worksheet = getWithCheckNotFound(projectId, path, worksheetOptional);
        PreConditions.notBlank(worksheet.getObjectId(), "objectId");
        if (StringUtils.isEmpty(worksheet.getObjectId())) {
            throw new BadRequestException(ErrorCodes.Unsupported,
                    new Object[] {"Not support downloading empty file!"},
                    "not support downloading empty file,projectId:" + projectId + ",path:" + path);
        }
        return objectStorageClient
                .generateDownloadUrl(worksheet.getObjectId(), WorksheetConstants.MAX_DURATION_DOWNLOAD_SECONDS)
                .toString();
    }

    @Override
    public void downloadPathsToDirectory(Long projectId, String groupId, List<Path> paths, Path commParentPath,
            File destinationDirectory) {
        PreConditions.notEmpty(paths, "paths");
        PreConditions.notNull(destinationDirectory, "destinationDirectory");
        PreConditions.notNull(commParentPath, "commParentPath");
        PreConditions.validArgumentState(destinationDirectory.isDirectory(), ErrorCodes.IllegalArgument, null,
                "destinationDirectory is not directory");
        Long organizationId = currentOrganizationId();
        List<CollaborationWorksheetEntity> downloadWorksheets =
                listWithSubListByProjectIdAndPaths(organizationId, projectId, groupId, paths);
        if (CollectionUtils.isEmpty(downloadWorksheets)) {
            return;
        }
        if (downloadWorksheets.size() > CHANGE_WORKSHEET_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_COUNT,
                    (double) CHANGE_WORKSHEET_NUM_LIMIT, "download number is over limit " + CHANGE_WORKSHEET_NUM_LIMIT);
        }
        for (CollaborationWorksheetEntity worksheet : downloadWorksheets) {
            downloadWorksheetToDirectory(projectId, worksheet, commParentPath, destinationDirectory);
        }
    }

    private void checkPathOrNameLength(Set<Path> createPaths) {
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

    private void checkProjectWorksheetNumberLimit(Long organizationId, Long projectId,
            BatchCreateWorksheetsPreProcessor createWorksheets) {
        long count = worksheetRepository.countByOrganizationIdAndProjectId(organizationId, projectId);
        if (count + createWorksheets.size() > PROJECT_WORKSHEET_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_COUNT_IN_PROJECT,
                    (double) PROJECT_WORKSHEET_NUM_LIMIT,
                    "create path num exceed project worksheet number limit,project id" + projectId
                            + ", create path num: " + createWorksheets.size()
                            + ",project worksheet number: " + count);
        }
    }

    private void createCheck(Long organizationId, Long projectId, String groupId,
            BatchCreateWorksheetsPreProcessor createWorksheets) {
        checkPathOrNameLength(createWorksheets.getCreatePathToObjectIdMap().keySet());
        checkProjectWorksheetNumberLimit(organizationId, projectId, createWorksheets);

        // the exist worksheets number + to create worksheets number can't exceed limit
        Integer levelNumOfToCreatePath = createWorksheets.getParentPath().getLevelNum() + 1;
        long existNum = worksheetRepository.countByPathLikeWithFilter(organizationId, projectId, groupId,
                createWorksheets.getParentPath().getStandardPath(), levelNumOfToCreatePath, levelNumOfToCreatePath,
                null);
        int toCreateNum = createWorksheets.getCreatePathToObjectIdMap().size();
        if (existNum + toCreateNum > SAME_LEVEL_WORKSHEET_NUM_LIMIT) {
            throw new OverLimitException(LimitMetric.WORKSHEET_SAME_LEVEL_COUNT,
                    (double) SAME_LEVEL_WORKSHEET_NUM_LIMIT,
                    "create path num exceed limit, create path num: " + toCreateNum
                            + ", same level exist file num: " + existNum);
        }

        // parent exist,create paths not exist
        List<String> queryPaths = createWorksheets.getCreatePathToObjectIdMap().keySet().stream()
                .map(Path::getStandardPath).collect(Collectors.toList());
        queryPaths.add(createWorksheets.getParentPath().getStandardPath());
        List<CollaborationWorksheetEntity> worksheets =
                worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(
                        organizationId, projectId, groupId, queryPaths);
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

    private long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    private long currentOrganizationId() {
        return authenticationFacade.currentOrganizationId();
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

    /**
     * move {@param movePath} to {@param destinationPath}
     * <p>
     * the situations cannot be moved
     * <ol>
     * <li>movePath= Root/Worksheets/Repos/Git_Repo;</li>
     * <li>destinationPath=Root/Repos;</li>
     * <li>the location between movePath and destinationPath is different;</li>
     * <li>movePath is parent of destinationPath;</li>
     * <li>movePath is same to destinationPath;</li>
     * <li>movePath is directory, destinationPath is file</li>
     * <li>movePath is file, destinationPath is file，and destinationPath has existed</li>
     * <li>movePath is file, destinationPath is directory/Worksheets/Git_Repo，and destinationPath is not
     * exist</li>
     * </ol>
     * 
     * the situations can be moved
     * <ol>
     * <li>movePath is file, destinationPath is file，and destinationPath is not exist,rename movePath to
     * destinationPath</li>
     * <li>movePath is file, destinationPath is directory/Worksheets/Git_Repo, destinationPath has
     * existed,create movePath in destinationPath</li>
     * <li>movePath is directory,destinationPath is directory/Worksheets/Git_Repo;if destinationPath
     * exist,create movePath in destinationPath；if destinationPath not exist(Worksheets/Git_Repo is
     * default exist,so destinationPath only be directory),rename movePath to destinationPath</li>
     * </ol>
     * 
     * @param projectId
     * @param movePath
     * @param destinationPath
     * @return
     */
    public List<WorksheetMetaResp> moveWorksheet(Long organizationId, Long projectId, String groupId,
            Path movePath, Path destinationPath, boolean isRename) {
        checkPathOrNameLength(Collections.singleton(destinationPath));
        WorksheetPathUtil.checkMoveValid(movePath, destinationPath);
        if (isRename) {
            WorksheetPathUtil.checkRenameValid(movePath, destinationPath);
            checkDuplicatedName(organizationId, projectId, groupId, movePath, destinationPath);
        }
        boolean isDestinationExist = destinationPath.isSystemDefine();
        if (!isDestinationExist) {
            isDestinationExist =
                    worksheetRepository
                            .findByOrganizationIdAndProjectIdAndGroupIdAndPath(organizationId, projectId, groupId,
                                    destinationPath.getStandardPath())
                            .isPresent();
        }
        Optional<Path> movedPathOptional = Optional.empty();
        if (isDestinationExist) {
            WorksheetPathUtil.checkMoveValidWithDestinationPathExist(movePath, destinationPath);
            movedPathOptional = movePath.moveWhenDestinationPathExist(movePath, destinationPath);
        } else {
            WorksheetPathUtil.checkMoveValidWithDestinationPathNotExist(movePath, destinationPath);
            movedPathOptional = movePath.moveWhenDestinationPathNotExist(movePath, destinationPath);
        }
        PreConditions.validArgumentState(movedPathOptional.isPresent(),
                ErrorCodes.BadArgument, null,
                "movePath:" + movePath + " move to destinationPath:" + destinationPath + " failed");
        checkDuplicatedName(organizationId, projectId, groupId, movePath, movedPathOptional.get());
        List<CollaborationWorksheetEntity> currentAndSubEntities =
                worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
                        movePath.getStandardPath(), null, null, null);
        checkPathNotFound(projectId, movePath, currentAndSubEntities);

        Set<CollaborationWorksheetEntity> movedEntities =
                doMove(movePath, destinationPath, currentAndSubEntities, isDestinationExist);
        if (CollectionUtils.isEmpty(movedEntities)) {
            return new ArrayList<>();
        }
        worksheetRepository.batchUpdatePath(movedEntities.stream()
                .collect(Collectors.toMap(CollaborationWorksheetEntity::getId, CollaborationWorksheetEntity::getPath)));

        return movedEntities.stream().map(WorksheetConverter::convertEntityToMetaResp)
                .collect(Collectors.toList());
    }

    private void checkPathNotFound(Long projectId, Path opPath,
            List<CollaborationWorksheetEntity> renameAndSubEntities) {
        boolean hasPath = false;
        for (CollaborationWorksheetEntity worksheet : renameAndSubEntities) {
            Path path = new Path(worksheet.getPath());
            if (path.equals(opPath)) {
                hasPath = true;
            }
        }
        if (!hasPath) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path",
                            opPath},
                    "can't find path, projectId:" + projectId + "path:" + opPath);
        }
    }

    private Set<CollaborationWorksheetEntity> doMove(Path path, Path destinationPath,
            List<CollaborationWorksheetEntity> renameAndSubEntities, boolean isDestinationExist) {
        Set<CollaborationWorksheetEntity> changedWorksheets = new HashSet<>();
        if (CollectionUtils.isEmpty(renameAndSubEntities)) {
            return changedWorksheets;
        }
        for (CollaborationWorksheetEntity subFile : renameAndSubEntities) {
            Path subPath = new Path(subFile.getPath());
            Optional<Path> movePathOptional =
                    isDestinationExist ? subPath.moveWhenDestinationPathExist(path, destinationPath)
                            : subPath.moveWhenDestinationPathNotExist(path, destinationPath);
            if (movePathOptional.isPresent()) {
                subFile.setPath(movePathOptional.get().getStandardPath());
                subFile.setPathLevel(movePathOptional.get().getLevelNum());
                subFile.setExtension(movePathOptional.get().getExtension());
                changedWorksheets.add(subFile);

                if (changedWorksheets.size() > CHANGE_WORKSHEET_NUM_LIMIT - 1) {
                    throw new OverLimitException(LimitMetric.WORKSHEET_CHANGE_COUNT,
                            (double) CHANGE_WORKSHEET_NUM_LIMIT,
                            "change num is over limit " + CHANGE_WORKSHEET_NUM_LIMIT);
                }
            }
        }
        return changedWorksheets;
    }

    private void checkDuplicatedName(Long organizationId, Long projectId, String groupId, Path path,
            Path destinationPath) {
        Optional<CollaborationWorksheetEntity> existWorksheets =
                worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndPath(
                        organizationId, projectId, groupId, destinationPath.getStandardPath());
        if (existWorksheets.isPresent()) {
            throw new BadRequestException(ErrorCodes.DuplicatedExists,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "destinationPath", destinationPath},
                    "duplicated path name for rename or move,projectId:" + projectId + ",path:" + path
                            + ",destinationPath:" + destinationPath);
        }
    }

    private CollaborationWorksheetEntity getWithCheckNotFound(Long projectId, Path path,
            Optional<CollaborationWorksheetEntity> worksheetOptional) {
        if (!worksheetOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {ResourceType.ODC_WORKSHEET.getLocalizedMessage(), "path", path},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        return worksheetOptional.get();
    }

    private void checkVersionConflict(Long projectId, Path path, Long readVersion, Long dbVersion) {
        if (readVersion != null && !readVersion.equals(dbVersion)) {
            throw new BadRequestException(ErrorCodes.WorksheetEditVersionConflict,
                    new Object[] {}, "version conflict,current version:" + dbVersion
                            + ",read version:" + readVersion + ",projectId:" + projectId + ",path:" + path);
        }
    }

    private void doEdit(CollaborationWorksheetEntity editWorksheet, Path path, Long size, String objectId) {
        editWorksheet.setObjectId(objectId);
        editWorksheet.setPathLevel(path.getLevelNum());
        editWorksheet.setSize(size);
        int updateCount = worksheetRepository.updateContentByIdAndVersion(editWorksheet);
        if (updateCount == 0) {
            throw new BadRequestException(ErrorCodes.WorksheetEditVersionConflict,
                    new Object[] {}, "version conflict"
                            + ",read version:" + editWorksheet.getVersion() + ",projectId:"
                            + editWorksheet.getProjectId() + ",path:" + path);
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
            File file = WorksheetPathUtil.createFileWithParent(absoluteFile, false);
            try {
                objectStorageClient.downloadToFile(worksheet.getObjectId(), file);
            } catch (IOException e) {
                log.error(
                        "download worksheet to file failed, projectId:{}, worksheet:{},commonParentPath:{},objetId{},"
                                + "filePath:{}",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), absoluteFile);
                throw new InternalServerError(String.format(
                        "download worksheet to file failed, projectId:%d, worksheet:%s,commonParentPath:%s,objetId:%s,"
                                + "filePath:%s",
                        projectId, worksheet, commParentPath, worksheet.getObjectId(), absoluteFile), e);
            }
        } else if (path.isDirectory()) {
            String absoluteFile = destinationDirectory.getAbsolutePath() + relativePathOptional.get();
            WorksheetPathUtil.createFileWithParent(absoluteFile, true);
        }
    }

    private List<CollaborationWorksheetEntity> listWithSubListByProjectIdAndPaths(Long organizationId, Long projectId,
            String groupId, List<Path> paths) {
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
            List<CollaborationWorksheetEntity> worksheets =
                    worksheetRepository.findByOrganizationIdAndProjectIdAndGroupIdAndInPaths(organizationId, projectId,
                            groupId, deleteFilePaths);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                resultWorksheets.addAll(worksheets);
            }

        }
        if (!deleteDirectoryPaths.isEmpty()) {
            Path commonParentPath = WorksheetPathUtil.findCommonPath(deleteDirectoryPaths);
            List<CollaborationWorksheetEntity> worksheets =
                    worksheetRepository.findByPathLikeWithFilter(organizationId, projectId, groupId,
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
