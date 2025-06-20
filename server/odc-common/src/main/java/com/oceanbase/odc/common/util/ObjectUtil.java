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
package com.oceanbase.odc.common.util;

import com.oceanbase.odc.common.json.JsonUtils;

/**
 * @author yizhou.xw
 * @version : ObjectUtil.java, v 0.1 2021-02-07 14:50
 */
public class ObjectUtil {

    public static <T> T deepCopy(T object, Class<T> classType) {
        String json = JsonUtils.unsafeToJson(object);
        return JsonUtils.fromJson(json, classType);
    }

    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object != null ? object : defaultValue;
    }

}
