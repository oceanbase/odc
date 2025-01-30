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
package com.oceanbase.odc.service.task.resource.manager;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoRepository;
import com.oceanbase.odc.service.task.resource.ResourceAllocateState;
import com.oceanbase.odc.service.task.resource.ResourceUsageState;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * wrap operation for ResourceAllocateInfoRepository
 * 
 * @author longpeng.zlp
 * @date 2024/12/19 14:13
 */
public class ResourceAllocateInfoRepositoryWrap {
    protected final ResourceAllocateInfoRepository repository;

    public ResourceAllocateInfoRepositoryWrap(ResourceAllocateInfoRepository repository) {
        this.repository = repository;
    }

    /**
     * allocate endpoint for id, this method will called by resource manager
     *
     * @param supervisorEndpoint
     * @param taskID
     */
    protected void allocateForJob(SupervisorEndpoint supervisorEndpoint, Long resourceId, Long taskID) {
        repository.updateEndpointByTaskId(JsonUtils.toJson(supervisorEndpoint), resourceId, taskID);
    }

    /**
     * pre allocate unavailable resource
     * 
     * @param supervisorEndpoint
     * @param resourceId
     * @param taskID
     */
    protected void prepareResourceForJob(SupervisorEndpoint supervisorEndpoint, Long resourceId, Long taskID) {
        repository.updateResourceIdByTaskId(JsonUtils.toJson(supervisorEndpoint), resourceId, taskID);
    }

    /**
     * task allocate has failed for id, this method will called by resource manager
     *
     * @param taskID
     */
    protected void failedAllocateForId(Long taskID) {
        repository.updateResourceAllocateStateByTaskId(ResourceAllocateState.FAILED.name(), taskID);
    }

    /**
     * task allocate has finished for id, this method will called by resource manager
     *
     * @param taskID
     */
    protected void finishedAllocateForId(Long taskID) {
        repository.updateResourceAllocateStateByTaskId(ResourceAllocateState.FINISHED.name(),
                taskID);
    }

    /**
     * collect allocate info needed to allocate
     *
     * @return
     */
    protected List<ResourceAllocateInfoEntity> collectAllocateInfo() {
        Specification<ResourceAllocateInfoEntity> condition = Specification.where(
                SpecificationUtil.columnIn("resourceAllocateState", Arrays.asList(
                        ResourceAllocateState.PREPARING.name(), ResourceAllocateState.CREATING_RESOURCE.name())));
        return repository.findAll(condition, PageRequest.of(0, 100)).getContent();
    }

    /**
     * collect deallocate info needed to deallocate
     *
     * @return
     */
    protected List<ResourceAllocateInfoEntity> collectDeAllocateInfo() {
        Specification<ResourceAllocateInfoEntity> condition = Specification.where(
                SpecificationUtil.columnEqual("resourceUsageState", ResourceUsageState.FINISHED.name()));
        Specification<ResourceAllocateInfoEntity> query = condition
                .and(SpecificationUtil.columnEqual("resourceAllocateState", ResourceAllocateState.AVAILABLE.name()));
        return repository.findAll(query, PageRequest.of(0, 100)).getContent();
    }
}
