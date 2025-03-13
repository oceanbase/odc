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
package com.oceanbase.odc.service.task.util;

import com.oceanbase.odc.service.task.executor.TaskResult;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/12/16 15:58
 */
@Data
@AllArgsConstructor
public class TaskResultWrap {
    TaskResult taskResult;
    boolean accessSuccess;
    Throwable e;

    // access success, receive response
    public static TaskResultWrap successTaskResult(TaskResult taskResult) {
        return new TaskResultWrap(taskResult, true, null);
    }

    // access success, receive response, but have other situation
    public static TaskResultWrap failedTaskResult(Throwable e) {
        return new TaskResultWrap(null, true, e);
    }

    // access failed, none response received
    public static TaskResultWrap unreachedTaskResult(Throwable e) {
        return new TaskResultWrap(null, false, e);
    }
}
