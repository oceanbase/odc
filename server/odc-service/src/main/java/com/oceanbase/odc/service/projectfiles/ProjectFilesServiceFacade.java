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

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFileReq;
import com.oceanbase.odc.service.projectfiles.model.FileUploadTempCredentialResp;
import com.oceanbase.odc.service.projectfiles.model.GenerateProjectFileTempCredentialReq;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileMetaResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileResp;
import com.oceanbase.odc.service.projectfiles.model.UpdateProjectFileReq;

/**
 * 项目文件管理服务类
 *
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
@Service
public class ProjectFilesServiceFacade implements IProjectFilesService {
    // @Resource
    // private IProjectFileOssGateway projectFileOssGateway;

    /**
     * 生成临时凭证
     *
     * @param projectId 项目ID
     * @param req 生成临时凭证请求
     * @return 临时凭证响应
     */
    public FileUploadTempCredentialResp generateTempCredential(Long projectId,
            GenerateProjectFileTempCredentialReq req) {
        return null;
        // return projectFileOssGateway.generateTempCredential(req.getDurationSeconds());
    }

    /**
     * 创建文件
     *
     * @param projectId 项目ID
     * @param path 文件路径
     * @param objectKey 对象键
     * @return 文件元数据响应
     */
    @Override
    public ProjectFileMetaResp createFile(Long projectId, String path, String objectKey) {
        return null;
    }

    /**
     * 获取文件详情
     *
     * @param path 文件路径
     * @param projectId 项目ID
     * @return 文件响应
     */
    @Override
    public ProjectFileResp getFileDetails(Long projectId, String path) {
        return null;
    }

    /**
     * 列出文件
     *
     * @param projectId 项目ID
     * @param path 文件路径
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> listFiles(Long projectId, String path) {
        return null;
    }

    /**
     * 搜索文件
     *
     * @param projectId 项目ID
     * @param nameLike 文件名模糊匹配
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> searchFiles(Long projectId, String nameLike) {
        return null;
    }

    /**
     * 批量上传文件
     *
     * @param projectId 项目ID
     * @param req 批量上传文件请求
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> batchUploadFiles(Long projectId, BatchUploadProjectFileReq req) {
        return null;
    }

    /**
     * 批量删除文件
     *
     * @param projectId 项目ID
     * @param paths 文件路径列表
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> batchDeleteFiles(Long projectId, List<String> paths) {
        return null;
    }

    /**
     * 重命名文件
     *
     * @param projectId 项目ID
     * @param path 文件路径
     * @param destination 新文件路径
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> renameFile(Long projectId, String path, String destination) {
        return null;
    }

    /**
     * 编辑文件
     *
     * @param projectId 项目ID
     * @param path 文件路径
     * @param req 编辑文件请求
     * @return 文件元数据列表响应
     */
    @Override
    public List<ProjectFileMetaResp> editFile(Long projectId, String path, UpdateProjectFileReq req) {
        return null;
    }

    /**
     * 批量下载文件
     *
     * @param projectId 项目ID
     * @param paths 文件路径集合
     * @return 下载链接
     */
    @Override
    public String batchDownloadFiles(Long projectId, Set<String> paths) {
        return null;
    }
}
