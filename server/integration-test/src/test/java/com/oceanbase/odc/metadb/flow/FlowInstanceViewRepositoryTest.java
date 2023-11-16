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

package com.oceanbase.odc.metadb.flow;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

/**
 * @author jingtian
 * @date 2023/8/13
 * @since ODC_release_4.2.0
 */
public class FlowInstanceViewRepositoryTest extends ServiceTestEnv {
    @Autowired
    private FlowInstanceViewRepository flowInstanceViewRepository;

    @Test
    public void test_leftJoinFlowInstanceApprovalView_with_creator_id() {
        HashSet<String> resourceRoleIdentifiers = new HashSet<>();
        resourceRoleIdentifiers.add("1:2");
        resourceRoleIdentifiers.add("2:2");
        Specification<FlowInstanceViewEntity> specification = Specification
                .where(FlowInstanceViewSpecs.leftJoinFlowInstanceApprovalView(resourceRoleIdentifiers, 1L,
                        FlowNodeStatus.getExecutingStatuses()))
                .and(FlowInstanceViewSpecs.creatorIdIn(new LinkedList<>()))
                .and(FlowInstanceViewSpecs.organizationIdEquals(1L))
                .and(FlowInstanceViewSpecs.projectIdEquals(null))
                .and(FlowInstanceViewSpecs.statusIn(null));
        specification = specification.and(FlowInstanceViewSpecs.taskTypeEquals(TaskType.ASYNC));
        List<FlowInstanceViewEntity> entities = flowInstanceViewRepository.findAll(specification);
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void test_leftJoinFlowInstanceApprovalView_without_creator_id() {
        HashSet<String> resourceRoleIdentifiers = new HashSet<>();
        resourceRoleIdentifiers.add("1:2");
        resourceRoleIdentifiers.add("2:2");
        Specification<FlowInstanceViewEntity> specification = Specification
                .where(FlowInstanceViewSpecs.leftJoinFlowInstanceApprovalView(resourceRoleIdentifiers, null,
                        FlowNodeStatus.getExecutingStatuses()))
                .and(FlowInstanceViewSpecs.creatorIdIn(new LinkedList<>()))
                .and(FlowInstanceViewSpecs.organizationIdEquals(1L))
                .and(FlowInstanceViewSpecs.projectIdEquals(null))
                .and(FlowInstanceViewSpecs.statusIn(null));
        specification = specification.and(FlowInstanceViewSpecs.taskTypeEquals(TaskType.ASYNC));
        List<FlowInstanceViewEntity> entities = flowInstanceViewRepository.findAll(specification);
        Assert.assertEquals(0, entities.size());
    }

}
