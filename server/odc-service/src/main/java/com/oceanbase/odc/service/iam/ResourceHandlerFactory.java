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
package com.oceanbase.odc.service.iam;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: Lebie
 * @Date: 2025/2/27 15:17
 * @Description: []
 */
@Component
public class ResourceHandlerFactory {
    private final List<ResourceHandler> handlers;

    @Autowired
    public ResourceHandlerFactory(List<ResourceHandler> handlers) {
        this.handlers = handlers;
    }

    public ResourceHandler getHandler(String resourceType) {
        return handlers.stream()
                .filter(handler -> handler.supports(resourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler found for resource type: " + resourceType));
    }
}
