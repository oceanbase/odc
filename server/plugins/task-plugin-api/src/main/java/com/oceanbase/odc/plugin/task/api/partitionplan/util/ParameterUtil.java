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
package com.oceanbase.odc.plugin.task.api.partitionplan.util;

import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;

import lombok.NonNull;

/**
 * {@link ParameterUtil}
 *
 * @author yh263208
 * @date 2024-01-19 17:29
 * @since ODC_release_4.2.4
 */
public class ParameterUtil {

    public static <T> T nullSafeExtract(@NonNull Map<String, Object> parameters,
            @NonNull String key, @NonNull Class<T> clazz) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing value for key, " + key);
        }
        String json = JsonUtils.toJson(value);
        T config = JsonUtils.fromJson(json, clazz);
        if (config == null) {
            throw new IllegalArgumentException("Illegal parameter, " + json);
        }
        return config;
    }

}
