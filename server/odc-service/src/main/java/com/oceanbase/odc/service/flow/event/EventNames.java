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
package com.oceanbase.odc.service.flow.event;

import com.oceanbase.odc.common.event.AbstractEvent;

/**
 * Event name for {@link AbstractEvent}
 *
 * @author yh263208
 * @date 2022-02-16 19:22
 * @since ODC_release_3.3.0
 */
public class EventNames {
    /**
     * Event name for Service Task Created
     */
    public static final String TASK_INSTANCE_CREATED = "ServiceTaskInstanceCreated";
    /**
     * Event name for Service Task Started
     */
    public static final String TASK_STARTED = "ServiceTaskStarted";
    /**
     * Event name for User Task Created
     */
    public static final String USER_TASK_CREATED = "UserTaskCreated";
    /**
     * Event name for ActiveTaskStatistics
     */
    public static final String ACTIVE_TASK_STATISTICS = "ActiveTaskStatistics";

}
