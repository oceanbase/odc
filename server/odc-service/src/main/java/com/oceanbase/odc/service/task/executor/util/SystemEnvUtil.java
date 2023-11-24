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

package com.oceanbase.odc.service.task.executor.util;

import java.util.Objects;

import com.oceanbase.odc.common.util.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/24 14:05
 */
@Slf4j
public class SystemEnvUtil {

    public static String nullSafeGet(String key) {
        String value = SystemUtils.getEnvOrProperty(key);
        if (Objects.nonNull(value)) {
            return value;
        }
        String errMsg = "System env or property '" + key + "' is not set";
        log.error(errMsg);
        throw new IllegalStateException(errMsg);
    }

}
