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
package com.oceanbase.odc.service.task.supervisor;

import lombok.Data;

/**
 * @author longpeng.zlp
 * @date 2024/12/9 11:30
 */
@Data
public class TaskCallerResult {
    public static final TaskCallerResult SUCCESS_RESULT = new TaskCallerResult(true, null);
    private final Boolean succeed;
    private final Exception e;

    public static TaskCallerResult failed(Exception e) {
        return new TaskCallerResult(false, e);
    }
}
