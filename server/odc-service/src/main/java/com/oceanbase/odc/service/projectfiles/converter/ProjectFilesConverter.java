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
package com.oceanbase.odc.service.projectfiles.converter;

import java.util.stream.Collectors;

import com.oceanbase.odc.service.projectfiles.domain.BatchDeleteFilesResult;
import com.oceanbase.odc.service.projectfiles.domain.ProjectFile;
import com.oceanbase.odc.service.projectfiles.model.BatchDeleteProjectFilesResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileMetaResp;
import com.oceanbase.odc.service.projectfiles.model.ProjectFileResp;

/**
 * @author keyang
 * @date 2024/08/02
 * @since 4.3.2
 */
public class ProjectFilesConverter {
    public static ProjectFileMetaResp convertToMetaResp(ProjectFile projectFile) {
        return ProjectFileMetaResp.builder().projectId(projectFile.getProjectId())
                .path(projectFile.getPath().getStandardPath())
                .type(projectFile.getPath().getType())
                .createTime(projectFile.getCreateTime())
                .updateTime(projectFile.getUpdateTime())
                .build();
    }

    public static ProjectFileResp convertToResp(ProjectFile projectFile) {
        return ProjectFileResp.builder()
                .version(projectFile.getVersion())
                .content(projectFile.getContent())
                .projectId(projectFile.getProjectId())
                .path(projectFile.getPath().getStandardPath())
                .type(projectFile.getPath().getType())
                .createTime(projectFile.getCreateTime())
                .updateTime(projectFile.getUpdateTime())
                .build();
    }

    public static BatchDeleteProjectFilesResp convertToBatchDeleteResp(BatchDeleteFilesResult result) {
        return BatchDeleteProjectFilesResp.builder()
                .isAllDeleted(result.getFailed().isEmpty())
                .successFiles(result.getSuccess().stream()
                        .map(ProjectFilesConverter::convertToMetaResp).collect(Collectors.toList()))
                .failedFiles(result.getFailed().stream()
                        .map(ProjectFilesConverter::convertToMetaResp).collect(Collectors.toList()))
                .build();
    }
}
