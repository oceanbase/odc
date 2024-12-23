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
package com.oceanbase.odc.service.schedule.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author：tinker
 * @Date: 2022/11/16 15:36
 * @Descripition:
 */
public enum ScheduleStatus {

    CREATING,
    APPROVING,

    APPROVAL_EXPIRED,

    REJECTED,
    PAUSE,
    ENABLED,
    TERMINATION,
    TERMINATED,

    COMPLETED,
    EXECUTION_FAILED,

    DELETED;

    public static List<ScheduleStatus> listUnfinishedStatus() {
        return Collections.unmodifiableList(Arrays.asList(CREATING, APPROVING, ENABLED));
    }
}
