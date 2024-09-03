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
package com.oceanbase.odc.service.resource.operator;

import java.util.List;

import com.oceanbase.odc.service.resource.model.NativeK8sResourceID;

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
        String namespace = this.defaultNamespace;
        if (config.getMetadata() != null && config.getMetadata().getNamespace() != null) {
            namespace = config.getMetadata().getNamespace();
        }
        return new CoreV1Api().createNamespacedPod(namespace, config, null, null, null, null);
    }

    @Override
    public NativeK8sResourceID getKey(V1Pod config) {
        return new NativeK8sResourceID(config.getMetadata(), V1Pod.class);
    }

    @Override
    protected List<V1Pod> list(String namespace) throws ApiException {
        return new CoreV1Api().listNamespacedPod(namespace,
                null, null, null, null, null, null, null, null, null, null, null).getItems();
    }

    @Override
    public void destroy(@NonNull NativeK8sResourceID key) throws Exception {
        if (key.getName() == null) {
            throw new IllegalArgumentException("Resource name is null");
        }
        String namespace = this.defaultNamespace;
        if (key.getNamespace() != null) {
            namespace = key.getNamespace();
        }
        new CoreV1Api().deleteNamespacedPod(key.getName(), namespace, null, null, null, null, null, null);
    }

}
