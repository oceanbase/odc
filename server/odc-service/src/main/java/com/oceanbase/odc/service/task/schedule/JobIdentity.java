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

package com.oceanbase.odc.service.task.schedule;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
@Data
public class JobIdentity {

    private Long id;
    private ScheduleSourceType sourceType;
    private String taskType;

    public static JobIdentity of(Long id, ScheduleSourceType sourceType, String taskType) {
        JobIdentity identity = new JobIdentity();
        identity.setId(id);
        identity.setSourceType(sourceType);
        identity.setTaskType(taskType);
        return identity;
    }

}
