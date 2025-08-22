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
package com.oceanbase.odc.service.collaboration.landingpage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Sets;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewSpecs;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository.ScheduleTypeCount;
import com.oceanbase.odc.metadb.schedule.ScheduleSpecs;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository.ScheduleTaskTypeAndStatusCount;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryFlowInstanceStatParams;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryScheduleStatParams;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryStatParams;
import com.oceanbase.odc.service.collaboration.landingpage.model.TaskTodoCategory;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.InnerQueryFlowInstanceParams;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.UserResourceRole;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: mayang
 * @Date: 2025/7/30 13:55
 * @Since: 4.4.0
 * @Description: landing page service
 */
@Validated
@Slf4j
@Service
@Authenticated
public class LandingPageService {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ResourceRoleService resourceRoleService;

    /**
     * @return
     * 
     *         <pre>
     *     {@code
     *          {
     *              "MOCKDATA": {
     *                   "count": {
     *                       "PENDING": 1,
     *                       "EXECUTING": 1,
     *                       "EXECUTION_FAILURE": 1,
     *                       "EXECUTION_SUCCESS": 1,
     *                       "OTHER": 1
     *                    }
     *               }
     *               ...
     *          }
     *      }
     *         </pre>
     *
     */
    public Stats listFlowInstanceTaskStats(@NonNull QueryFlowInstanceStatParams params) {
        if (!supportsLandingPage()) {
            return Stats.empty();
        }
        Set<Long> validProjectIds = projectService.checkAndGetJoinedProjectIds(params.getProjectIds());
        if (CollectionUtils.isEmpty(validProjectIds) || CollectionUtils.isEmpty(params.getTaskTypes())) {
            return Stats.empty();
        }

        InnerQueryFlowInstanceParams innerQueryFlowParams = new InnerQueryFlowInstanceParams()
                .setStartTime(params.getStartTime())
                .setEndTime(params.getEndTime())
                .setProjectIds(validProjectIds)
                .setTaskTypes(params.getTaskTypes());
        return TaskStatProcessor.toFlowInstanceStat(
                flowInstanceRepository.findFlowInstanceTaskTypeAndStatusCount(innerQueryFlowParams));
    }

    /**
     * @return
     * 
     *         <pre>
     *     {@code
     *          {
     *              "PARTITION_PLAN": {
     *                   "count": {
     *                       "ENABLED": 1,
     *                       "PENDING": 1,
     *                       "EXECUTING": 1,
     *                       "EXECUTION_FAILURE": 1,
     *                       "EXECUTION_SUCCESS": 1,
     *                       "OTHER": 1
     *                   }
     *              }
     *              ...
     *          }
     *      }
     *         </pre>
     */
    public Stats listScheduleAndTaskStats(@NonNull QueryScheduleStatParams params) {
        final Stats scheduleStats = Stats.empty();
        if (!supportsLandingPage()) {
            return scheduleStats;
        }
        params.setProjectIds(projectService.checkAndGetJoinedProjectIds(params.getProjectIds()));
        if (CollectionUtils.isEmpty(params.getProjectIds()) || CollectionUtils.isEmpty(params.getScheduleTypes())) {
            return scheduleStats;
        }
        params.configLandingPageInitialParam();

        List<ScheduleTypeCount> scheduleTypeCounts = scheduleRepository.findScheduleTypeCount(params);

        Specification<ScheduleEntity> selectIdSubQuerySpec =
                scheduleRepository.buildWithScheduleTypeAndStatusSpec(params);
        List<ScheduleTaskTypeAndStatusCount> scheduleTaskStatusCounts =
                scheduleTaskRepository.findScheduleTaskStatusCountBySubQuery(params, selectIdSubQuerySpec);

        return TaskStatProcessor.toScheduleStat(scheduleTypeCounts, scheduleTaskStatusCounts);
    }

