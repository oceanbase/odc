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

package com.oceanbase.odc.service.onlineschemachange.subtask;

import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-18
 * @since 4.2.0
 */
@Slf4j
@Component
public class OmsResourceCleanHandler {

    @Autowired
    private ProjectOpenApiService projectOpenApiService;

    public boolean checkAndReleaseProject(ProjectControlRequest projectControl) {
        if (projectControl.getId() == null) {
            return true;
        }

        boolean released = false;
        // todo project is not exists
        ProjectControlRequest controlRequest = new ProjectControlRequest();
        controlRequest.setId(projectControl.getId());
        controlRequest.setUid(projectControl.getUid());
        ProjectProgressResponse progressResponse = projectOpenApiService.describeProjectProgress(controlRequest);
        if (progressResponse.getStatus() == ProjectStatusEnum.RELEASED ||
                progressResponse.getStatus() == ProjectStatusEnum.RELEASING ||
                progressResponse.getStatus() == ProjectStatusEnum.DELETED) {
            released = true;
        }
        if (!released) {
            log.info("Oms project {} has not released, try to release it.", projectControl.getId());
            doReleaseOmsResource(controlRequest);
        }
        return released;
    }

    private void doReleaseOmsResource(ProjectControlRequest projectControlRequest) {
        if (projectControlRequest.getId() == null) {
            return;
        }
        Executors.newSingleThreadExecutor().submit(() -> {
            try {

                ProjectProgressResponse response = projectOpenApiService.describeProjectProgress(projectControlRequest);
                if (response.getStatus() == ProjectStatusEnum.RUNNING) {
                    projectOpenApiService.stopProject(projectControlRequest);
                }
                projectOpenApiService.releaseProject(projectControlRequest);
                log.info("Release oms project, id {}", projectControlRequest.getId());
            } catch (Throwable ex) {
                log.warn("Failed to release oms project, id {}, occur error {}", projectControlRequest.getId(),
                        ex.getMessage());
            }
        });

    }

}
