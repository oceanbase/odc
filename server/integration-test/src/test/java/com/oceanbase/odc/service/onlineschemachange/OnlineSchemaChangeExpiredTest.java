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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.TriggerListener;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.test.context.TestPropertySource;

import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-12
 * @since 4.2.0
 */
@Slf4j
@TestPropertySource(properties = "osc-task-expired-after-seconds=1")
public class OnlineSchemaChangeExpiredTest extends OBMySqlOscTestEnv {

    @Test
    public void test_osc_task_expired_after_seconds() {

        createTableForMultiTask();
        try {
            OnlineSchemaChangeParameters changeParameters = getOnlineSchemaChangeParameters();
            ScheduleEntity schedule = getScheduleEntity(config, changeParameters);
            List<OnlineSchemaChangeScheduleTaskParameters> subTaskParameters =
                    changeParameters.generateSubTaskParameters(config, config.getDefaultSchema());
            List<ScheduleTaskEntity> taskEntities = new ArrayList<>();
            subTaskParameters.forEach(
                    taskParameter -> taskEntities.add(getScheduleTaskEntity(schedule.getId(), taskParameter)));
            onlineSchemaChangeTaskHandler.start(schedule.getId(), taskEntities.get(0).getId());

            TriggerListener triggerListener = new TriggerListenerFactory().generateTriggerListener(
                    schedule.getId(), c -> {
                        try {
                            log.info("schedule id {} to sleep {} milliseconds", schedule.getId(), 1000);
                            TimeUnit.MILLISECONDS.sleep(1000);

                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
            scheduler.getListenerManager().addTriggerListener(triggerListener);

            AtomicBoolean scheduleDeleted = new AtomicBoolean(false);
            SchedulerListener listener = new SchedulerListenerSupport() {
                @Override
                public void jobDeleted(JobKey jobKey) {
                    scheduleDeleted.getAndSet(true);
                }
            };

            scheduler.getListenerManager().addSchedulerListener(listener);
            Await.await().timeout(30).until(scheduleDeleted::get).build().start();
            Assert.assertEquals(2, taskEntities.size());
            Assert.assertEquals(TaskStatus.CANCELED,
                    scheduleTaskRepository.findById(taskEntities.get(0).getId()).get().getStatus());
            Assert.assertEquals(TaskStatus.PREPARING,
                    scheduleTaskRepository.findById(taskEntities.get(1).getId()).get().getStatus());

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        } finally {
            dropTableForMultiTask();
        }
    }

}
