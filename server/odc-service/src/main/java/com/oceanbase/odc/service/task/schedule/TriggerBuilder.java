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
import org.quartz.JobDataMap;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.constants.JobConstants;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
public class TriggerBuilder {

    public static Trigger build(JobDefinition jd) throws JobException {
        JobDataMap triData = new JobDataMap();
        JobContext jc = new DefaultJobContextBuilder().build(jd);
        triData.put(JobConstants.QUARTZ_DATA_MAP_JOB_CONTEXT, jc);

        TriggerConfig triggerConfig = jd.getTriggerConfig();
        if (jd.getTriggerConfig() == null) {
            triggerConfig = new TriggerConfig();
            triggerConfig.setTriggerStrategy(TriggerStrategy.START_NOW);
        }
        return build(QuartzKeyGenerator.generateTriggerKey(jc.getJobIdentity()),
                triggerConfig, jd.getMisfireStrategy(), triData);
    }

    private static Trigger build(TriggerKey key, TriggerConfig config,
            MisfireStrategy misfireStrategy, JobDataMap triggerDataMap) throws JobException {
        switch (config.getTriggerStrategy()) {
            case START_NOW:
                return org.quartz.TriggerBuilder
                        .newTrigger().withIdentity(key).withSchedule(SimpleScheduleBuilder
                                .simpleSchedule().withRepeatCount(0).withMisfireHandlingInstructionFireNow())
                        .startNow().usingJobData(triggerDataMap).build();
            case START_AT:
                return org.quartz.TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)
                                .withMisfireHandlingInstructionFireNow())
                        .startAt(config.getStartAt()).usingJobData(triggerDataMap).build();
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

                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);

                switch (misfireStrategy) {
                    case MISFIRE_INSTRUCTION_IGNORE_MISFIRE:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                        break;
                    case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                        break;
                    default:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                        break;
                }

                return org.quartz.TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(cronScheduleBuilder).usingJobData(triggerDataMap).build();

            }
            default:
                throw new UnsupportedException();
        }
    }
}
