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
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sDeploymentOperator;
import com.oceanbase.odc.service.resource.k8s.operator.NativeK8sPodOperator;
import com.oceanbase.odc.service.resource.k8s.status.K8sDeploymentStatusDfa;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NativeK8sDeploymentOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-10 11:28
 * @since ODC_release_4.3.2
 */
@Slf4j
@Component
public class NativeK8sDeploymentOperatorBuilder extends BaseNativeK8sResourceOperatorBuilder<K8sDeployment> {

    public NativeK8sDeploymentOperatorBuilder(
            @Autowired TaskFrameworkProperties frameworkProperties) throws IOException {
        super(frameworkProperties);
    }

    @Override
    protected boolean doMatch(String type) {
        return K8sDeployment.TYPE.equals(type);
    }

    @Override
    public ResourceOperator<K8sDeployment, K8sDeployment> build(@NonNull ResourceLocation resourceLocation) {
        return new NativeK8sDeploymentOperator(this.defaultNamespace,
                resourceLocation, new NativeK8sPodOperator(this.defaultNamespace, resourceLocation));
    }

    @Override
    public K8sDeployment toResource(ResourceEntity e, Optional<K8sDeployment> runtimeResource) {
        ResourceState nextState = e.getStatus();
        try {
            K8sDeploymentStatusDfa dfa = K8sDeploymentStatusDfa.buildInstance(e.getStatus());
            nextState = dfa.next(runtimeResource.orElse(null), e.getStatus());
        } catch (Exception exception) {
            log.warn("Failed to get next deployment's status, id={}", e.getId(), exception);
        }
        K8sDeployment k8sDeployment = new K8sDeployment(new ResourceLocation(e), nextState, null);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(e.getResourceName());
        meta.setNamespace(e.getNamespace());
        k8sDeployment.setMetadata(meta);
        if (runtimeResource.isPresent()) {
            k8sDeployment.setK8sPodList(runtimeResource.get().getK8sPodList());
            k8sDeployment.setKind(runtimeResource.get().getKind());
            k8sDeployment.setSpec(runtimeResource.get().getSpec());
            k8sDeployment.setStatus(runtimeResource.get().getStatus());
            k8sDeployment.setApiVersion(runtimeResource.get().getApiVersion());
        }
        return k8sDeployment;
    }

}
