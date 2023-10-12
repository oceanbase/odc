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

package com.oceanbase.odc.service.permissionapply.project;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @author gaoda.xy
 * @date 2023/10/12 12:00
 */
@FlowTaskPreprocessor(type = TaskType.PERMISSION_APPLY_PROJECT)
public class ApplyProjectPreprocessor implements Preprocessor {

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ApplyProjectParameter parameter = (ApplyProjectParameter) req.getParameters();
        Verify.notNull(parameter.getProjectId(), "projectId");
        Verify.notEmpty(parameter.getResourceRoleIds(), "resourceRoleIds");
        Verify.notBlank(parameter.getApplyReason(), "applyReason");
        checkProjectExistAndValid(parameter.getProjectId());
        checkResourceRoleExist(parameter.getResourceRoleIds());
        parameter.setUserId(authenticationFacade.currentUserId());
        req.setProjectId(parameter.getProjectId());
    }

    private void checkProjectExistAndValid(Long projectId) {
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        if (projectEntity.getOrganizationId() != authenticationFacade.currentOrganizationId()) {
            throw new NotFoundException(ResourceType.ODC_PROJECT, "id", projectId);
        }
    }

    private void checkResourceRoleExist(List<Long> resourceRoleIds) {
        if (CollectionUtils.isEmpty(resourceRoleIds)) {
            return;
        }
        for (Long resourceRoleId : resourceRoleIds) {
            resourceRoleService.findResourceRoleById(resourceRoleId)
                    .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RESOURCE_ROLE, "id", resourceRoleId));
        }
    }

}
