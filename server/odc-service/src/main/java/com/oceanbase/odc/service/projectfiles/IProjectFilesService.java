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

import java.util.List;
import java.util.Set;

import com.oceanbase.odc.service.projectfiles.model.BatchUploadProjectFileReq;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileMetaResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileResp;
import com.oceanbase.odc.service.projectfiles.model.UpdateProjectFileReq;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public interface IProjectFilesService {

    ProjectFileMetaResp createFile(Long projectId, String path, String objectKey);

    ProjectFileResp getFileDetails(Long projectId, String path);

    List<ProjectFileMetaResp> listFiles(Long projectId, String path);

    List<ProjectFileMetaResp> searchFiles(Long projectId, String nameLike);

    List<ProjectFileMetaResp> batchUploadFiles(Long projectId, BatchUploadProjectFileReq req);

    List<ProjectFileMetaResp> batchDeleteFiles(Long projectId, List<String> paths);

    List<ProjectFileMetaResp> renameFile(Long projectId, String path, String destination);

    List<ProjectFileMetaResp> editFile(Long projectId, String path, UpdateProjectFileReq req);

    String batchDownloadFiles(Long projectId, Set<String> paths);
}
