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
package com.oceanbase.odc.metadb.resource;

/**
 * resource id all resource entity id should contains this class
 * 
 * @author longpeng.zlp
 * @date 2024/9/2 16:47
 */
public class ResourceID {
    /**
     * location of the resource
     */
    private final ResourceLocation resourceLocation;

    /**
     * name space of the resource eg: a namespace of k8s cluster
     */
    private final String namespace;

    /**
     * name of resource. eg: a pod name
     */
    private final String name;

    public ResourceID(ResourceLocation resourceLocation, String namespace, String name) {
        this.resourceLocation = resourceLocation;
        this.namespace = namespace;
        this.name = name;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
