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

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sPodOperator;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.NonNull;

/**
 * {@link NativeK8sPodOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-06 20:51
 * @since ODC_release_4.3.2
 */
@Component
public class NativeK8sPodOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<K8sPod> {

    @Override
    protected boolean doMatch(String type) {
        return K8sPod.TYPE.equals(type);
    }

    @Override
    public K8sPod toResource(ResourceEntity e) {
        K8sPod k8sPod = new K8sPod(new ResourceLocation(e), e.getStatus());
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(e.getResourceName());
        meta.setNamespace(e.getNamespace());
        k8sPod.setMetadata(meta);
        return k8sPod;
    }

    @Override
    public ResourceOperator<K8sPod, K8sPod> build(@NonNull ResourceLocation location) {
        return new NativeK8sPodOperator(this.defaultNamespace, location);
    }

}
