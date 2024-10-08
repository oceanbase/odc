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
package com.oceanbase.odc.service.resource.k8s;

import java.util.Optional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperatorBuilder;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClientSelector;

import lombok.AllArgsConstructor;

/**
 * default impl for k8s resource operator
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 17:33
 */
@AllArgsConstructor
public class DefaultResourceOperatorBuilder implements ResourceOperatorBuilder<K8sResourceContext, K8sPodResource> {
    public static final String CLOUD_K8S_POD_TYPE = "cloudK8sPod";
    private final K8sJobClientSelector k8sJobClientSelector;
    private final long podPendingTimeoutSeconds;
    private final ResourceRepository resourceRepository;

    @Override
    public K8sResourceOperator build(ResourceLocation resourceLocation) {
        return new K8sResourceOperator(new K8sResourceOperatorContext(k8sJobClientSelector,
                this::getResourceCreateTimeInSeconds, podPendingTimeoutSeconds));
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
        return new K8sPodResource(
                resourceEntity.getRegion(),
                resourceEntity.getGroupName(),
                resourceEntity.getResourceType(),
                resourceEntity.getNamespace(),
                resourceEntity.getResourceName(),
                resourceEntity.getStatus(),
                resourceEntity.getEndpoint(),
                resourceEntity.getCreateTime());
    }

    /**
     * cloud K8s pod match this builder
     * 
     * @return
     */
    @Override
    public boolean matches(String type) {
        return StringUtils.equalsIgnoreCase(type, CLOUD_K8S_POD_TYPE);
    }
}
