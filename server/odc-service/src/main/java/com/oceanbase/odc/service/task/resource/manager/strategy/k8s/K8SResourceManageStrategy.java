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
package com.oceanbase.odc.service.task.resource.manager.strategy.k8s;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointRepository;
import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceWithID;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.resource.Constants;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.resource.K8sResourceContext;
import com.oceanbase.odc.service.task.resource.manager.ResourceManageStrategy;
import com.oceanbase.odc.service.task.resource.manager.SupervisorEndpointRepositoryWrap;
import com.oceanbase.odc.service.task.supervisor.SupervisorEndpointState;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * default K8S resource manage strategy
 * 
 * @author longpeng.zlp
 * @date 2024/12/2 14:43
 */
@Slf4j
public class K8SResourceManageStrategy implements ResourceManageStrategy {
    // real resource manger
    protected final ResourceManager resourceManager;
    protected final K8sProperties k8sProperties;
    protected final Supplier<Integer> supervisorListenPortProvider;
    protected final SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap;

    public K8SResourceManageStrategy(K8sProperties k8sProperties, ResourceManager resourceManager,
            SupervisorEndpointRepository supervisorEndpointRepository, Supplier<Integer> supervisorListenPortProvider) {
        this.resourceManager = resourceManager;
        this.k8sProperties = k8sProperties;
        this.supervisorListenPortProvider = supervisorListenPortProvider;
        this.supervisorEndpointRepositoryWrap = new SupervisorEndpointRepositoryWrap(supervisorEndpointRepository);
    }

    /**
     * handle no resource available for allocate request allocate a new pod for request it will create
     * new K8S pod for resource entity, and return resourceID of created k8s resource. TODO(longxuan):
     * add rollback and send event
     * 
     * @param resourceAllocateInfoEntity
     * @return info to stored
     */
    public SupervisorEndpointEntity handleNoResourceAvailable(ResourceAllocateInfoEntity resourceAllocateInfoEntity)
            throws Exception {
        ResourceLocation resourceLocation = new ResourceLocation(resourceAllocateInfoEntity.getResourceRegion(),
                resourceAllocateInfoEntity.getResourceGroup());
        int supervisorListenPort = supervisorListenPortProvider.get();
        K8sResourceContextBuilder contextBuilder = new K8sResourceContextBuilder(k8sProperties, supervisorListenPort);
        K8sResourceContext k8sResourceContext =
                contextBuilder.buildK8sResourceContext(resourceAllocateInfoEntity.getTaskId(), resourceLocation);
        ResourceWithID<K8sPodResource> k8sPodResource = null;
        // allocate resource failed, send alarm event and throws exception
        try {
            k8sPodResource = resourceManager.create(resourceLocation,
                    k8sResourceContext);
        } catch (Throwable e) {
            alarmResourceFailed(resourceAllocateInfoEntity, e);
            throw new JobException("create resource failed for " + resourceAllocateInfoEntity, e);
        }
        log.info("create k8s pod = {} for allocate entity = {}", k8sPodResource, resourceAllocateInfoEntity);
        // save to db failed, try release resource
        try {
            K8sPodResource podResource = k8sPodResource.getResource();
            podResource.setServicePort(String.valueOf(supervisorListenPort));
            if (StringUtils.isEmpty(podResource.getPodIpAddress())) {
                podResource.setPodIpAddress(Constants.RESOURCE_NULL_HOST);
            }
            // create with load 1 to let resource not released
            return supervisorEndpointRepositoryWrap.save(podResource, k8sPodResource.getId(), 0);
        } catch (Throwable e) {
            log.warn("save k8s pod resource to endpoint failed", e);
            // roll back create resource operation
            resourceManager.destroy(k8sPodResource.getResource().resourceID());
            throw new RuntimeException("save resource to db failed", e);
        }
    }

