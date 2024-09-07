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
import java.util.Objects;
import java.util.Optional;

import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.k8s.model.K8sResource;

import cn.hutool.core.collection.CollectionUtil;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.NonNull;

/**
 * {@link BaseNativeK8sResourceOperator}
 *
 * @author yh263208
 * @date 2024-09-02 17:39
 * @since ODC_release_4.3.2
 */
public abstract class BaseNativeK8sResourceOperator<T extends K8sResource> implements ResourceOperator<T, T> {

    protected final String defaultNamespace;
    protected final ResourceLocation resourceLocation;

    public BaseNativeK8sResourceOperator(@NonNull String defaultNamespace,
            @NonNull ResourceLocation resourceLocation) {
        this.defaultNamespace = defaultNamespace;
        this.resourceLocation = resourceLocation;
    }

    @Override
    public T create(T resourceContext) throws Exception {
        V1ObjectMeta meta = resourceContext.getMetadata();
        String namespace = null;
        if (meta != null) {
            namespace = meta.getNamespace();
            meta.setNamespace(this.defaultNamespace);
        }
        try {
            return doCreate(resourceContext);
        } finally {
            if (meta != null) {
                meta.setNamespace(namespace);
            }
        }
    }

    @Override
    public Optional<T> query(ResourceID resourceID) throws Exception {
        List<T> list = list();
        if (CollectionUtil.isEmpty(list)) {
            return Optional.empty();
        }
        return list.stream().filter(t -> Objects.equals(t.resourceID(), resourceID)).findFirst();
    }

    @Override
    public boolean canBeDestroyed(ResourceID resourceID) {
        return true;
    }

    protected abstract T doCreate(T resourceContext) throws Exception;

}
