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
package com.oceanbase.odc.service.resource.k8s.operator;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.NonNull;

/**
 * {@link NativeK8sPodOperator}
 *
 * @author yh263208
 * @date 2024-09-06 20:25
 * @since ODC_release_4.3.2
 */
public class NativeK8sPodOperator extends BaseNativeK8sResourceOperator<K8sPod> {

    public NativeK8sPodOperator(@NonNull String defaultNamespace, @NonNull ResourceLocation resourceLocation) {
        super(defaultNamespace, resourceLocation);
    }

    @Override
    protected K8sPod doCreate(K8sPod resourceContext) throws Exception {
        V1Pod pod = new CoreV1Api().createNamespacedPod(
                this.defaultNamespace, resourceContext, null, null, null, null);
        resourceContext.setStatus(pod.getStatus());
        resourceContext.setResourceState(ResourceState.CREATING);
        resourceContext.setResourceLocation(this.resourceLocation);
        return resourceContext;
    }

    @Override
    public String destroy(ResourceID resourceID) throws Exception {
        if (resourceID.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        V1Pod pod = new CoreV1Api().deleteNamespacedPod(resourceID.getIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
        return pod == null || pod.getMetadata() == null ? null : pod.getMetadata().getName();
    }

    @Override
    public List<K8sPod> list() throws Exception {
        List<V1Pod> pods = new CoreV1Api().listNamespacedPod(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
        return pods.stream().map(v1Pod -> {
            K8sPod k8sPod = new K8sPod(this.resourceLocation, ResourceState.UNKNOWN);
            k8sPod.setStatus(v1Pod.getStatus());
            k8sPod.setKind(v1Pod.getKind());
            k8sPod.setMetadata(v1Pod.getMetadata());
            k8sPod.setApiVersion(v1Pod.getApiVersion());
            k8sPod.setSpec(v1Pod.getSpec());
            return k8sPod;
        }).collect(Collectors.toList());
    }

    @Override
    public K8sPod patch(ResourceID resourceID, K8sPod resourceContext) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

}
