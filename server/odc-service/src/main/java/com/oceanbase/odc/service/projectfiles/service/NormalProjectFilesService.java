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
package com.oceanbase.odc.service.projectfiles.service;

import static com.oceanbase.odc.service.projectfiles.constants.ProjectFilesConstant.CHANGE_FILE_NUM_LIMIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Iterables;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.projectfiles.domain.BatchCreateFiles;
import com.oceanbase.odc.service.projectfiles.domain.BatchDeleteFilesResult;
import com.oceanbase.odc.service.projectfiles.domain.INormalProjectFilesRepository;
import com.oceanbase.odc.service.projectfiles.domain.IProjectFileOssGateway;
import com.oceanbase.odc.service.projectfiles.domain.Path;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;
import com.oceanbase.odc.service.projectfiles.exceptions.ChangeFileTooMuchLongException;

import lombok.extern.slf4j.Slf4j;

/**
 * Worksheets下的文件的处理
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Slf4j
@Service
public class NormalProjectFilesService implements IProjectFilesService {
    @Resource
    private INormalProjectFilesRepository normalProjectFilesRepository;
    @Resource
    private IProjectFileOssGateway projectFileOssGateway;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ProjectFile createFile(Long projectId, Path createPath, String objectKey) {
        Optional<Path> parentPath = createPath.getParentPath();
        if (!parentPath.isPresent()) {
            throw new IllegalArgumentException(
                    "invalid path, projectId:" + projectId + "path:" + createPath + ",objectKey:"
                            + objectKey);
        }
        Optional<ProjectFile> parentFileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, parentPath.get(),
                        true, true, true, false);

        // 这里不会出现parentFileOptional为空的情况
        ProjectFile prarentprojectFile =
                parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + "parent path:" + parentPath));
        ProjectFile createdProjectFile = prarentprojectFile.create(createPath, objectKey);
        normalProjectFilesRepository.batchAdd(Collections.singleton(createdProjectFile));
        if (createdProjectFile.getPath().isFile()) {
            projectFileOssGateway.copyTo(objectKey, createPath);
        }
        return createdProjectFile;
    }

    @Override
    public ProjectFile getFileDetails(Long projectId, Path path) {
        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }

        ProjectFile projectFile = fileOptional.get();
        if (path.isFile() && StringUtils.isNotBlank(projectFile.getObjectKey())) {
            projectFile.setContent(projectFileOssGateway.getContent(fileOptional.get().getObjectKey()));
        }

        return projectFile;
    }

    @Override
    public List<ProjectFile> listFiles(Long projectId, Path path) {
        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        ProjectFile projectFile = fileOptional.get();

        return projectFile.getNextLevelFiles();
    }

    @Override
    public List<ProjectFile> searchFiles(Long projectId, String nameLike, int limit) {
        if (StringUtils.isBlank(nameLike)) {
            return new ArrayList<>();
        }
        return normalProjectFilesRepository.listByProjectIdAndPathNameLike(projectId, nameLike, limit);
    }

    @Override
    public List<ProjectFile> batchUploadFiles(Long projectId, BatchCreateFiles batchCreateFiles) {
        Set<ProjectFile> createProjectFiles = transactionTemplate.execute(status -> {
            Optional<ProjectFile> parentFileOptional =
                    normalProjectFilesRepository.findByProjectAndPath(projectId,
                            batchCreateFiles.getParentPath(),
                            true, true, true, false);

            // 这里不会出现parentFileOptional为空的情况
            ProjectFile parentProjectFile =
                    parentFileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                            + projectId + "parent path:" + batchCreateFiles.getParentPath()));
            Set<ProjectFile> innerProjectFiles = parentProjectFile.batchCreate(
                    batchCreateFiles.getCreatePathToObjectKeyMap());
            normalProjectFilesRepository.batchAdd(innerProjectFiles);
            return innerProjectFiles;
        });
        if (CollectionUtils.isEmpty(createProjectFiles)) {
            return new ArrayList<>();
        }
        for (ProjectFile projectFile : createProjectFiles) {
            if (projectFile.getPath().isFile()) {
                try {
                    projectFileOssGateway.copyTo(projectFile.getObjectKey(), projectFile.getPath());
                } catch (Exception e) {
                    log.error("copy file to path failed, projectId:{}, path:{}, objectKey:{}", projectId,
                            projectFile.getPath(),
                            projectFile.getObjectKey());
                }
            }
        }
        return new ArrayList<>(createProjectFiles);
    }

    @Override
    public BatchDeleteFilesResult batchDeleteFiles(Long projectId, Set<Path> paths) {
        List<ProjectFile> deleteFiles = new ArrayList<>();
        for (Path path : paths) {
            List<ProjectFile> files =
                    normalProjectFilesRepository.listByProjectIdAndPath(projectId, path, true);
            deleteFiles.addAll(files);
        }
        if (deleteFiles.size() > CHANGE_FILE_NUM_LIMIT) {
            throw new ChangeFileTooMuchLongException("delete num is over limit " + CHANGE_FILE_NUM_LIMIT);
        }
        if (CollectionUtils.isEmpty(deleteFiles)) {
            return new BatchDeleteFilesResult();
        }
        int batchSize = 200;
        BatchDeleteFilesResult result = new BatchDeleteFilesResult();
        Iterables.partition(deleteFiles, batchSize).forEach(files -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    normalProjectFilesRepository.batchDelete(deleteFiles.stream()
                            .map(ProjectFile::getId).collect(Collectors.toSet()));
                    projectFileOssGateway.batchDelete(files.stream()
                            .map(ProjectFile::getObjectKey).collect(Collectors.toSet()));
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
    public List<ProjectFile> renameFile(Long projectId, Path path, Path destination) {
        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        true, true, true, true);
        ProjectFile projectFile =
                fileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<ProjectFile> renamedProjectFiles = projectFile.rename(destination);
        normalProjectFilesRepository.batchUpdateById(renamedProjectFiles, false);

        return new ArrayList<>(renamedProjectFiles);
    }

    @Override
    public List<ProjectFile> editFile(Long projectId, Path path, Path destination,
            String objectKey, Long readVersion) {
        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, path,
                        true, true, true, true);
        ProjectFile projectFile =
                fileOptional.orElseThrow(() -> new IllegalStateException("unexpected exception,projectId:"
                        + projectId + " path:" + path));

        Set<ProjectFile> editedProjectFiles = projectFile.edit(destination, objectKey, readVersion);
        normalProjectFilesRepository.batchUpdateById(editedProjectFiles, true);

        return new ArrayList<>(editedProjectFiles);
    }

    @Override
    public String batchDownloadFiles(Long projectId, Set<String> paths) {
        return "";
    }
}
