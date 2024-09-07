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
import com.oceanbase.odc.service.resource.k8s.model.K8sConfigMap;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Status;
import lombok.NonNull;

/**
 * {@link NativeK8sConfigMapOperator}
 *
 * @author yh263208
 * @date 2024-09-07 18:47
 * @since ODC_release_4.3.2
 */
public class NativeK8sConfigMapOperator extends BaseNativeK8sResourceOperator<K8sConfigMap> {

    public NativeK8sConfigMapOperator(@NonNull String defaultNamespace, @NonNull ResourceLocation resourceLocation) {
        super(defaultNamespace, resourceLocation);
    }

    @Override
    protected K8sConfigMap doCreate(K8sConfigMap resourceContext) throws Exception {
        new CoreV1Api().createNamespacedConfigMap(
                this.defaultNamespace, resourceContext, null, null, null, null);
        resourceContext.setResourceState(ResourceState.CREATING);
        return resourceContext;
    }

    @Override
    public String destroy(ResourceID resourceID) throws Exception {
        if (resourceID.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        V1Status v1Status = new CoreV1Api().deleteNamespacedConfigMap(
                resourceID.getIdentifier(), this.defaultNamespace, null, null, null, null, null, null);
        return v1Status == null ? null : v1Status.getStatus();
    }

    @Override
    public List<K8sConfigMap> list() throws Exception {
        List<V1ConfigMap> configMaps = new CoreV1Api().listNamespacedConfigMap(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
        return configMaps.stream().map(item -> {
            K8sConfigMap configMap = new K8sConfigMap(this.resourceLocation, ResourceState.UNKNOWN);
            configMap.setKind(item.getKind());
            configMap.setMetadata(item.getMetadata());
            configMap.setApiVersion(item.getApiVersion());
            configMap.setData(item.getData());
            configMap.setBinaryData(item.getBinaryData());
            configMap.setImmutable(item.getImmutable());
            return configMap;
        }).collect(Collectors.toList());
    }

    @Override
    public K8sConfigMap patch(ResourceID resourceID, K8sConfigMap resourceContext) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

}
