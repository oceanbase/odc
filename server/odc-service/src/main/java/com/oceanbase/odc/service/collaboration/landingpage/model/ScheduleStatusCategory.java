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
package com.oceanbase.odc.service.collaboration.landingpage.model;

import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: ysj
 * @Date: 2025/8/12 16:14
 * @Since: 4.4.1
 * @Description:
 */
@Getter
public enum ScheduleStatusCategory {

    ENABLED(ScheduleStatus.ENABLED),

    OTHER();

    private final ImmutableSet<ScheduleStatus> includesScheduleStatus;

    ScheduleStatusCategory(ScheduleStatus... includesScheduleStatus) {
        this.includesScheduleStatus = ImmutableSet.copyOf(includesScheduleStatus);
    }

    public static ScheduleStatusCategory getCategory(@NonNull ScheduleStatus scheduleStatus) {
        Optional<ScheduleStatusCategory> categoryOptional = Arrays.stream(ScheduleStatusCategory.values())
                .filter(scheduleStatusCategory -> scheduleStatusCategory.includesScheduleStatus
                        .contains(scheduleStatus))
                .findFirst();
        return categoryOptional.orElse(OTHER);
    }
}
