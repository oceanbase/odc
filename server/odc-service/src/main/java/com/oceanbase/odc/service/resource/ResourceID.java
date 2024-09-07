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
package com.oceanbase.odc.service.resource;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * resource id all resource entity id should contains this class resource id should be impl as
 * global unique for possible
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 16:47
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class ResourceID {
    /**
     * location of the resource
     */
    private final ResourceLocation resourceLocation;

    /**
     * resource type, this type will used to choose a {@link ResourceOperator} of
     * {@link #resourceLocation} eg: k8s pod resource type, k8s service resource type
     */
    private final String type;

    /**
     * name space of the resource eg: a namespace of k8s cluster
     */
    private final String namespace;

    /**
     * identifier of resource. eg: a pod name
     */
    private final String identifier;
}
