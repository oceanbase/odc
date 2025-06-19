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
package com.oceanbase.odc.service.schedule.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.SingleOrganizationResource;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleMapper;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class BatchSchedulePermissionValidator {

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Autowired
    @Lazy
    private FlowInstanceService flowInstanceService;

    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;

    @Autowired
    private ScheduleRepository scheduleRepository;


    public void checkScheduleIdsPermission(ScheduleType scheduleType, Collection<Long> ids) {
        Set<Long> projectIds;
        if (scheduleType.equals(ScheduleType.PARTITION_PLAN)) {
            List<FlowInstanceEntity> flowInstanceEntities = flowInstanceService.listByIds(ids);
            projectIds =
                    flowInstanceEntities.stream().map(FlowInstanceEntity::getProjectId).collect(Collectors.toSet());
            List<FlowOrganizationIsolated> flowOrganizationIsolateds = flowInstanceEntities.stream().map(
                    f -> new FlowOrganizationIsolated(f.getOrganizationId(), f.getId())).collect(
                            Collectors.toList());
            horizontalDataPermissionValidator.checkCurrentOrganization(flowOrganizationIsolateds);
        } else {
            List<Schedule> scheduleEntities = scheduleRepository.findByIdIn(ids).stream()
                    .map(ScheduleMapper.INSTANCE::entityToModel).collect(
                            Collectors.toList());
            horizontalDataPermissionValidator.checkCurrentOrganization(scheduleEntities);
            projectIds = scheduleEntities.stream().map(Schedule::getProjectId).collect(Collectors.toSet());
        }
        if (authenticationFacade.currentOrganization().getType().equals(OrganizationType.TEAM)) {
            projectPermissionValidator.checkProjectRole(projectIds, ResourceRoleName.all());
        }
    }


    @Data
    @AllArgsConstructor
    private final static class FlowOrganizationIsolated implements SingleOrganizationResource {

        private Long organizationId;
        private Long id;

        @Override
        public String resourceType() {
            return ResourceType.ODC_FLOW_INSTANCE.name();
        }

        @Override
        public Long organizationId() {
            return organizationId;
        }

        @Override
        public Long id() {
            return id;
        }
    }
}
