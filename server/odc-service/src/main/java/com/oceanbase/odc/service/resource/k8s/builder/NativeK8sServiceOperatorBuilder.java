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
import com.oceanbase.odc.service.resource.k8s.model.K8sService;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sServiceOperator;
import com.oceanbase.odc.service.resource.k8s.status.K8sServiceStatusDfa;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NativeK8sServiceOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-10 11:38
 * @since ODC_release_4.3.2
 */
@Slf4j
@Component
public class NativeK8sServiceOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<K8sService> {

    public NativeK8sServiceOperatorBuilder(@Autowired TaskFrameworkProperties frameworkProperties) throws IOException {
        super(frameworkProperties);
    }

    @Override
    protected boolean doMatches(String type) {
        return K8sService.TYPE.equals(type);
    }

    @Override
    public ResourceOperator<K8sService, K8sService> build(@NonNull ResourceLocation resourceLocation) {
        return new NativeK8sServiceOperator(this.defaultNamespace, resourceLocation);
    }

    @Override
    public K8sService toResource(ResourceEntity e, Optional<K8sService> runtimeResource) {
        ResourceState nextState = e.getStatus();
        try {
            K8sServiceStatusDfa dfa = K8sServiceStatusDfa.buildInstance();
            nextState = dfa.next(runtimeResource.orElse(null), e.getStatus());
        } catch (Exception exception) {
            log.warn("Failed to get next service's status, id={}", e.getId(), exception);
        }
        K8sService k8sService = new K8sService(new ResourceLocation(e), nextState);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(e.getResourceName());
        meta.setNamespace(e.getNamespace());
        k8sService.setMetadata(meta);
        if (runtimeResource.isPresent()) {
            k8sService.setKind(runtimeResource.get().getKind());
            k8sService.setSpec(runtimeResource.get().getSpec());
            k8sService.setStatus(runtimeResource.get().getStatus());
            k8sService.setApiVersion(runtimeResource.get().getApiVersion());
        }
        return k8sService;
    }

}
