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

import com.oceanbase.odc.service.flow.model.FlowNodeStatus;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@FunctionalInterface
public interface FlowTaskCallBack {

    /**
     * approval next task in this flow instance id, decide process continue or not
     * 
     * @param flowInstanceId flow instance id
     * @param flowTaskInstanceId reference flow task instance node#id
     * @param flowNodeStatus flow node status
     */
    void callback(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus);
}
