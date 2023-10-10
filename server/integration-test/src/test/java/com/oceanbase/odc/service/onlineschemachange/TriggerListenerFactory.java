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

package com.oceanbase.odc.service.onlineschemachange;

import static com.oceanbase.odc.service.schedule.model.JobType.ONLINE_SCHEMA_CHANGE_COMPLETE;

import java.util.function.Consumer;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.listeners.TriggerListenerSupport;

import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;

/**
 * @author yaobin
 * @date 2023-08-23
 * @since 4.2.0
 */
public class TriggerListenerFactory {

    public TriggerListener generateTriggerListener(Long scheduleId, Consumer<JobExecutionContext> contextConsumer) {

        return new TriggerListenerSupport() {
            @Override
            public String getName() {
                return QuartzKeyGenerator.generateTriggerKey(scheduleId, ONLINE_SCHEMA_CHANGE_COMPLETE).toString();
            }

            @Override
            public void triggerFired(Trigger trigger, JobExecutionContext context) {
                contextConsumer.accept(context);
            }
        };
    }
}
