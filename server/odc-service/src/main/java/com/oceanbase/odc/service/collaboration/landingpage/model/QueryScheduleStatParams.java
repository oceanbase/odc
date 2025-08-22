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

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @Author: ysj
 * @Date: 2025/2/25 10:06
 * @Since: 4.3.4
 * @Description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class QueryScheduleStatParams extends QueryStatParams {

    private Set<ScheduleType> scheduleTypes;
    private Set<ScheduleTaskType> scheduleTaskTypes;
    private Set<ScheduleStatus> statuses;

    /**
     * inner query param
     */
    private Boolean isInnerSchedule;
    private Set<Long> scheduleIds;

    public void configLandingPageInitialParam() {
        this.setStatuses(Sets.immutableEnumSet(ScheduleStatus.ENABLED));

        // front-end would not show task if is_inner is true
        this.setIsInnerSchedule(false);
    }

    public Set<ScheduleType> getScheduleTypes() {
        return CollUtil.intersectionDistinct(scheduleTypes, ScheduleType.supportedLandingPage());
    }

    public Set<ScheduleTaskType> getScheduleTaskTypes() {
        Set<ScheduleTaskType> valid = getScheduleTypes().stream()
                .flatMap(scheduleType -> ScheduleTaskType.from(scheduleType).stream())
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(scheduleTaskTypes)) {
            return valid;
        }
        return CollUtil.intersectionDistinct(scheduleTaskTypes, valid);
    }
}
