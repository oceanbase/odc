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
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Status;
import lombok.NonNull;

/**
 * {@link NativeK8sDeploymentOperator}
 *
 * @author yh263208
 * @date 2024-09-10 11:19
 * @since ODC_release_4.3.2
 */
public class NativeK8sDeploymentOperator extends BaseNativeK8sResourceOperator<K8sDeployment> {

    private final ResourceOperator<K8sPod, K8sPod> resourceOperator;

    public NativeK8sDeploymentOperator(@NonNull String defaultNamespace,
            @NonNull ResourceLocation resourceLocation,
            @NonNull ResourceOperator<K8sPod, K8sPod> resourceOperator) {
        super(defaultNamespace, resourceLocation);
        this.resourceOperator = resourceOperator;
    }

    @Override
    protected K8sDeployment doCreate(K8sDeployment resourceContext) throws Exception {
        V1Deployment deployment = new AppsV1Api().createNamespacedDeployment(
                this.defaultNamespace, resourceContext, null, null, null, null);
        resourceContext.setStatus(deployment.getStatus());
        resourceContext.setResourceState(ResourceState.CREATING);
        resourceContext.setResourceLocation(this.resourceLocation);
        resourceContext.setResourceOperator(this.resourceOperator);
        return resourceContext;
    }

    @Override
    public K8sDeployment patch(ResourceID resourceID, K8sDeployment resourceContext) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public String destroy(ResourceID resourceID) throws Exception {
        if (resourceID.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        V1Status status = new AppsV1Api().deleteNamespacedDeployment(resourceID.getIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
        return status == null ? null : status.getStatus();
    }

    @Override
    public List<K8sDeployment> list() throws Exception {
        List<V1Deployment> deployments = new AppsV1Api().listNamespacedDeployment(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
        return deployments.stream().map(item -> {
            K8sDeployment deployment = new K8sDeployment(
                    this.resourceLocation, ResourceState.UNKNOWN, this.resourceOperator);
            deployment.setStatus(item.getStatus());
            deployment.setKind(item.getKind());
            deployment.setMetadata(item.getMetadata());
            deployment.setApiVersion(item.getApiVersion());
            deployment.setSpec(item.getSpec());
            return deployment;
        }).collect(Collectors.toList());
    }

}
