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
package com.oceanbase.odc.service.resource.local;

import com.oceanbase.odc.service.resource.ResourceContext;

import lombok.AllArgsConstructor;

/**
 * @author longpeng.zlp
 * @date 2024/8/14 10:09
 */
@AllArgsConstructor
public class LocalResourceContext implements ResourceContext {

    private final double cpuCore;
    private final long memInMB;

    @Override
    public double cpuCore() {
        return 1;
    }

    @Override
    public long memInMB() {
        return 0;
    }

    @Override
    public String region() {
        return "local";
    }

    @Override
    public String resourceGroup() {
        return "local";
    }

    @Override
    public String resourceNamespace() {
        return "local";
    }

    @Override
    public String resourceName() {
        return "local";
    }
}
