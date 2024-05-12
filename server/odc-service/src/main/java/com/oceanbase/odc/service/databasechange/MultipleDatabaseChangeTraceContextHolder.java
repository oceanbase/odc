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
package com.oceanbase.odc.service.databasechange;

import org.slf4j.MDC;

/**
 * @author: zijia.cj
 * @date: 2024/5/6
 */
public final class MultipleDatabaseChangeTraceContextHolder {
    public static final String TASK_ID = "taskId";

    private MultipleDatabaseChangeTraceContextHolder() {}

    public static void trace(long userId, long taskId) {
        MDC.put(TASK_ID, String.valueOf(taskId));
    }

    public static void clear() {
        MDC.remove(TASK_ID);
    }
}
