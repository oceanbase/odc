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
package com.oceanbase.odc.service.resource.k8s.model;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.resource.Resource;
import com.oceanbase.odc.service.resource.ResourceContext;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

/**
 * {@link K8sResource}
 *
 * @author yh263208
 * @date 2024-09-06 19:54
 * @since ODC_release_4.3.2
 */
public interface K8sResource extends KubernetesObject, Resource, ResourceContext {

    ResourceLocation resourceLocation();

    @Override
    default String resourceName() {
        V1ObjectMeta meta = getMetadata();
        Verify.notNull(meta, "Meta data");
        return meta.getName();
    }

    @Override
    default ResourceID resourceID() {
        V1ObjectMeta meta = getMetadata();
        Verify.notNull(meta, "Meta data");
        return new ResourceID(resourceLocation(), type(), meta.getNamespace(), meta.getName());
    }

}
