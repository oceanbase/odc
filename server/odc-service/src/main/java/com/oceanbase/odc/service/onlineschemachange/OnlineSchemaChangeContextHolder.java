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
package com.oceanbase.odc.service.onlineschemachange;

import java.util.Map;

import org.slf4j.MDC;

import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

public class OnlineSchemaChangeContextHolder {

    public static final String TASK_ID = "taskId";
    public static final String TASK_WORK_SPACE = "onlineSchemaChangeWorkSpace";

    private OnlineSchemaChangeContextHolder() {}

    /**
     * 请求入口处，将任务日志meta信息写入上下文
     */
    public static void trace(Long userId, Long taskId, Long organizationId) {

        MDC.put(TASK_WORK_SPACE, userId + "");
        MDC.put(TASK_ID, taskId + "");
        MDC.put(OdcConstants.ORGANIZATION_ID, organizationId + "");

        // set current user to get tenant list
        SecurityContextUtils.setCurrentUser(userId, organizationId, null);
    }

    public static void trace(String userId, String taskId, String organizationId) {
        Long userIdL = (userId == null ? null : Long.parseLong(userId));
        Long taskIdL = (taskId == null ? null : Long.parseLong(taskId));
        Long organizationIdL = (organizationId == null ? null : Long.parseLong(organizationId));
        trace(userIdL, taskIdL, organizationIdL);
    }

    public static void retrace(Map<String, String> context) {
        MDC.setContextMap(context);
    }

    /**
     * 清除任务日志meta信息上下文
     */
    public static void clear() {
        MDC.clear();
    }

    public static String get(String key) {
        return MDC.get(key);
    }

}
