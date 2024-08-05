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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.service.projectfiles.domain.BatchCreateFiles;
import com.oceanbase.odc.service.projectfiles.domain.BatchDeleteFilesResult;
import com.oceanbase.odc.service.projectfiles.domain.Path;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;

/**
 * Worksheets下的文件的处理
 *
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
@Service
public class RepoProjectFilesService implements IProjectFilesService {

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ProjectFile createFile(Long projectId, Path createPath, String objectKey) {
        return null;
    }

    @Override
    public ProjectFile getFileDetails(Long projectId, Path path) {

        return null;
    }

    @Override
    public List<ProjectFile> listFiles(Long projectId, Path path) {

        return new ArrayList<>();
    }

    @Override
    public List<ProjectFile> searchFiles(Long projectId, String nameLike, int limit) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFile> batchUploadFiles(Long projectId, BatchCreateFiles batchCreateFiles) {
        return Collections.emptyList();
    }

    @Override
    public BatchDeleteFilesResult batchDeleteFiles(Long projectId, Set<Path> paths) {
        return new BatchDeleteFilesResult();
    }

    @Override
    public List<ProjectFile> renameFile(Long projectId, Path path, Path destination) {
        return Collections.emptyList();
    }

    @Override
    public List<ProjectFile> editFile(Long projectId, Path path, Path destination, String objectKey, Long readVersion) {
        return Collections.emptyList();
    }

    @Override
    public String batchDownloadFiles(Long projectId, Set<String> paths) {
        return "";
    }
}
