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

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceOperator;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sConfigMap;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sConfigMapOperator;
import com.oceanbase.odc.service.resource.k8s.status.K8sConfigMapStatusDfa;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NativeK8sConfigMapOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-07 18:55
 * @since ODC_release_4.3.2
 */
@Slf4j
@Component
public class NativeK8sConfigMapOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<K8sConfigMap> {

    public NativeK8sConfigMapOperatorBuilder(
            @Autowired TaskFrameworkProperties frameworkProperties) throws IOException {
        super(frameworkProperties);
    }

    @Override
    protected boolean doMatches(String type) {
        return K8sConfigMap.TYPE.equals(type);
    }

    @Override
    public K8sConfigMap toResource(ResourceEntity e, Optional<K8sConfigMap> runtimeResource) {
        ResourceState nextState = e.getStatus();
        try {
            K8sConfigMapStatusDfa dfa = K8sConfigMapStatusDfa.buildInstance();
            nextState = dfa.next(runtimeResource.orElse(null), e.getStatus());
        } catch (Exception exception) {
            log.warn("Failed to get next configmap's status, id={}", e.getId(), exception);
        }
        K8sConfigMap k8sConfigMap = new K8sConfigMap(new ResourceLocation(e), nextState);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(e.getResourceName());
        meta.setNamespace(e.getNamespace());
        k8sConfigMap.setMetadata(meta);
        if (runtimeResource.isPresent()) {
            k8sConfigMap.setData(runtimeResource.get().getData());
            k8sConfigMap.setKind(runtimeResource.get().getKind());
            k8sConfigMap.setImmutable(runtimeResource.get().getImmutable());
            k8sConfigMap.setApiVersion(runtimeResource.get().getApiVersion());
            k8sConfigMap.setBinaryData(runtimeResource.get().getBinaryData());
        }
        return k8sConfigMap;
    }


    @Override
    public ResourceOperator<K8sConfigMap, K8sConfigMap> build(@NonNull ResourceLocation resourceLocation) {
        return new NativeK8sConfigMapOperator(this.defaultNamespace, resourceLocation);
    }

}
