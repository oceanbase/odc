/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.projectfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.projectfiles.converter.ProjectFilesConverter;
import com.oceanbase.odc.service.projectfiles.domain.INormalProjectFilesRepository;
import com.oceanbase.odc.service.projectfiles.domain.IProjectFileOssGateway;
import com.oceanbase.odc.service.projectfiles.domain.Path;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;
import com.oceanbase.odc.service.projectfiles.exceptions.NameDuplicatedException;
import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFileReq;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileMetaResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileResp;
import com.oceanbase.odc.service.projectfiles.model.UpdateProjectFileReq;

/**
 * Worksheets下的文件的处理
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Service
public class NormalProjectFilesService implements IProjectFilesService {
    @Resource
    private INormalProjectFilesRepository normalProjectFilesRepository;
    @Resource
    private IProjectFileOssGateway projectFileOssGateway;


    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ProjectFileMetaResp createFile(Long projectId, String path, String objectKey) {
        Path addPath = new Path(path);

        Optional<Path> parentPath = addPath.getParentPath();
        if (!parentPath.isPresent()) {
            throw new IllegalArgumentException("invalid path, projectId:" + projectId + "path:" + path + ",objectKey:"
                    + objectKey);
        }
        Optional<ProjectFile> parentFileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, parentPath.get(),
                        true, true, true, false);
        PreConditions.assertTrue(parentFileOptional.isPresent(), "parentPath");
        if (!parentFileOptional.get().canCreateSubFile(addPath)) {
            throw new NameDuplicatedException(
                    "duplicated path for create file, projectId:" + projectId + "path:" + path + ",objectKey:"
                            + objectKey);
        }

        ProjectFile projectFile = ProjectFile.create(projectId, addPath, objectKey);
        normalProjectFilesRepository.add(projectFile);
        if (addPath.isFile()) {
            projectFileOssGateway.copyTo(objectKey, addPath);
        }
        return ProjectFilesConverter.convertToMetaResp(projectFile);
    }

    @Override
    public ProjectFileResp getFileDetails(Long projectId, String path) {
        Path detailPath = new Path(path);
        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, detailPath,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }

        ProjectFile projectFile = fileOptional.get();
        if (detailPath.isFile() && StringUtils.isNotBlank(projectFile.getObjectKey())) {
            projectFile.setContent(projectFileOssGateway.getContent(fileOptional.get().getObjectKey()));
        }

        return ProjectFilesConverter.convertToResp(projectFile);
    }

    @Override
    public List<ProjectFileMetaResp> listFiles(Long projectId, String path) {
        Path detailPath = new Path(path);

        Optional<ProjectFile> fileOptional =
                normalProjectFilesRepository.findByProjectAndPath(projectId, detailPath,
                        false, false, false, false);
        if (!fileOptional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"path"},
                    "can't find path, projectId:" + projectId + "path:" + path);
        }
        ProjectFile projectFile = fileOptional.get();

        List<ProjectFile> nextLevelFiles = projectFile.getNextLevelFiles();
        return nextLevelFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectFileMetaResp> searchFiles(Long projectId, String nameLike) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFileMetaResp> batchUploadFiles(Long projectId, BatchUploadProjectFileReq req) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFileMetaResp> batchDeleteFiles(Long projectId, List<String> paths) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFileMetaResp> renameFile(Long projectId, String path, String destination) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFileMetaResp> editFile(Long projectId, String path, UpdateProjectFileReq req) {
        return Collections.emptyList();
    }

    @Override
    public String batchDownloadFiles(Long projectId, Set<String> paths) {
        return "";
    }
}
