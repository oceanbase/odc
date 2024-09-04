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

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.NonNull;

/**
 * {@link NativeK8sPodOperator}
 *
 * @author yh263208
 * @date 2024-09-02 17:09
 * @since ODC_release_4.3.2
 */
public class NativeK8sPodOperator extends BaseNativeK8sResourceOperator<V1Pod> {

    public NativeK8sPodOperator(@NonNull String namespace) {
        super(namespace);
    }

    @Override
    public V1Pod create(@NonNull V1Pod config) throws Exception {
        return new CoreV1Api().createNamespacedPod(this.defaultNamespace, config, null, null, null, null);
    }

    @Override
    public V1Pod patch(V1Pod config) throws Exception {
        throw new UnsupportedOperationException("Unsupported yet");
    }

    @Override
    public List<V1Pod> list() throws ApiException {
        return new CoreV1Api().listNamespacedPod(this.defaultNamespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
    }

    @Override
    public void destroy(@NonNull ResourceID key) throws Exception {
        if (key.getUniqueIdentifier() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        new CoreV1Api().deleteNamespacedPod(key.getUniqueIdentifier(),
                this.defaultNamespace, null, null, null, null, null, null);
    }

}