    /**
     * @return
     * 
     *         <pre>
     *     {@code
     *         {
     *             "FLOW": {
     *                  "count": {
     *                      "FLOW_WAIT_ME_APPROVAL": 0,
     *                      "FLOW_WAIT_ME_EXECUTION": 0
     *                  }
     *             },
     *             "SCHEDULE": {
     *                  "count": {
     *                      "SCHEDULE_WAIT_ME_APPROVAL": 0
     *                  }
     *             }
     *         }
     *     }
     *         </pre>
     */
    public Stats getFlowAndScheduleTodoStat(@NonNull QueryStatParams params) {
        final Stats flowAndScheduleTodoStat = Stats.empty();
        if (!supportsLandingPage()) {
            return flowAndScheduleTodoStat;
        }
        params.setProjectIds(projectService.checkAndGetJoinedProjectIds(params.getProjectIds()));
        if (params.getProjectIds().isEmpty()) {
            return flowAndScheduleTodoStat;
        }
        HashMap<TaskTodoCategory, Long> category2Count = new HashMap<>();

        // flow approval by current
        Specification<FlowInstanceViewEntity> flowSpecOfApprovalByCurrent = getSpecOfApprovalByCurrent(params);
        category2Count.put(TaskTodoCategory.FLOW_WAIT_ME_APPROVAL,
                flowInstanceRepository.countDistinctId(flowSpecOfApprovalByCurrent));

        // flow execution by current
        Set<Long> permittedProjectIds = resourceRoleService.listByResourceTypeAndResourceIdIn(ResourceType.ODC_PROJECT,
                params.getProjectIds())
                .stream().filter(r -> ResourceRoleName.OWNER.equals(r.getResourceRole())
                        || ResourceRoleName.DBA.equals(r.getResourceRole()))
                .map(UserResourceRole::getResourceId).collect(Collectors.toSet());
        Specification<FlowInstanceViewEntity> flowSpecOfExecutionByCurrent =
                getSpecOfExecutionByCurrent(permittedProjectIds);
        category2Count.put(TaskTodoCategory.FLOW_WAIT_ME_EXECUTION,
                flowInstanceRepository.countDistinctId(flowSpecOfExecutionByCurrent));

        // schedule approval by current
        final QueryScheduleParams queryApprovalScheduleParams = QueryScheduleParams.builder()
                .projectIds(params.getProjectIds())
                .latestScheduleChangeStatuses(Sets.newHashSet(ScheduleChangeStatus.APPROVING))
                .build();
        category2Count.put(TaskTodoCategory.SCHEDULE_WAIT_ME_APPROVAL,
                scheduleRepository.countDistinctId(ScheduleSpecs.joinScheduleChangeLog(queryApprovalScheduleParams)));

        return TaskStatProcessor.toFlowAndScheduleTodoStat(category2Count);
    }

    private Specification<FlowInstanceViewEntity> getSpecOfApprovalByCurrent(
            @NonNull QueryStatParams params) {
        return FlowInstanceViewSpecs.projectIdIn(params.getProjectIds())
                .and(FlowInstanceViewSpecs.taskTypeIn(TaskType.visibleTaskTypes()))
                .and(flowInstanceService.getSpecOfApprovalByCurrent());
    }

    /**
     * {@code
     *  SELECT COUNT(*)
     *  FROM list_flow_instance_view
     *  WHERE task_type in (..)
     *      AND status in ('WAIT_FOR_EXECUTION')
     *      AND (creator_id = x OR project_id in (...))
     *  GROUP BY id、task_type
     * }
     */
    private Specification<FlowInstanceViewEntity> getSpecOfExecutionByCurrent(
            @NonNull Set<Long> permittedProjectIds) {
        Specification<FlowInstanceViewEntity> permittedSpec =
                FlowInstanceViewSpecs.creatorIdEquals(authenticationFacade.currentUserId());
        if (!permittedProjectIds.isEmpty()) {
            permittedSpec = permittedSpec.or(FlowInstanceViewSpecs.projectIdIn(permittedProjectIds));
        }
        return FlowInstanceViewSpecs.taskTypeIn(TaskType.visibleTaskTypes())
                .and(FlowInstanceViewSpecs.statusIn(Collections.singleton(FlowStatus.WAIT_FOR_EXECUTION)))
                .and(permittedSpec);
    }

    private boolean supportsLandingPage() {
        return authenticationFacade.currentOrganization().getType() == OrganizationType.TEAM;
    }

}
