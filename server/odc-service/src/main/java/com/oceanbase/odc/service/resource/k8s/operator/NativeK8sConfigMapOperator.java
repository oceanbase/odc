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

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import lombok.NonNull;

public class NativeK8sConfigMapOperator extends BaseNativeK8sResourceOperator<V1ConfigMap> {

    public NativeK8sConfigMapOperator(@NonNull String defaultNamespace) {
        super(defaultNamespace);
    }

    @Override
    protected V1ConfigMap doCreate(V1ConfigMap config) throws Exception {
        return new CoreV1Api().createNamespacedConfigMap(this.defaultNamespace, config, null, null, null, null);
    }

    @Override
    public V1ConfigMap patch(V1ConfigMap config) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public List<V1ConfigMap> list() throws Exception {
        return new CoreV1Api().listNamespacedConfigMap(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
    }

    @Override
    public void destroy(ResourceID key) throws Exception {
        if (key.getUniqueIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        new CoreV1Api().deleteNamespacedConfigMap(key.getUniqueIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
    }

}
