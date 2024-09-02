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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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
public abstract class BaseNativeK8sResourceOperator<T extends KubernetesObject> implements ResourceOperator<T> {

    protected final String namespace;

    public BaseNativeK8sResourceOperator(@NonNull String namespace) {
        this.namespace = namespace;
    }

    public Optional<T> find(T config) throws Exception {
        String name = null;
        if (config.getMetadata() != null) {
            name = config.getMetadata().getName();
            String ns = config.getMetadata().getNamespace();
            if (ns != null && !this.namespace.equals(ns)) {
                return Optional.empty();
            }
        }
        if (name == null) {
            return Optional.empty();
        }
        final String rName = name;
        List<T> list = list();
        if (CollectionUtil.isEmpty(list)) {
            return Optional.empty();
        }
        return list.stream().filter(t -> {
            if (t.getMetadata() == null) {
                return false;
            }
            return StringUtils.equals(rName, t.getMetadata().getName());
        }).findFirst();
    }

}
