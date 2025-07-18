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

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperatorBuilder;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.resource.client.K8sJobClientSelector;

import lombok.extern.slf4j.Slf4j;

/**
 * default impl for k8s resource operator
 *
 * @author longpeng.zlp
 * @date 2024/9/2 17:33
 */
@Slf4j
public abstract class AbstractK8sResourceOperatorBuilder
        implements ResourceOperatorBuilder<K8sResourceContext, K8sPodResource> {
    public static final String CLOUD_K8S_POD_TYPE = "cloudK8sPod";
    protected K8sJobClientSelector k8sJobClientSelector;
    protected K8sProperties k8sProperties;
    protected ResourceRepository resourceRepository;
    protected String operatorType;

    public AbstractK8sResourceOperatorBuilder(TaskFrameworkProperties taskFrameworkProperties,
            ResourceRepository resourceRepository, String typeName) throws IOException {
        this.k8sProperties = taskFrameworkProperties.getK8sProperties();
        this.resourceRepository = resourceRepository;
        this.k8sJobClientSelector = buildK8sJobSelector(taskFrameworkProperties);
        this.operatorType = typeName;
    }

    /**
     * create k8s client selector
     * 
     * @param taskFrameworkProperties
     * @return
     */
    protected abstract K8sJobClientSelector buildK8sJobSelector(TaskFrameworkProperties taskFrameworkProperties)
            throws IOException;

    @Override
    public K8sResourceOperator build(ResourceLocation resourceLocation) {
        return new K8sResourceOperator(new K8sResourceOperatorContext(k8sJobClientSelector,
                this::getResourceCreateTimeInSeconds, k8sProperties.getPodPendingTimeoutSeconds()));
    }

    /**
     * query k8s resource create time from meta store
     *
     * @param resourceID
     * @return
     */
    private long getResourceCreateTimeInSeconds(ResourceID resourceID) {
        Optional<ResourceEntity> resource = resourceRepository.findByResourceID(resourceID);
        if (resource.isPresent()) {
            return (System.currentTimeMillis() - resource.get().getCreateTime().getTime()) / 1000;
        } else {
            return 0;
        }
    }

    /**
     * convert k8s resource to resource entity
     *
     * @param k8sResource
     * @return
     */
    public ResourceEntity toResourceEntity(K8sPodResource k8sResource) {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType(k8sResource.type());
        resourceEntity.setEndpoint(k8sResource.endpoint().getResourceUrl());
        resourceEntity.setCreateTime(k8sResource.createDate());
        resourceEntity.setRegion(k8sResource.getRegion());
        resourceEntity.setGroupName(k8sResource.getGroup());
        resourceEntity.setNamespace(k8sResource.getNamespace());
        resourceEntity.setResourceName(k8sResource.getArn());
        resourceEntity.setStatus(k8sResource.getResourceState());
        return resourceEntity;
    }

    @Override
    public K8sPodResource toResource(ResourceEntity resourceEntity, Optional<K8sPodResource> runtimeResource) {
        Pair<String, String> ipAndPort = runtimeResource
                .map(r -> Pair.of(r.getPodIpAddress(), r.getServicePort()))
                .orElse(K8sPodResource.parseIPAndPort(resourceEntity.getEndpoint()));
        return new K8sPodResource(
                resourceEntity.getRegion(),
                resourceEntity.getGroupName(),
                resourceEntity.getResourceType(),
                resourceEntity.getNamespace(),
                resourceEntity.getResourceName(),
                resourceEntity.getStatus(),
                ipAndPort.getLeft(),
                ipAndPort.getRight(),
                resourceEntity.getCreateTime());
    }

    /**
     * builder matcher
     *
     * @return
     */
    @Override
    public boolean matches(String type) {
        return StringUtils.equalsIgnoreCase(type, operatorType);
    }
}
