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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.service.task.resource.ResourceAllocateState;
import com.oceanbase.odc.service.task.resource.ResourceUsageState;
import com.oceanbase.odc.service.task.service.TransactionManager;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.proxy.RemoteTaskSupervisorProxy;

import lombok.extern.slf4j.Slf4j;

/**
 * agent and resource allocator
 * 
 * @author longpeng.zlp
 * @date 2024/12/27 16:01
 */
@Slf4j
public class ResourceAllocator {
    protected final SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap;
    protected final ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap;
    protected final RemoteTaskSupervisorProxy remoteTaskSupervisorProxy;
    protected final ResourceManageStrategy resourceManageStrategy;

    public ResourceAllocator(SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap,
            ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap,
            RemoteTaskSupervisorProxy remoteTaskSupervisorProxy, ResourceManageStrategy resourceManageStrategy) {
        this.supervisorEndpointRepositoryWrap = supervisorEndpointRepositoryWrap;
        this.resourceAllocateInfoRepositoryWrap = resourceAllocateInfoRepositoryWrap;
        this.remoteTaskSupervisorProxy = remoteTaskSupervisorProxy;
        this.resourceManageStrategy = resourceManageStrategy;
    }

    // allocate entry
    public void allocateSupervisorAgent(TransactionManager transactionManager) {
        List<ResourceAllocateInfoEntity> resourceToAllocate = resourceAllocateInfoRepositoryWrap.collectAllocateInfo();
        for (ResourceAllocateInfoEntity resourceAllocateInfo : resourceToAllocate) {
            transactionManager.doInTransactionWithoutResult(() -> allocate(resourceAllocateInfo));
        }
    }

    // allocate for one allocate info
    protected void allocate(ResourceAllocateInfoEntity resourceAllocateInfo) {
        try {
            if (!isAllocateInfoValid(resourceAllocateInfo)) {
                return;
            }
            ResourceAllocateState resourceAllocateState =
                    ResourceAllocateState.fromString(resourceAllocateInfo.getResourceAllocateState());
            if (resourceAllocateState == ResourceAllocateState.PREPARING) {
                // handle preparing info
                handleAllocateResourceInfoInPreparing(resourceAllocateInfo);
            } else if (resourceAllocateState == ResourceAllocateState.CREATING_RESOURCE) {
                // handle creating resource info
                handleAllocateResourceInfoInCreatingResource(resourceAllocateInfo);
            } else {
                log.warn("invalid state for allocate supervisor agent, current is " + resourceAllocateInfo);
            }
        } catch (Throwable e) {
            log.warn("allocate supervisor agent for allocate info = {} failed", resourceAllocateInfo, e);
            resourceAllocateInfoRepositoryWrap.failedAllocateForId(resourceAllocateInfo.getTaskId());
        }
    }

    // handle prepare allocate resource state
    protected void handleAllocateResourceInfoInPreparing(ResourceAllocateInfoEntity allocateInfoEntity)
            throws Exception {
        SupervisorEndpointEntity supervisorEndpoint = chooseSupervisorEndpoint(allocateInfoEntity);
        if (null != supervisorEndpoint) {
            // allocate success
            log.info("allocate supervisor endpoint = {} for job id = {}", supervisorEndpoint,
                    allocateInfoEntity.getTaskId());
            resourceAllocateInfoRepositoryWrap.allocateForJob(supervisorEndpoint.getEndpoint(),
                    supervisorEndpoint.getResourceID(), allocateInfoEntity.getTaskId());
        } else {
            // try allocate new resource
            SupervisorEndpointEntity endpoint = resourceManageStrategy.handleNoResourceAvailable(allocateInfoEntity);
            // info provided, not change current state
            if (null != endpoint) {
                // increase load to bind this task to this resource, and let resource not released
                // Notice: if we don't bind task to allocated pod, this logic should be rewrite
                supervisorEndpointRepositoryWrap.operateLoad(endpoint.getHost(), endpoint.getPort(),
                        endpoint.getResourceID(), 1);
                resourceAllocateInfoRepositoryWrap.prepareResourceForJob(endpoint.getEndpoint(),
                        endpoint.getResourceID(), allocateInfoEntity.getTaskId());
                log.info("endpoint prepare for job id = {}, wait  resource = {} ready",
                        allocateInfoEntity.getTaskId(), endpoint);
            } else {
                log.debug("not endpoint available for job id = {}, ignore current schedule",
                        allocateInfoEntity.getTaskId());
            }
        }
    }

