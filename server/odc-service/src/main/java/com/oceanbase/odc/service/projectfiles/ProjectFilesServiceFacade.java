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
package com.oceanbase.odc.service.projectfiles;

import static com.oceanbase.odc.service.projectfiles.constants.ProjectFilesConstant.PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.projectfiles.converter.ProjectFilesConverter;
import com.oceanbase.odc.service.projectfiles.domain.BatchCreateFiles;
import com.oceanbase.odc.service.projectfiles.domain.BatchDeleteFilesResult;
import com.oceanbase.odc.service.projectfiles.domain.IProjectFileOssGateway;
import com.oceanbase.odc.service.projectfiles.domain.PartitionBatchDeleteFiles;
import com.oceanbase.odc.service.projectfiles.domain.Path;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFilesSearch;
import com.oceanbase.odc.service.projectfiles.factory.ProjectFileServiceFactory;
import com.oceanbase.odc.service.projectfiles.model.BatchDeleteProjectFilesResp;
import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFilesReq;
import com.oceanbase.odc.service.projectfiles.model.FileUploadTempCredentialResp;
import com.oceanbase.odc.service.projectfiles.model.GenerateProjectFileTempCredentialReq;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileLocation;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileMetaResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileResp;
import com.oceanbase.odc.service.projectfiles.model.UpdateProjectFileReq;
import com.oceanbase.odc.service.projectfiles.service.IProjectFilesService;

