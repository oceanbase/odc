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
package com.oceanbase.odc.service.flow.task;

import java.util.Map;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.flow.FlowInstanceService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
public class FlowableTaskCallBackApprovalUtils {


    public static void approval(long flowInstanceId, long taskId, TaskStatus taskStatus) {
        approval(flowInstanceId, taskId, taskStatus, null);
    }

    public static void approval(long flowInstanceId, long taskId, TaskStatus taskStatus,
            Map<String, Object> approvalVariables) {

        if (!taskStatus.isTerminated()) {
            log.warn("Task is not terminated, callback failed, taskId={}.", taskId);
            return;
        }
        try {
            if (taskStatus == TaskStatus.DONE) {
                SpringContextUtil.getBean(FlowInstanceService.class)
                        .approve(flowInstanceId, "task execute succeed.", true, approvalVariables);
            } else {
                SpringContextUtil.getBean(FlowInstanceService.class)
                        .reject(flowInstanceId, "task execute failed.", true);
            }
        } catch (Exception e) {
            log.warn("Failed to reject flow instance, flowInstanceId={}", flowInstanceId);
        }
    }
}
