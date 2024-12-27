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
package com.oceanbase.odc.service.task.resource;

import java.util.Optional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoRepository;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/12/5 10:41
 */
@Slf4j
public class SupervisorAgentAllocator {
    protected final ResourceAllocateInfoRepository resourceAllocateInfoRepository;

    public SupervisorAgentAllocator(ResourceAllocateInfoRepository resourceAllocateInfoRepository) {
        this.resourceAllocateInfoRepository = resourceAllocateInfoRepository;
    }

    public Optional<SupervisorEndpoint> tryAllocateSupervisorEndpoint(String applierName, JobContext jobContext,
            ResourceLocation resourceLocation) {
        // register it to allocate info
        ResourceAllocateInfoEntity entity = createAllocateInfo(applierName, jobContext, resourceLocation);
        ResourceAllocateState resourceAllocateState =
                ResourceAllocateState.fromString(entity.getResourceAllocateState());
        switch (resourceAllocateState) {
            // failed and finished is illegal state for allocate operation
            case FAILED:
                log.info("allocate resource failed for jobContext = {}", jobContext);
                updateUsageState(jobContext.getJobIdentity().getId(), ResourceUsageState.FINISHED);
                throw new RuntimeException("allocate resource failed for jobContext = " + jobContext + ")");
            case FINISHED:
                log.info("allocate resource invalid state with finished for jobContext = {}", jobContext);
                throw new RuntimeException(
                        "allocate resource invalid state with finished for jobContext = " + jobContext + ")");
            case AVAILABLE:
                log.info("allocate resource succeed for jobContext = {}, allocate endpoint = {}", jobContext,
                        entity.getEndpoint());
                SupervisorEndpoint ret = JsonUtils.fromJson(entity.getEndpoint(), SupervisorEndpoint.class);
                updateUsageState(jobContext.getJobIdentity().getId(), ResourceUsageState.USING);
                return Optional.of(ret);
            case PREPARING:
            case CREATING_RESOURCE:
                return Optional.empty();
            default:
                throw new RuntimeException("allocate resource meet unexpected state =" + resourceAllocateState);
        }
    }

    public void deallocateSupervisorEndpoint(Long taskID) {
        updateUsageState(taskID, ResourceUsageState.FINISHED);
    }

    public Optional<ResourceAllocateInfoEntity> queryResourceAllocateIntoEntity(Long taskID) {
        return resourceAllocateInfoRepository.findByTaskIdNative(taskID);
    }

    /**
     * create allocate info for job context
     * 
     * @param jobContext
     */
    protected ResourceAllocateInfoEntity createAllocateInfo(String applierName, JobContext jobContext,
            ResourceLocation resourceLocation) {
        Optional<ResourceAllocateInfoEntity> resourceAllocateInfoEntity =
                resourceAllocateInfoRepository.findByTaskIdNative(jobContext.getJobIdentity().getId());
        if (resourceAllocateInfoEntity.isPresent()) {
            return resourceAllocateInfoEntity.get();
        }
        ResourceAllocateInfoEntity created = new ResourceAllocateInfoEntity();
        created.setResourceAllocateState(ResourceAllocateState.PREPARING.name());
        created.setResourceUsageState(ResourceUsageState.PREPARING.name());
        created.setResourceRegion(resourceLocation.getRegion());
        created.setResourceGroup(resourceLocation.getGroup());
        created.setEndpoint(null);
        created.setTaskId(jobContext.getJobIdentity().getId());
        created.setResourceApplierName(applierName);
        resourceAllocateInfoRepository.save(created);
        return created;
    }

    /**
     * update usage state for task id (job id), this method will called by resource user
     * 
     * @param taskId
     * @param resourceUsageState
     */
    protected void updateUsageState(Long taskId, ResourceUsageState resourceUsageState) {
        resourceAllocateInfoRepository.updateResourceUsageStateByTaskId(resourceUsageState.name(), taskId);
    }
}
