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
package com.oceanbase.odc.service.projectfiles.factory;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.projectfiles.model.ProjectFileLocation;
import com.oceanbase.odc.service.projectfiles.service.IProjectFilesService;
import com.oceanbase.odc.service.projectfiles.service.NormalProjectFilesService;
import com.oceanbase.odc.service.projectfiles.service.RepoProjectFilesService;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Component
public class ProjectFileServiceFactory {
    @Resource
    private NormalProjectFilesService normalProjectFilesService;
    @Resource
    private RepoProjectFilesService repoProjectFilesService;

    public IProjectFilesService getProjectFileService(ProjectFileLocation location) {
        if (location == null) {
            return normalProjectFilesService;
        }
        if (location == ProjectFileLocation.REPOS) {
            return repoProjectFilesService;
        }
        return normalProjectFilesService;
    }
}
