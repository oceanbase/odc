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

import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.model.NativeK8sResourceID;

import cn.hutool.core.collection.CollectionUtil;
import io.kubernetes.client.common.KubernetesObject;
import lombok.NonNull;

/**
 * {@link BaseNativeK8sResourceOperator}
 *
 * @author yh263208
 * @date 2024-09-02 17:39
 * @since ODC_release_4.3.2
 */
public abstract class BaseNativeK8sResourceOperator<T extends KubernetesObject>
        implements ResourceOperator<T, NativeK8sResourceID> {

    protected final String defaultNamespace;

    public BaseNativeK8sResourceOperator(@NonNull String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public Optional<T> get(NativeK8sResourceID key) throws Exception {
        List<T> list = list();
        if (CollectionUtil.isEmpty(list)) {
            return Optional.empty();
        }
        return list.stream().filter(t -> Objects.equals(getKey(t), key)).findFirst();
    }

}
