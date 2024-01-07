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

import java.text.ParseException;

import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.task.caller.JobException;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
public class TriggerBuilder {

    public static Trigger build(TriggerKey key, TriggerConfig config) throws JobException {
        switch (config.getTriggerStrategy()) {
            case START_NOW:
                return org.quartz.TriggerBuilder
                        .newTrigger().withIdentity(key).withSchedule(SimpleScheduleBuilder
                                .simpleSchedule().withRepeatCount(0).withMisfireHandlingInstructionFireNow())
                        .startNow().build();
            case START_AT:
                return org.quartz.TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)
                                .withMisfireHandlingInstructionFireNow())
                        .startAt(config.getStartAt()).build();
            case CRON:
            case DAY:
            case MONTH:
            case WEEK: {

                String cron;

                try {
                    cron = QuartzCronExpressionUtils
                            .adaptCronExpression(config.getCronExpression());
                } catch (ParseException e) {
                    throw new JobException("Invalid cron expression,create quartz job failed.", e);
                }

                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing();

                return org.quartz.TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(cronScheduleBuilder).build();

            }
            default:
                throw new UnsupportedException();
        }
    }
}
