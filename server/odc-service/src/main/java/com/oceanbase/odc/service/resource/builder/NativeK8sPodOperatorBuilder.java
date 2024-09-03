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
package com.oceanbase.odc.service.resource.builder;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.resource.model.K8sResourceKey;
import com.oceanbase.odc.service.resource.operator.NativeK8sPodOperator;
import com.oceanbase.odc.service.resource.operator.ResourceOperator;

import io.kubernetes.client.openapi.models.V1Pod;
import lombok.NonNull;

/**
 * {@link NativeK8sPodOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-02 18:27
 * @since ODC_release_4.3.2
 */
@Component
public class NativeK8sPodOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<V1Pod, K8sResourceKey> {

    @Override
    protected boolean doSupports(@NonNull Class<?> clazz) {
        return V1Pod.class.equals(clazz);
    }

    @Override
    protected ResourceOperator<V1Pod, K8sResourceKey> doBuild(@NonNull String defaultNamespace) {
        return new NativeK8sPodOperator(defaultNamespace);
    }

}