    /**
     * detect if resource is ready for new resource
     * 
     * @param resourceAllocateInfoEntity
     * @return resource endpoint if ready, else null
     */
    public SupervisorEndpointEntity detectIfEndpointIsAvailable(ResourceAllocateInfoEntity resourceAllocateInfoEntity) {
        if (!StringUtils.containsIgnoreCase(resourceAllocateInfoEntity.getResourceApplierName(), "K8S")) {
            return null;
        }
        Long supervisorEndpointId = resourceAllocateInfoEntity.getSupervisorEndpointId();
        if (null == supervisorEndpointId) {
            throw new RuntimeException(
                    "for k8s mode resource allocate mode, endpoint id should not be null, entity = "
                            + resourceAllocateInfoEntity);
        }

        SupervisorEndpointEntity entity =
                supervisorEndpointRepositoryWrap.findById(resourceAllocateInfoEntity.getSupervisorEndpointId());
        SupervisorEndpointState state = SupervisorEndpointState.fromString(entity.getStatus());
        if (state == SupervisorEndpointState.AVAILABLE) {
            return entity;
        } else if (state == SupervisorEndpointState.UNAVAILABLE) {
            throw new RuntimeException("allocate resource failed, entity = " + entity);
        } else {
            return null;
        }
    }

    @Override
    public void refreshSupervisorEndpoint(SupervisorEndpointEntity endpoint) {
        try {
            ResourceWithID<Resource> resourceWithID = resourceManager.query(endpoint.getResourceID())
                    .orElseThrow(() -> new RuntimeException("resource not found, id = " + endpoint.getResourceID()));
            K8sPodResource podResource = (K8sPodResource) resourceWithID.getResource();
            if (podResource.getPodIpAddress() != null) {
                endpoint.setHost(podResource.getPodIpAddress());
                supervisorEndpointRepositoryWrap.updateEndpointHost(endpoint);
                log.info("refresh pod ip address success, id = {}, host =z {}", endpoint.getResourceID(),
                        podResource.getPodIpAddress());
            }
        } catch (Exception e) {
            log.warn("get pod ip address failed, resource id={}", endpoint.getResourceID(), e);
        }
    }

    @Override
    public boolean isEndpointHaveEnoughResource(SupervisorEndpointEntity supervisorEndpoint,
            ResourceAllocateInfoEntity entity) {
        // the pod should be idle, rewrite this method if needed
        return supervisorEndpoint.getLoads() == 0;
    }


    public void releaseResourceById(SupervisorEndpointEntity endpoint) {
        // first release resource
        Optional<ResourceEntity> resourceEntity = resourceManager.getResourceRepository().findById(
                endpoint.getResourceID());
        if (resourceEntity.isPresent()) {
            log.info("endpoint = {} will be released", endpoint);
            resourceManager.release(new ResourceID(resourceEntity.get()));
        } else {
            log.info("not resource found for {}", endpoint);
        }
        // then abandon endpoint
        supervisorEndpointRepositoryWrap.abandonSupervisorEndpoint(endpoint);
    }

    protected void alarmResourceFailed(ResourceAllocateInfoEntity entity, Throwable e) {
        // notify create resource failed
        log.warn("create resource failed for " + entity);
        Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                .item(AlarmUtils.ORGANIZATION_NAME, StrUtil.EMPTY)
                .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(entity.getTaskId()))
                .item(AlarmUtils.MESSAGE_NAME,
                        MessageFormat.format("create resource failed, jobId={0}, message={1}",
                                entity.getTaskId(), e.getMessage()))
                .build();
        AlarmUtils.alarm(AlarmEventNames.ALLOCATE_RESOURCE_FAILED, eventMessage);
    }

    // pick endpoints to release
    // derived logic here to impl you own strategy
    public List<SupervisorEndpointEntity> pickReleasedEndpoint(List<SupervisorEndpointEntity> entity) {
        if (CollectionUtils.isEmpty(entity)) {
            return entity;
        }
        List<SupervisorEndpointEntity> toReleased = new ArrayList<>(entity);
        toReleased.sort((e1, e2) -> e2.getUpdateTime().compareTo(e1.getUpdateTime()));
        SupervisorEndpointEntity first = toReleased.get(0);
        // maintain at most one agent for most 300 seconds
        if (Duration.between(first.getUpdateTime().toInstant(), Instant.now()).getSeconds() > 300) {
            return entity;
        } else {
            toReleased.remove(0);
            return toReleased;
        }
    }
}
