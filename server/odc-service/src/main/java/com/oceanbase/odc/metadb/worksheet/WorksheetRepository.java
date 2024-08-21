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
package com.oceanbase.odc.metadb.worksheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataRepository;
import com.oceanbase.odc.metadb.worksheet.converter.WorksheetConverter;
import com.oceanbase.odc.service.objectstorage.model.ObjectUploadStatus;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.domain.WorksheetId;
import com.oceanbase.odc.service.worksheet.utils.WorksheetPathUtil;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author keyang
 * @date 2024/08/12
 * @since 4.3.2
 */
@Slf4j
@Component
public class WorksheetRepository {
    @Autowired
    private ObjectMetadataRepository metadataRepository;

    /**
     * find worksheet info of {@param path} in {@param projectId},and also find sub-worksheets of
     * {@param path}.
     * <p>
     * if worksheet of {@param path} not exist ,but its sub-worksheets are not empty，return a temp
     * worksheet with worksheet#id is null.
     * 
     * @param projectId project id
     * @param path the path of worksheet to query
     * @param nameLike if nameLike is not blank,the worksheets that path cannot fuzzy match nameLike
     *        will be excluded from the sub-worksheets
     * @return the return value must not be empty，returns temp when unable to find the worksheet of the
     *         {@param path}, otherwise returns a normal worksheet
     */
    public Worksheet findWithSubListByProjectIdAndPathAndNameLike(Long projectId, Path path,
            String nameLike) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(path, "path");
        List<ObjectMetadataEntity> entities = null;
        if (StringUtils.isNotBlank(nameLike)) {
            entities = metadataRepository.findAllByBucketNameAndObjectNameLeftLikeAndNameLikeAndStatus(
                    WorksheetUtil.getBucketNameOfWorkSheets(projectId), path.getStandardPath(), nameLike,
                    ObjectUploadStatus.FINISHED);
        } else {
            entities = metadataRepository.findAllByBucketNameAndObjectNameLeftLikeAndStatus(
                    WorksheetUtil.getBucketNameOfWorkSheets(projectId), path.getStandardPath(),
                    ObjectUploadStatus.FINISHED);
        }
        if (CollectionUtils.isEmpty(entities)) {
            return Worksheet.ofTemp(projectId, path);
        }
        return WorksheetConverter.toDomainFromEntities(entities, projectId, path,
                true, true, false);
    }

    /**
     * find worksheet info of {@param path} in {@param projectId},and also find sub-worksheets of
     * {@param path},and also find the worksheets that are children of the direct parent {@param path}.
     * <p>
     * if worksheet of {@param path} not exist,but its sub-worksheets are not empty，return a temp
     * worksheet with worksheet#id is null.
     * <p>
     * It should be noted that,this method will add an exclusive lock on the queried {@param path}
     * worksheet (and all its sub-worksheets). Using this method to query and update in transactions can
     * ensure that there will be no concurrency issues when updating the {@param path} worksheet or its
     * sub-worksheets.
     * <p>
     * for example,In the scenario of updating worksheet name,this method can be used to add exclusive
     * locks to the parent worksheet(and it's sub-worksheets); In the same transaction, after the
     * current method, check whether the name for renaming has been used and perform the renaming
     * operation. Even if there is a concurrent renaming request coming, it will be blocked when calling
     * the current query method until the update of the previous request is completed (exclusive lock is
     * released).
     *
     * @param projectId project id
     * @param path the path of worksheet to query
     * @return the return value must not be empty，returns temp when unable to find the worksheet of the
     *         {@param path}, otherwise returns a normal worksheet
     */
    public Worksheet findWithSubListAndSameDirectParentListByProjectIdAndPathWithLock(Long projectId, Path path) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(path, "path");
        List<ObjectMetadataEntity> entities =
                metadataRepository.findAllByBucketNameAndObjectNameLeftLikeAndStatusWithLock(
                        WorksheetUtil.getBucketNameOfWorkSheets(projectId), path.getStandardPath(),
                        ObjectUploadStatus.FINISHED);

        if (CollectionUtils.isEmpty(entities)) {
            return Worksheet.ofTemp(projectId, path);
        }
        return WorksheetConverter.toDomainFromEntities(entities, projectId, path,
                true, true, true);
    }

    /**
     * find worksheet of {@param path},not get sub-worksheets.
     * 
     * @param projectId project id
     * @param path the path of worksheet to query
     * @return returns empty when unable to find the worksheet of the {@param path}, otherwise returns a
     *         nonempty worksheet
     */
    public Optional<Worksheet> findByProjectIdAndPath(Long projectId, Path path) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(path, "path");
        List<ObjectMetadataEntity> entities =
                metadataRepository.findAllByBucketNameAndObjectNameAndStatus(
                        WorksheetUtil.getBucketNameOfWorkSheets(projectId), path.getStandardPath(),
                        ObjectUploadStatus.FINISHED);

        if (CollectionUtils.isEmpty(entities)) {
            return Optional.empty();
        }
        return Optional.ofNullable(WorksheetConverter.toDomainFromEntities(entities, projectId, path,
                false, false, false));
    }

    public List<Worksheet> listByProjectIdAndInPaths(Long projectId, List<Path> paths) {
        PreConditions.notNull(projectId, "projectId");
        if (CollectionUtils.isEmpty(paths)) {
            return Collections.emptyList();
        }
        List<ObjectMetadataEntity> resultList = metadataRepository.findByProjectIdAndNames(
                WorksheetUtil.getBucketNameOfWorkSheets(projectId), paths.stream().map(Path::getStandardPath).collect(
                        Collectors.toList()));
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        return resultList.stream().map(WorksheetConverter::toDomain).collect(Collectors.toList());
    }

    public List<Worksheet> listWithSubListByProjectIdAndPath(Long projectId, Path path) {
        PreConditions.notNull(path, "projectId");
        PreConditions.notNull(path, "path");
        List<ObjectMetadataEntity> entities = metadataRepository.findAllByBucketNameAndObjectNameLeftLikeAndStatus(
                WorksheetUtil.getBucketNameOfWorkSheets(projectId), path.getStandardPath(),
                ObjectUploadStatus.FINISHED);;
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return entities.stream().map(WorksheetConverter::toDomain).collect(Collectors.toList());
    }

    public List<Worksheet> listWithSubListByProjectIdAndPaths(Long projectId, List<Path> paths) {
        List<Worksheet> resultWorksheets = new ArrayList<>();
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
            List<Worksheet> worksheets = this.listByProjectIdAndInPaths(projectId,
                    deleteFilePaths);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                resultWorksheets.addAll(worksheets);
            }

        }
        if (!deleteDirectoryPaths.isEmpty()) {
            Path commonParentPath = WorksheetPathUtil.findCommonPath(deleteDirectoryPaths);
            List<Worksheet> worksheets = this.listWithSubListByProjectIdAndPath(projectId,
                    commonParentPath);
            if (CollectionUtils.isNotEmpty(worksheets)) {
                Path[] deleteDirectoryArr = deleteDirectoryPaths.toArray(new Path[0]);
                for (Worksheet worksheet : worksheets) {
                    if (deleteDirectoryPaths.contains(worksheet.getPath()) ||
                            worksheet.getPath().isChildOfAny(deleteDirectoryArr)) {
                        resultWorksheets.add(worksheet);
                    }
                }
            }
        }
        return resultWorksheets;
    }

    public void batchAdd(Set<Worksheet> worksheets) {
        if (CollectionUtils.isEmpty(worksheets)) {
            return;
        }
        List<ObjectMetadataEntity> entities =
                worksheets.stream().map(WorksheetConverter::toEntity).collect(Collectors.toList());
        metadataRepository.saveAllAndFlush(entities);
        setEntityIdToDomain(worksheets, entities);
    }

    private static void setEntityIdToDomain(Set<Worksheet> worksheets, List<ObjectMetadataEntity> entities) {
        Map<WorksheetId, Long> worksheetIdToRowIdMap = entities.stream().collect(
                Collectors.toMap(o -> new WorksheetId(o.getBucketName(), o.getObjectName()),
                        ObjectMetadataEntity::getId, (o1, o2) -> o1));
        for (Worksheet worksheet : worksheets) {
            worksheet.setId(worksheetIdToRowIdMap.get(new WorksheetId(worksheet.getProjectId(), worksheet.getPath())));
        }
    }

    public void batchDelete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        metadataRepository.deleteAllById(ids);
    }

    public void batchUpdateById(Set<Worksheet> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        List<ObjectMetadataEntity> entities =
                files.stream().map(WorksheetConverter::toEntity).collect(Collectors.toList());
        metadataRepository.saveAllAndFlush(entities);
    }
}
