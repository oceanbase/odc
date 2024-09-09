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

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.service.resource.ResourceOperatorBuilder;
import com.oceanbase.odc.service.resource.k8s.client.NativeK8sJobClient;
import com.oceanbase.odc.service.resource.k8s.model.K8sResource;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import lombok.NonNull;

/**
 * {@link BaseNativeK8sResourceOperatorBuilder}
 *
 * @author yh263208
 * @date 2024-09-06 20:40
 * @since ODC_release_4.3.2
 */
public abstract class BaseNativeK8sResourceOperatorBuilder<T extends K8sResource>
        implements ResourceOperatorBuilder<T, T> {

    private boolean apiClientSet = false;
    protected String defaultNamespace = "default";

    public BaseNativeK8sResourceOperatorBuilder(TaskFrameworkProperties frameworkProperties) throws IOException {
        K8sProperties properties = frameworkProperties.getK8sProperties();
        if (properties != null) {
            ApiClient apiClient = NativeK8sJobClient.generateNativeK8sApiClient(properties);
            if (apiClient != null) {
                this.apiClientSet = true;
                Configuration.setDefaultApiClient(apiClient);
            }
            if (StringUtils.isNotBlank(properties.getNamespace())) {
                this.defaultNamespace = properties.getNamespace();
            }
        }
    }

    protected abstract boolean doMatch(String type);

    @Override
    public boolean match(@NonNull String type) {
        return this.apiClientSet && doMatch(type);
    }

    @Override
    public ResourceEntity toResourceEntity(T resource) {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceName(resource.resourceName());
        resourceEntity.setResourceProperties(null);
        resourceEntity.setResourceType(resource.type());
        resourceEntity.setEndpoint("N/A");
        resourceEntity.setNamespace(this.defaultNamespace);
        return resourceEntity;
    }

}
