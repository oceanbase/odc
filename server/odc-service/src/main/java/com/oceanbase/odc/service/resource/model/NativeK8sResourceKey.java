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
package com.oceanbase.odc.service.resource.model;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link NativeK8sResourceKey}
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class NativeK8sResourceKey {

    private String name;
    private Class<?> type;
    private String namespace;

    public NativeK8sResourceKey(@NonNull V1ObjectMeta objectMeta, @NonNull Class<?> type) {
        this.type = type;
        this.name = objectMeta.getName();
        this.namespace = objectMeta.getNamespace();
    }

}
