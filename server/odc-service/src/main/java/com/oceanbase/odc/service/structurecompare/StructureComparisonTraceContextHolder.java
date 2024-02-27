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
package com.oceanbase.odc.service.structurecompare;

import org.slf4j.MDC;

/**
 * @author jingtian
 * @date 2024/1/19
 * @since
 */
public final class StructureComparisonTraceContextHolder {
    public static final String TASK_ID = "taskId";
    public static final String TASK_WORK_SPACE = "structureComparisonWorkSpace";

    private StructureComparisonTraceContextHolder() {}

    /**
     * 请求入口处，将任务日志meta信息写入上下文
     */
    public static void trace(long userId, long taskId) {
        MDC.put(TASK_WORK_SPACE, String.valueOf(userId));
        MDC.put(TASK_ID, String.valueOf(taskId));
    }

    /**
     * 清除任务日志meta信息上下文
     */
    public static void clear() {
        MDC.remove(TASK_WORK_SPACE);
        MDC.remove(TASK_ID);
    }
}
