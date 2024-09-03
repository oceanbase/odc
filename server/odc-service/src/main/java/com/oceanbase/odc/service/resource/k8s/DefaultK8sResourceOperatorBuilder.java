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

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClient;
import com.oceanbase.odc.service.resource.k8s.client.K8sJobClientSelector;

import lombok.AllArgsConstructor;

/**
 * default impl for k8s resource operator
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 17:33
 */
@AllArgsConstructor
public class DefaultK8sResourceOperatorBuilder implements K8sResourceOperatorBuilder {
    private final K8sJobClientSelector k8sJobClientSelector;
    private final long podPendingTimeoutSeconds;
    private final ResourceRepository resourceRepository;

    @Override
    public K8SResourceOperator build(String region, String group) {
        K8sJobClient k8sJobClient = k8sJobClientSelector.select(region);
        return new K8SResourceOperator(new K8sResourceOperatorContext(k8sJobClient,
                this::getResourceCreateTimeInSeconds, podPendingTimeoutSeconds));
    }

    /**
     * query k8s resource create time from meta store
     *
     * @param resourceID
     * @return
     */
    private long getResourceCreateTimeInSeconds(K8sPodResourceID resourceID) {
        Optional<ResourceEntity> resource = resourceRepository.findByResourceID(resourceID);
        if (resource.isPresent()) {
            return (System.currentTimeMillis() - resource.get().getCreateTime().getTime()) / 1000;
        } else {
            return 0;
        }
    }
}
