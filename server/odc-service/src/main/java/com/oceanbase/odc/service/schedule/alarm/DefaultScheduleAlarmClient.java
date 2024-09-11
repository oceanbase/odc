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
package com.oceanbase.odc.service.schedule.alarm;

import java.util.Date;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/7/6 20:21
 * @Descripition:
 */

@Slf4j
@Component
@Profile("alipay")
public class DefaultScheduleAlarmClient implements ScheduleAlarmClient {

    @Override
    public void misfire(Long scheduleId, Date fireTime) {
        log.warn("Schedule is misfire,id={},fireTime={}", scheduleId, fireTime);
    }

    @Override
    public void fail(Long scheduleTaskId) {
        log.warn("Schedule task execution failed,scheduleTaskId={}", scheduleTaskId);
    }

    @Override
    public void timeout(Long scheduleTaskId) {
        log.warn("Schedule task execution timeout,scheduleTaskId={}", scheduleTaskId);
    }
}
