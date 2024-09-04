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

import com.oceanbase.odc.service.resource.model.ResourceID;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.NonNull;

/**
 * {@link NativeK8sDeploymentOperator}
 *
 * @author yh263208
 * @date 2024-09-04 15:07
 * @since ODC_release_4.3.2
 */
public class NativeK8sDeploymentOperator extends BaseNativeK8sResourceOperator<V1Deployment> {

    public NativeK8sDeploymentOperator(@NonNull String defaultNamespace) {
        super(defaultNamespace);
    }

    @Override
    protected V1Deployment doCreate(V1Deployment config) throws Exception {
        return new AppsV1Api().createNamespacedDeployment(this.defaultNamespace, config, null, null, null, null);
    }

    @Override
    public V1Deployment patch(V1Deployment config) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public List<V1Deployment> list() throws Exception {
        return new AppsV1Api().listNamespacedDeployment(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
    }

    @Override
    public void destroy(ResourceID key) throws Exception {
        if (key.getUniqueIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        new AppsV1Api().deleteNamespacedDeployment(key.getUniqueIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
    }

}
