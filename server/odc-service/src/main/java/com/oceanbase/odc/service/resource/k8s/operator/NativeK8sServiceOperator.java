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
import com.oceanbase.odc.service.resource.k8s.model.K8sService;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.NonNull;

/**
 * {@link NativeK8sServiceOperator}
 *
 * @author yh263208
 * @date 2024-09-10 11:34
 * @since ODC_release_4.3.2
 */
public class NativeK8sServiceOperator extends BaseNativeK8sResourceOperator<K8sService> {

    public NativeK8sServiceOperator(@NonNull String defaultNamespace, @NonNull ResourceLocation resourceLocation) {
        super(defaultNamespace, resourceLocation);
    }

    @Override
    protected K8sService doCreate(K8sService resourceContext) throws Exception {
        V1Service svc = new CoreV1Api().createNamespacedService(
                this.defaultNamespace, resourceContext, null, null, null, null);
        resourceContext.setStatus(svc.getStatus());
        resourceContext.setResourceState(ResourceState.CREATING);
        resourceContext.setResourceLocation(this.resourceLocation);
        return resourceContext;
    }

    @Override
    public K8sService patch(ResourceID resourceID, K8sService resourceContext) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public String destroy(ResourceID resourceID) throws Exception {
        if (resourceID.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        V1Service svc = new CoreV1Api().deleteNamespacedService(resourceID.getIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
        return svc == null || svc.getMetadata() == null ? null : svc.getMetadata().getName();
    }

    @Override
    public List<K8sService> list() throws Exception {
        List<V1Service> svcs = new CoreV1Api().listNamespacedService(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
        return svcs.stream().map(item -> {
            K8sService svc = new K8sService(this.resourceLocation, ResourceState.UNKNOWN);
            svc.setStatus(item.getStatus());
            svc.setKind(item.getKind());
            svc.setMetadata(item.getMetadata());
            svc.setApiVersion(item.getApiVersion());
            svc.setSpec(item.getSpec());
            return svc;
        }).collect(Collectors.toList());
    }

}
