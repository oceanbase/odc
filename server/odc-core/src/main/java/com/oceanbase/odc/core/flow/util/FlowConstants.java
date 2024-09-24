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
package com.oceanbase.odc.core.flow.util;

/**
 * Some constants for {@code Flowable}
 *
 * @author yh263208
 * @date 2022-01-18 21:00
 * @since ODC_release_3.3.0
 */
public class FlowConstants {
    /**
     * Event name for task created
     */
    public static final String TASK_CREATE_EVENT_NAME = "create";
    /**
     * Event name for task deleted
     */
    public static final String TASK_DELETE_EVENT_NAME = "delete";
    /**
     * Event name for task completed
     */
    public static final String TASK_COMPLETE_EVENT_NAME = "complete";
    /**
     * Event name for task assign
     */
    public static final String TASK_ASSIGN_EVENT_NAME = "assignment";
    /**
     * Event name for execution start
     */
    public static final String EXECUTION_START_EVENT_NAME = "start";

    /**
     * Event name for execution end
     */
    public static final String EXECUTION_END_EVENT_NAME = "end";
    /**
     * Event name for execution take
     */
    public static final String EXECUTION_TAKE_EVENT_NAME = "take";

}