/**
 * 项目文件管理服务类
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Service
public class ProjectFilesServiceFacade {

    @Resource
    private IProjectFileOssGateway projectFileOssGateway;

    @Resource
    private ProjectFileServiceFactory projectFileServiceFactory;

    /**
     * 生成临时凭证
     *
     * @param projectId 项目ID
     * @param req 生成临时凭证请求
     * @return 临时凭证响应
     */
    public FileUploadTempCredentialResp generateTempCredential(Long projectId,
            GenerateProjectFileTempCredentialReq req) {
        return projectFileOssGateway.generateTempCredential(req.getDurationSeconds());
    }

    /**
     * 创建文件
     *
     * @param projectId 项目ID
     * @param path 文件路径
     * @param objectKey 对象键
     * @return 文件元数据响应
     */
    public ProjectFileMetaResp createFile(Long projectId, String path, String objectKey) {
        Path createPath = new Path(path);
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                createPath.getLocation());
        ProjectFile file = projectFileService.createFile(projectId, createPath, objectKey);
        return ProjectFilesConverter.convertToMetaResp(file);
    }

    /**
     * 获取文件详情
     *
     * @param pathStr 文件路径
     * @param projectId 项目ID
     * @return 文件响应
     */
    public ProjectFileResp getFileDetails(Long projectId, String pathStr) {
        Path path = new Path(pathStr);
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                path.getLocation());
        ProjectFile file = projectFileService.getFileDetails(projectId, path);
        return ProjectFilesConverter.convertToResp(file);
    }

    /**
     * 列出文件
     *
     * @param projectId 项目ID
     * @param pathStr 文件路径
     * @return 文件元数据列表响应
     */
    public List<ProjectFileMetaResp> listFiles(Long projectId, String pathStr) {
        Path path = new Path(pathStr);
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                path.getLocation());
        List<ProjectFile> projectFiles = projectFileService.listFiles(projectId, path);
        return projectFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .collect(Collectors.toList());
    }

    /**
     * 搜索文件
     *
     * @param projectId 项目ID
     * @param nameLike 文件名模糊匹配
     * @return 文件元数据列表响应
     */
    public List<ProjectFileMetaResp> searchFiles(Long projectId, String nameLike) {
        if (StringUtils.isBlank(nameLike)) {
            return new ArrayList<>();
        }
        IProjectFilesService repoProjectFileService =
                projectFileServiceFactory.getProjectFileService(ProjectFileLocation.REPOS);
        IProjectFilesService normalProjectFileService =
                projectFileServiceFactory.getProjectFileService(ProjectFileLocation.WORKSHEETS);
        ProjectFilesSearch projectFilesSearch = new ProjectFilesSearch(nameLike,
                normalProjectFileService.searchFiles(projectId, nameLike, PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT),
                repoProjectFileService.searchFiles(projectId, nameLike, PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT));
        List<ProjectFile> projectFiles = projectFilesSearch.searchByNameLike(nameLike);
        return projectFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .limit(PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * 批量上传文件
     *
     * @param projectId 项目ID
     * @param req 批量上传文件请求
     * @return 文件元数据列表响应
     */
    public List<ProjectFileMetaResp> batchUploadFiles(Long projectId, BatchUploadProjectFilesReq req) {
        BatchCreateFiles batchCreateFiles = new BatchCreateFiles(req);
        Path parentPath = batchCreateFiles.getParentPath();
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                parentPath.getLocation());
        List<ProjectFile> projectFiles = projectFileService.batchUploadFiles(projectId, batchCreateFiles);
        return projectFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .collect(Collectors.toList());
    }

    /**
     * 批量删除文件
     *
     * @param projectId 项目ID
     * @param paths 文件路径列表
     * @return 文件元数据列表响应
     */

    public BatchDeleteProjectFilesResp batchDeleteFiles(Long projectId, List<String> paths) {
        PartitionBatchDeleteFiles partitionBatchDeleteFiles = new PartitionBatchDeleteFiles(paths);

        BatchDeleteFilesResult batchDeleteFilesResult = new BatchDeleteFilesResult();
        if (CollectionUtils.isNotEmpty(partitionBatchDeleteFiles.getNormalPaths())) {

            batchDeleteFilesResult.addResult(
                    projectFileServiceFactory.getProjectFileService(ProjectFileLocation.WORKSHEETS)
                            .batchDeleteFiles(projectId, partitionBatchDeleteFiles.getNormalPaths()));
        }
        if (CollectionUtils.isNotEmpty(partitionBatchDeleteFiles.getReposPaths())) {
            batchDeleteFilesResult.addResult(
                    projectFileServiceFactory.getProjectFileService(ProjectFileLocation.REPOS)
                            .batchDeleteFiles(projectId, partitionBatchDeleteFiles.getReposPaths()));
        }
        return ProjectFilesConverter.convertToBatchDeleteResp(batchDeleteFilesResult);
    }

    /**
     * 重命名文件
     *
     * @param projectId 项目ID
     * @param pathStr 文件路径
     * @param destination 新文件路径
     * @return 文件元数据列表响应
     */
    public List<ProjectFileMetaResp> renameFile(Long projectId, String pathStr, String destination) {
        Path path = new Path(pathStr);
        Path destPath = new Path(destination);
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                path.getLocation());
        List<ProjectFile> projectFiles = projectFileService.renameFile(projectId, path, destPath);
        return projectFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .collect(Collectors.toList());
    }

    /**
     * 编辑文件
     *
     * @param projectId 项目ID
     * @param pathStr 文件路径
     * @param req 编辑文件请求
     * @return 文件元数据列表响应
     */
    public List<ProjectFileMetaResp> editFile(Long projectId, String pathStr, UpdateProjectFileReq req) {
        Path path = new Path(pathStr);
        Path destPath = new Path(req.getDestination());
        IProjectFilesService projectFileService = projectFileServiceFactory.getProjectFileService(
                path.getLocation());
        List<ProjectFile> projectFiles =
                projectFileService.editFile(projectId, path, destPath, req.getObjectKey(), req.getVersion());
        return projectFiles.stream()
                .map(ProjectFilesConverter::convertToMetaResp)
                .collect(Collectors.toList());
    }

    /**
     * 批量下载文件
     *
     * @param projectId 项目ID
     * @param paths 文件路径集合
     * @return 下载链接
     */
    public String batchDownloadFiles(Long projectId, Set<String> paths) {
        return null;
    }
}
