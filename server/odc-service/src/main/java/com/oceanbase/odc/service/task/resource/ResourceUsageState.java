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
package com.oceanbase.odc.service.task.resource;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * state for resource usage, only update by resource user
 * 
 * @author longpeng.zlp
 * @date 2024/12/4 17:57
 */
public enum ResourceUsageState {
    PREPARING,
    USING,
    FINISHED;

    public static ResourceUsageState fromString(String usageState) {
        return ResourceUsageState.valueOf(StringUtils.upperCase(StringUtils.trim(usageState)));
    }

    public boolean equal(String usageState) {
        return StringUtils.equalsIgnoreCase(name(), StringUtils.trim(usageState));
    }
}