    // handle creating resource allocate resource state
    protected void handleAllocateResourceInfoInCreatingResource(ResourceAllocateInfoEntity allocateInfoEntity)
            throws Exception {
        SupervisorEndpointEntity supervisorEndpoint =
                resourceManageStrategy.detectIfResourceIsReady(allocateInfoEntity);
        if (null != supervisorEndpoint) {
            // allocate success
            log.info("resource ready with resource id = {}, allocate supervisor endpoint = {} for job id = {}",
                    allocateInfoEntity.getResourceId(), supervisorEndpoint, allocateInfoEntity.getTaskId());
            resourceAllocateInfoRepositoryWrap.allocateForJob(supervisorEndpoint.getEndpoint(),
                    supervisorEndpoint.getResourceID(), allocateInfoEntity.getTaskId());
        } else {
            // wait resource ready
            log.debug("resource not ready with resource id = {}, endpoint = {} for job id = {}, wait resource ready",
                    allocateInfoEntity.getResourceId(), allocateInfoEntity.getEndpoint(),
                    allocateInfoEntity.getTaskId());
        }
    }

    // if resource entity is valid
    protected boolean isAllocateInfoValid(ResourceAllocateInfoEntity allocateInfoEntity) {
        if (isAllocateInfoExpired(allocateInfoEntity)) {
            log.info("resource prepare expired for entity =  {}", allocateInfoEntity);
            resourceAllocateInfoRepositoryWrap.failedAllocateForId(allocateInfoEntity.getTaskId());
            return false;
        }
        // resource not need any more
        if (ResourceUsageState.fromString(allocateInfoEntity.getResourceUsageState()) == ResourceUsageState.FINISHED) {
            log.info("resource prepare canceled for entity = {}", allocateInfoEntity);
            resourceAllocateInfoRepositoryWrap.finishedAllocateForId(allocateInfoEntity.getTaskId());
            return false;
        }
        return true;
    }

    /**
     * try choose a supervisor agent for given region and group
     *
     * @return
     */
    protected SupervisorEndpointEntity chooseSupervisorEndpoint(ResourceAllocateInfoEntity entity) {
        List<SupervisorEndpointEntity> supervisorEndpointEntities = supervisorEndpointRepositoryWrap
                .collectAvailableSupervisorEndpoint(entity.getResourceRegion(), entity.getResourceGroup());
        // no available found
        if (CollectionUtils.isEmpty(supervisorEndpointEntities)) {
            // no endpoint found, that's not good
            log.warn("not supervisor end point found");
            return null;
        }
        // use load smaller
        supervisorEndpointEntities = supervisorEndpointEntities.stream()
                .sorted((s1, s2) -> Integer.compare(s1.getLoads(), s2.getLoads())).collect(
                        Collectors.toList());
        for (SupervisorEndpointEntity tmp : supervisorEndpointEntities) {
            if (!resourceManageStrategy.isEndpointHaveEnoughResource(tmp, entity)) {
                continue;
            }
            SupervisorEndpoint ret = new SupervisorEndpoint(tmp.getHost(), tmp.getPort());
            // TODO(longxuan): handle unreached supervisor
            if (remoteTaskSupervisorProxy.isSupervisorAlive(ret)) {
                // each task means one load
                supervisorEndpointRepositoryWrap.operateLoad(tmp.getHost(), tmp.getPort(), tmp.getResourceID(), 1);
                return tmp;
            }
        }
        return null;
    }

    protected boolean isAllocateInfoExpired(ResourceAllocateInfoEntity entity) {
        Duration between = Duration.between(entity.getUpdateTime().toInstant(), Instant.now());
        // 300 seconds considered as timeout
        // TODO(lx): config it
        return (between.toMillis() / 1000 > 60);
    }

}
