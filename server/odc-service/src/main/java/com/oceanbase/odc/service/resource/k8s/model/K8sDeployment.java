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
package com.oceanbase.odc.service.resource.k8s.model;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.ResourceState;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link K8sDeployment}
 *
 * @author yh263208
 * @date 2024-09-10 11:18
 * @since ODC_release_4.3.2
 */
@Getter
@Setter
@NoArgsConstructor
public class K8sDeployment extends V1Deployment implements K8sResource {

    public static final String TYPE = "K8S_DEPLOYMENT";
    private ResourceState resourceState;
    private ResourceLocation resourceLocation;
    private List<K8sPod> k8sPodList = null;
    private ResourceOperator<K8sPod, K8sPod> resourceOperator;

    public K8sDeployment(@NonNull ResourceLocation resourceLocation, @NonNull ResourceState resourceState) {
        this.resourceState = resourceState;
        this.resourceLocation = resourceLocation;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ResourceState resourceState() {
        return this.resourceState;
    }

    @Override
    public ResourceLocation resourceLocation() {
        return this.resourceLocation;
    }

    public synchronized List<K8sPod> getK8sPods() throws Exception {
        if (this.k8sPodList != null) {
            return this.k8sPodList;
        }
        if (this.resourceOperator == null) {
            throw new IllegalStateException("K8s Pod operator is null");
        }
        this.k8sPodList = this.resourceOperator.list().stream().filter(pod -> {
            if (!pod.resourceName().startsWith(resourceName())
                    || pod.getMetadata() == null) {
                return false;
            }
            V1ObjectMeta meta = pod.getMetadata();
            return pod.resourceID().equals(
                    new ResourceID(resourceLocation(), K8sPod.TYPE, meta.getNamespace(), meta.getName()));
        }).collect(Collectors.toList());
        return this.k8sPodList;
    }

}
