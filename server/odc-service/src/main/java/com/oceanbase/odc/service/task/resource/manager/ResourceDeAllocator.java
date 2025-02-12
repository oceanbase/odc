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

import java.util.List;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.service.task.service.TransactionManager;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.proxy.RemoteTaskSupervisorProxy;

import lombok.extern.slf4j.Slf4j;

/**
 * agent and resource de allocator
 * 
 * @author longpeng.zlp
 * @date 2024/12/27 16:08
 */
@Slf4j
public class ResourceDeAllocator {
    protected final SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap;
    protected final ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap;
    protected final RemoteTaskSupervisorProxy remoteTaskSupervisorProxy;
    protected final ResourceManageStrategy resourceManageStrategy;

    public ResourceDeAllocator(SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap,
            ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap,
            RemoteTaskSupervisorProxy remoteTaskSupervisorProxy, ResourceManageStrategy resourceManageStrategy) {
        this.supervisorEndpointRepositoryWrap = supervisorEndpointRepositoryWrap;
        this.resourceAllocateInfoRepositoryWrap = resourceAllocateInfoRepositoryWrap;
        this.remoteTaskSupervisorProxy = remoteTaskSupervisorProxy;
        this.resourceManageStrategy = resourceManageStrategy;
    }

    // deallocate entry
    public void deAllocateSupervisorAgent(TransactionManager transactionManager) {
        List<ResourceAllocateInfoEntity> resourceToDeallocate =
                resourceAllocateInfoRepositoryWrap.collectDeAllocateInfo();
        for (ResourceAllocateInfoEntity deAllocateInfoEntity : resourceToDeallocate) {
            transactionManager.doInTransactionWithoutResult(() -> deallocate(deAllocateInfoEntity));
        }
    }

    protected void deallocate(ResourceAllocateInfoEntity deAllocateInfoEntity) {
        try {
            SupervisorEndpoint supervisorEndpoint =
                    JsonUtils.fromJson(deAllocateInfoEntity.getEndpoint(), SupervisorEndpoint.class);
            if (null == deAllocateInfoEntity.getSupervisorEndpointId()) {
                log.warn("invalid state, resource id or endpoint should not be null, entity = {}",
                        deAllocateInfoEntity);
                return;
            } else {
                // de allocate success
                log.info("release resource for taskID = {}, endpoint = {}, endpointId = {}",
                        deAllocateInfoEntity.getTaskId(), deAllocateInfoEntity.getEndpoint(),
                        deAllocateInfoEntity.getSupervisorEndpointId());
                supervisorEndpointRepositoryWrap.releaseLoad(deAllocateInfoEntity.getSupervisorEndpointId());
            }
            resourceAllocateInfoRepositoryWrap.finishedAllocateForId(deAllocateInfoEntity.getTaskId());
        } catch (Throwable e) {
            // wait do next round
            log.warn("deallocate resource for allocate info ={}", deAllocateInfoEntity, e);
        }
    }
}
