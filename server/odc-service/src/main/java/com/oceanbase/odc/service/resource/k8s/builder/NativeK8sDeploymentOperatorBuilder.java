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
package com.oceanbase.odc.service.resource.k8s.builder;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sDeploymentOperator;
import com.oceanbase.odc.service.resource.model.ResourceOperatorTag;

import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.NonNull;

/**
 * {@link NativeK8sDeploymentOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-04 15:19
 * @since ODC_release_4.3.2
 */
@Component
public class NativeK8sDeploymentOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<V1Deployment> {

    @Override
    protected boolean doSupports(@NonNull Class<?> clazz) {
        return V1Deployment.class.isAssignableFrom(clazz);
    }

    @Override
    public ResourceOperator<V1Deployment> build(@NonNull ResourceOperatorTag resourceOperatorTag) {
        return new NativeK8sDeploymentOperator(this.defaultNamespace);
    }

}
