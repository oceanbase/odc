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

import java.util.List;
import java.util.Set;

import com.oceanbase.odc.service.projectfiles.domain.BatchCreateFiles;
import com.oceanbase.odc.service.projectfiles.domain.BatchDeleteFilesResult;
import com.oceanbase.odc.service.projectfiles.domain.Path;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public interface IProjectFilesService {

    ProjectFile createFile(Long projectId, Path createPath, String objectKey);

    ProjectFile getFileDetails(Long projectId, Path path);

    List<ProjectFile> listFiles(Long projectId, Path path);

    List<ProjectFile> searchFiles(Long projectId, String nameLike, int limit);

    List<ProjectFile> batchUploadFiles(Long projectId, BatchCreateFiles batchCreateFiles);

    BatchDeleteFilesResult batchDeleteFiles(Long projectId, Set<Path> paths);

    List<ProjectFile> renameFile(Long projectId, Path path, Path destination);

    List<ProjectFile> editFile(Long projectId, Path path, Path destination, String objectKey, Long readVersion);

    String batchDownloadFiles(Long projectId, Set<String> paths);
}
