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
package com.oceanbase.odc.service.resource.k8s;

import com.oceanbase.odc.service.resource.ResourceContext;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2024/8/26 11:32
 */
@AllArgsConstructor
@Getter
public class K8sResourceContext implements ResourceContext {
    /**
     * default pod config for current k8s region
     */
    private final PodConfig podConfig;
    /**
     * job name to run for this resource allocate
     */
    private final String resourceName;
    /**
     * region of the resource
     */
    private final String region;

    /**
     * group of the resource
     */
    private final String group;

    /**
     * type to choose {@link com.oceanbase.odc.service.resource.ResourceOperator}
     */
    private final String type;

    /**
     * extended for debug
     */
    private final Object extraData;

    public String region() {
        return region;
    }

    public String resourceGroup() {
        return group;
    }

    public String resourceNamespace() {
        return podConfig.getNamespace();
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }
}
