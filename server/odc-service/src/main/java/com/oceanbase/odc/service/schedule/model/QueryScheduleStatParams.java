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

import java.util.Date;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: ysj
 * @Date: 2025/2/25 10:06
 * @Since: 4.3.4
 * @Description:
 */
@Data
@Builder
public class QueryScheduleStatParams {
    private Set<Long> scheduleIds;
    private Set<ScheduleType> scheduleTypes;
    private Date startTime;
    private Date endTime;
    private Set<ScheduleStatus> statuses;
}
