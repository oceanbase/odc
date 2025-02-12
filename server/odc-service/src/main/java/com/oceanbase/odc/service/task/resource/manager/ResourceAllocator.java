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

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
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
    protected final TaskFrameworkProperties taskFrameworkProperties;

    public ResourceAllocator(SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap,
            ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap,
            RemoteTaskSupervisorProxy remoteTaskSupervisorProxy, ResourceManageStrategy resourceManageStrategy,
            TaskFrameworkProperties taskFrameworkProperties) {
        this.supervisorEndpointRepositoryWrap = supervisorEndpointRepositoryWrap;
        this.resourceAllocateInfoRepositoryWrap = resourceAllocateInfoRepositoryWrap;
        this.remoteTaskSupervisorProxy = remoteTaskSupervisorProxy;
        this.resourceManageStrategy = resourceManageStrategy;
        this.taskFrameworkProperties = taskFrameworkProperties;
    }

    // allocate entry
    public void allocateSupervisorAgent(TransactionManager transactionManager) {
        List<ResourceAllocateInfoEntity> resourceToAllocate = resourceAllocateInfoRepositoryWrap.collectAllocateInfo();
        for (ResourceAllocateInfoEntity resourceAllocateInfo : resourceToAllocate) {
            try {
                transactionManager.doInTransactionWithoutResult(() -> allocate(resourceAllocateInfo));
            } catch (Throwable e) {
                log.warn("allocate supervisor agent for allocate info = {} failed", resourceAllocateInfo, e);
                resourceAllocateInfoRepositoryWrap.failedAllocateForId(resourceAllocateInfo.getTaskId());
            }
        }
    }

    // allocate for one allocate info
    protected void allocate(ResourceAllocateInfoEntity resourceAllocateInfo) {
        try {
            ResourceAllocateState resourceAllocateState =
                    ResourceAllocateState.fromString(resourceAllocateInfo.getResourceAllocateState());
            if (resourceAllocateState == ResourceAllocateState.PREPARING) {
                // handle preparing info
                handleAllocateResourceInfoInPreparingState(resourceAllocateInfo);
            } else if (resourceAllocateState == ResourceAllocateState.CREATING_RESOURCE) {
                // handle creating resource info
                handleAllocateResourceInfoInCreatingResourceState(resourceAllocateInfo);
            } else {
                log.warn("invalid state for allocate supervisor agent, current is " + resourceAllocateInfo);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // handle prepare allocate resource state
    protected void handleAllocateResourceInfoInPreparingState(ResourceAllocateInfoEntity allocateInfoEntity)
            throws Exception {
        // no resource allocated, directly change to failed state to tell user resource allocate failed
        if (!isAllocateInfoValid(allocateInfoEntity)) {
            log.info("resource prepare expired for entity =  {}", allocateInfoEntity);
            resourceAllocateInfoRepositoryWrap.failedAllocateForId(allocateInfoEntity.getTaskId());
            return;
        }
        SupervisorEndpointEntity supervisorEndpoint = chooseSupervisorEndpoint(allocateInfoEntity);
        if (null != supervisorEndpoint) {
            // allocate success
            log.info("allocate supervisor endpoint = {} for job id = {}", supervisorEndpoint,
                    allocateInfoEntity.getTaskId());
            resourceAllocateInfoRepositoryWrap.allocateForJob(supervisorEndpoint.getEndpoint(),
                    supervisorEndpoint.getId(), allocateInfoEntity.getTaskId());
        } else {
            // try allocate new resource
            SupervisorEndpointEntity endpoint = resourceManageStrategy.handleNoResourceAvailable(allocateInfoEntity);
            // info provided, not change current state
            if (null != endpoint) {
                // increase load to bind this task to this resource, and let resource not released
                // Notice: if we don't bind task to allocated pod, this logic should be rewrite
                supervisorEndpointRepositoryWrap.operateLoad(endpoint.getId(), 1);
                resourceAllocateInfoRepositoryWrap.prepareResourceForJob(endpoint.getEndpoint(),
                        endpoint.getId(), allocateInfoEntity.getTaskId());
                log.info("endpoint prepare for job id = {}, wait  resource = {} ready",
                        allocateInfoEntity.getTaskId(), endpoint);
            } else {
                log.debug("not endpoint available for job id = {}, ignore current schedule",
                        allocateInfoEntity.getTaskId());
            }
        }
    }

    // handle creating resource allocate resource state
    protected void handleAllocateResourceInfoInCreatingResourceState(ResourceAllocateInfoEntity allocateInfoEntity)
            throws Exception {
        if (!isAllocateInfoValid(allocateInfoEntity)) {
            // resource is creating and use finished, release load, let
            // TaskResourceManager.detectIfResourceIsReady determinate how to process resource
            releaseSupervisorEndPointLoad(allocateInfoEntity);
            resourceAllocateInfoRepositoryWrap.failedAllocateForId(allocateInfoEntity.getTaskId());
            return;
        }
        SupervisorEndpointEntity supervisorEndpoint =
                resourceManageStrategy.detectIfEndpointIsAvailable(allocateInfoEntity);
        if (null != supervisorEndpoint) {
            // allocate success
            log.info("resource ready with endpoint id = {}, allocate supervisor endpoint = {} for job id = {}",
                    allocateInfoEntity.getSupervisorEndpointId(), supervisorEndpoint, allocateInfoEntity.getTaskId());
            resourceAllocateInfoRepositoryWrap.allocateForJob(supervisorEndpoint.getEndpoint(),
                    supervisorEndpoint.getId(), allocateInfoEntity.getTaskId());
        } else {
            // wait resource ready
            log.debug("resource not ready with endpoint id = {}, endpoint = {} for job id = {}, wait resource ready",
                    allocateInfoEntity.getSupervisorEndpointId(), allocateInfoEntity.getEndpoint(),
                    allocateInfoEntity.getTaskId());
        }
    }

    protected boolean isAllocateInfoValid(ResourceAllocateInfoEntity allocateInfoEntity) {
        boolean isExpired = isAllocateInfoExpired(allocateInfoEntity);
        if (isExpired || ResourceUsageState
                .fromString(allocateInfoEntity.getResourceUsageState()) == ResourceUsageState.FINISHED) {
            // job is canceled or resource not prepare success for a long time
            log.info("resource preparing failed cause {}, entity =  {}",
                    isExpired ? "expired" : "resource not needed anymore", allocateInfoEntity);
            return false;
        } else {
            return true;
        }
    }

    protected void releaseSupervisorEndPointLoad(ResourceAllocateInfoEntity allocateInfoEntity) {
        SupervisorEndpoint endpoint = JsonUtils.fromJson(allocateInfoEntity.getEndpoint(), SupervisorEndpoint.class);
        if (null != endpoint) {
            supervisorEndpointRepositoryWrap.releaseLoad(allocateInfoEntity.getSupervisorEndpointId());
        }
    }

    /**
     * try choose a supervisor agent for given region and group
     *
     * @return
     */
    protected SupervisorEndpointEntity chooseSupervisorEndpoint(ResourceAllocateInfoEntity entity) {
        // TODO(): maybe we can sort entity, reuse this endpoint result set
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
            SupervisorEndpoint ret = new SupervisorEndpoint(tmp.getHost(), tmp.getPort());
            // TODO(longxuan): handle unreached supervisor
            // alive and have enough resource can start new job
            if (remoteTaskSupervisorProxy.isSupervisorAlive(ret)
                    && resourceManageStrategy.isEndpointHaveEnoughResource(tmp, entity)) {
                // each task means one load
                supervisorEndpointRepositoryWrap.operateLoad(tmp.getId(), 1);
                return tmp;
            }
        }
        return null;
    }

    protected boolean isAllocateInfoExpired(ResourceAllocateInfoEntity entity) {
        Duration between = Duration.between(entity.getUpdateTime().toInstant(), Instant.now());
        // 300 seconds considered as timeout
        // TODO(lx): config it
        return (between.toMillis() / 1000 > taskFrameworkProperties.getResourceAllocateTimeOutSeconds());
    }

}
