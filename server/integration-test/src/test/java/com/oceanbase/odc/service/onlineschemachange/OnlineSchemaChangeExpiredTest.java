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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

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
@TestPropertySource(properties = "osc-task-expired-after-seconds=-100")
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
            taskEntities.get(0).setStatus(TaskStatus.RUNNING);
            scheduleTaskRepository.save(taskEntities.get(0));
            onlineSchemaChangeTaskHandler.complete(schedule.getId(), taskEntities.get(0).getId());

            Assert.assertEquals(2, taskEntities.size());
            Assert.assertEquals(TaskStatus.CANCELED,
                    scheduleTaskRepository.findById(taskEntities.get(0).getId()).get().getStatus());
            Assert.assertEquals(TaskStatus.PREPARING,
                    scheduleTaskRepository.findById(taskEntities.get(1).getId()).get().getStatus());

        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            dropTableForMultiTask();
        }
    }

}
