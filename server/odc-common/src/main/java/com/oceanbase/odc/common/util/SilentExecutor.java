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

import com.oceanbase.odc.common.function.Procedure;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-03-25
 * @since 4.2.4
 */
@Slf4j
public class SilentExecutor {

    public static void executeSafely(Procedure operation) {
        try {
            operation.invoke();
        } catch (Exception e) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[2];
            log.warn("Execute {}#{} failed.", element.getClassName(), element.getMethodName(), e);
        }
    }
}
