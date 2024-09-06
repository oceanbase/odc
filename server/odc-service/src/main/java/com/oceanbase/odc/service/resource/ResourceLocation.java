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
import lombok.Getter;

/**
 * location of the resource
 * 
 * @author longpeng.zlp
 * @date 2024/9/4 10:42
 */
@AllArgsConstructor
@Getter
public class ResourceLocation {
    /**
     * region of the resource, that associate to a geography area eg: shanghai/beijing/singapore
     */
    private final String region;
    /**
     * group of resource. eg: a k8s cluster of alibaba cloud a k8s cluster of tencent cloud a vpc of aws
     * cloud this group will be associated to a management endpoint
     */
    private final String group;
}
