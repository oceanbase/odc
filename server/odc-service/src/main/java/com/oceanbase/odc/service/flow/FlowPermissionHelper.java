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
package com.oceanbase.odc.service.flow;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @Author: Lebie
 * @Date: 2024/10/25 15:29
 * @Description: []
 */
@Component
public class FlowPermissionHelper {
    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    private ApprovalPermissionService approvalPermissionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;


    public Consumer<FlowInstance> withProjectMemberCheck() {
        return withProjectPermissionCheck(
                flowInstance -> flowInstance.getProjectId() != null && projectPermissionValidator
                        .hasProjectRole(flowInstance.getProjectId(), ResourceRoleName.all()));
    }

    public Consumer<FlowInstance> withProjectOwnerCheck() {
        return withProjectPermissionCheck(
                flowInstance -> flowInstance.getProjectId() != null && projectPermissionValidator
                        .hasProjectRole(flowInstance.getProjectId(),
                                Collections.singletonList(ResourceRoleName.OWNER)));
    }

    public Consumer<FlowInstance> withApprovableCheck() {
        return flowInstance -> {
            List<UserTaskInstanceEntity> entities = approvalPermissionService.getApprovableApprovalInstances();
            Set<Long> flowInstanceIds = entities.stream().map(UserTaskInstanceEntity::getFlowInstanceId)
                    .collect(Collectors.toSet());
            PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "id", flowInstance.getId(),
                    () -> flowInstanceIds.contains(flowInstance.getId()));
            horizontalDataPermissionValidator.checkCurrentOrganization(flowInstance);
        };
    }

    public Consumer<FlowInstance> withCreatorCheck() {
        return flowInstance -> {
            if (!Objects.equals(authenticationFacade.currentUserId(), flowInstance.getCreatorId())) {
                throw new AccessDeniedException();
            }
            horizontalDataPermissionValidator.checkCurrentOrganization(flowInstance);
        };
    }

    public Consumer<FlowInstance> skipCheck() {
        return flowInstance -> {
        };
    }

    private Consumer<FlowInstance> withProjectPermissionCheck(Predicate<FlowInstance> predicate) {
        return flowInstance -> {
            if (!Objects.equals(authenticationFacade.currentUserId(), flowInstance.getCreatorId())
                    && !predicate.test(flowInstance)) {
                throw new AccessDeniedException();
            }
            horizontalDataPermissionValidator.checkCurrentOrganization(flowInstance);
        };
    }
}
