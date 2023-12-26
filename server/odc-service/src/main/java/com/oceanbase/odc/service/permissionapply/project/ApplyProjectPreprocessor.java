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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permissionapply.project.ApplyProjectParameter.ApplyResourceRole;

/**
 * @author gaoda.xy
 * @date 2023/10/12 12:00
 */
@FlowTaskPreprocessor(type = TaskType.APPLY_PROJECT_PERMISSION)
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
        Verify.notNull(parameter.getProject(), "project");
        Verify.notEmpty(parameter.getResourceRoles(), "resourceRole");
        Verify.notNull(parameter.getApplyReason(), "applyReason");
        ProjectEntity projectEntity = checkProjectExistAndValid(parameter.getProject().getId());
        List<ResourceRoleEntity> resourceRoleEntities = checkResourceRoleExist(
                parameter.getResourceRoles().stream().map(ApplyResourceRole::getId).collect(Collectors.toList()));
        parameter.setUserId(authenticationFacade.currentUserId());
        parameter.getProject().setName(projectEntity.getName());
        for (int index = 0; index < parameter.getResourceRoles().size(); index++) {
            parameter.getResourceRoles().get(index).setName(resourceRoleEntities.get(index).getRoleName());
        }
        req.setProjectId(projectEntity.getId());
        req.setProjectName(projectEntity.getName());
        Locale locale = LocaleContextHolder.getLocale();
        String i18nKey = "com.oceanbase.odc.builtin-resource.permission-apply.project.description";
        req.setDescription(I18n.translate(
                i18nKey,
                new Object[] {projectEntity.getName(),
                        resourceRoleEntities.stream().map(r -> r.getRoleName().getLocalizedMessage())
                                .collect(Collectors.joining(","))},
                locale));
    }

    private ProjectEntity checkProjectExistAndValid(Long projectId) {
        ProjectEntity projectEntity = projectService.nullSafeGet(projectId);
        if (projectEntity.getOrganizationId() != authenticationFacade.currentOrganizationId()) {
            throw new NotFoundException(ResourceType.ODC_PROJECT, "id", projectId);
        }
        return projectEntity;
    }

    private List<ResourceRoleEntity> checkResourceRoleExist(List<Long> resourceRoleIds) {
        List<ResourceRoleEntity> resourceRoleEntities = new ArrayList<>();
        if (CollectionUtils.isEmpty(resourceRoleIds)) {
            return resourceRoleEntities;
        }
        for (Long resourceRoleId : resourceRoleIds) {
            resourceRoleEntities.add(resourceRoleService.findResourceRoleById(resourceRoleId)
                    .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RESOURCE_ROLE, "id", resourceRoleId)));
        }
        return resourceRoleEntities;
    }

}
