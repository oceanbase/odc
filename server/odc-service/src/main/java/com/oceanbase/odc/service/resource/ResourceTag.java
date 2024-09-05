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

import com.oceanbase.odc.metadb.resource.ResourceLocation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/9/3 17:16
 */
@AllArgsConstructor
@Data
public class ResourceTag {
    /**
     * location of the resource
     */
    private final ResourceLocation resourceLocation;
    /**
     * type of the resource, eg service, pod. this field will be used to select resource operator
     */
    private final String type;
}
