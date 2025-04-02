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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.task.model.OnlineSchemaChangeTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.task.TaskService;

/**
 * @author longpeng.zlp
 * @date 2024/8/23 10:39
 */
public class OnlineSchemaChangeFlowableTaskTest {
    private static String resultJson = "{"
            + "\"fullLogDownloadUrl\":null,"
            + "\"tasks\":["
            + "  {"
            + "    \"createTime\":1724291469000,"
            + "    \"executor\":null,\"fireTime\":1724291470000,"
            + "    \"id\":65,\"jobGroup\":\"ONLINE_SCHEMA_CHANGE_COMPLETE\","
            + "    \"jobId\":null,\"jobName\":\"64\","
            + "    \"parameters\":"
            + "      {"
            + "        \"continueOnError\":false,"
            + "        \"delimiter\":\";\","
            + "        \"errorStrategy\":null,"
            + "        \"flowInstanceId\":null,"
            + "        \"flowTaskID\":null,"
            + "        \"lockTableTimeOutSeconds\":null,"
            + "        \"lockUsers\":null,"
            + "        \"originTableCleanStrategy\":null,"
            + "        \"rateLimitConfig\":{\"dataSizeLimit\":null,\"rowLimit\":null},"
            + "        \"sqlContent\":null,"
            + "        \"sqlType\":null,"
            + "        \"swapTableNameRetryTimes\":null,"
            + "        \"swapTableType\":null},"
            + "        \"progressPercentage\":90.00,"
            + "        \"resultJson\":\"{\\\"checkFailedTime\\\":{},\\\"currentStep\\\":\\\"TRANSFER_APP_SWITCH\\\",\\\"currentStepStatus\\\":\\\"RUNNING\\\",\\\"dialectType\\\":\\\"OB_MYSQL\\\",\\\"fullTransferEstimatedCount\\\":13,\\\"fullTransferFinishedCount\\\":13,\\\"fullTransferProgressPercentage\\\":100.00,\\\"fullVerificationProgressPercentage\\\":0.00,\\\"fullVerificationResult\\\":\\\"UNCHECK\\\",\\\"fullVerificationResultDescription\\\":\\\"未校验\\\",\\\"manualSwapTableEnabled\\\":true,\\\"manualSwapTableStarted\\\":false,\\\"newTableDdl\\\":null,\\\"oldTableName\\\":\\\"_ghost1_osc_old_\\\",\\\"originTableDdl\\\":\\\"CREATE TABLE `ghost1` (\\\\n  `id` int(11) NOT NULL AUTO_INCREMENT,\\\\n  `age` int(11) DEFAULT NULL,\\\\n  PRIMARY KEY (`id`)\\\\n) AUTO_INCREMENT = 15000001 DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMPRESSION = 'zstd_1.3.8' REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 0\\\",\\\"originTableName\\\":\\\"ghost1\\\",\\\"precheckResult\\\":\\\"FINISHED\\\",\\\"precheckResultDescription\\\":null"
            + "      }\","
            + "      \"status\":\"RUNNING\","
            + "      \"updateTime\":1724291750000"
            + "    }"
            + " ]"
            + "}";

    // test deserialize
    @Test
    public void testOlineSchemaChangeTaskResultDeserialize() {
        String json = resultJson;
        OnlineSchemaChangeTaskResult onlineSchemaChangeTaskResult =
                JsonUtils.fromJson(json, OnlineSchemaChangeTaskResult.class);
        Assert.assertNotNull(onlineSchemaChangeTaskResult);
    }

    // test flowable task update progress
    @Test
    public void testOnlineSchemaChangeFlowableTaskUpdateProgress() {
        MockOSCFlowTask mockOSCFlowTask = new MockOSCFlowTask();
        TaskService taskService = Mockito.mock(TaskService.class);
        // prepare mock
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(TaskStatus.RUNNING);
        taskEntity.setResultJson(resultJson);
        Mockito.when(taskService.detail(1L)).thenReturn(taskEntity);
        ArgumentCaptor<TaskEntity> argumentCaptor = ArgumentCaptor.forClass(TaskEntity.class);
        // do method call expect task update
        mockOSCFlowTask.onProgressUpdate(1L, taskService);
        Mockito.verify(taskService).update(argumentCaptor.capture());
        TaskEntity task = argumentCaptor.getValue();
        OnlineSchemaChangeTaskResult onlineSchemaChangeTaskResult =
                JsonUtils.fromJson(task.getResultJson(), OnlineSchemaChangeTaskResult.class);
        // check update result json
        Assert.assertEquals(onlineSchemaChangeTaskResult.getTasks().size(), 1);
        Assert.assertEquals(onlineSchemaChangeTaskResult.getTasks().get(0).getId().longValue(), 1024);
    }

    @Test
    public void testScheduleTasksUpdateHint() {
        MockOSCFlowTask mockOSCFlowTask = new MockOSCFlowTask();
        List<ScheduleTaskEntity> scheduleTaskEntities = Arrays.asList(
                createScheduleTaskEntity(1, true, OscStepName.FULL_TRANSFER.name()),
                createScheduleTaskEntity(2, false, OscStepName.INCR_TRANSFER.name()),
                createScheduleTaskEntity(3, false, null));
        OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint scheduleTasksUpdateHint =
                mockOSCFlowTask.getScheduleTasksUpdateHint(scheduleTaskEntities);
        Assert.assertEquals(scheduleTasksUpdateHint.getEnableManualSwapTableFlagCounts(), 1);
        Assert.assertEquals(scheduleTasksUpdateHint.getTaskStepsMap().size(), 2);
        Assert.assertEquals(scheduleTasksUpdateHint.getTaskStepsMap().get(1L), OscStepName.FULL_TRANSFER.name());
        Assert.assertEquals(scheduleTasksUpdateHint.getTaskStepsMap().get(2L), OscStepName.INCR_TRANSFER.name());
        Assert.assertEquals(scheduleTasksUpdateHint.getTaskStatusMap().get(2L), TaskStatus.RUNNING.name());
        Map<Long, String> taskMap = new HashMap() {
            {
                put(1L, TaskStatus.RUNNING.name());
                put(2L, TaskStatus.RUNNING.name());
                put(3L, TaskStatus.RUNNING.name());
            }
        };
        Assert.assertFalse(scheduleTasksUpdateHint.hasDiff(scheduleTasksUpdateHint));

        Assert.assertTrue(
                scheduleTasksUpdateHint.hasDiff(new OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint(0)));
        Assert.assertTrue(scheduleTasksUpdateHint.hasDiff(new OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint(1,

                Collections.singletonMap(1L, OscStepName.FULL_TRANSFER.name()), taskMap)));
        Map<Long, String> muted = new HashMap<>(scheduleTasksUpdateHint.getTaskStepsMap());
        muted.put(2L, OscStepName.APP_SWITCH.name());
        Assert.assertTrue(
                scheduleTasksUpdateHint
                        .hasDiff(new OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint(1, muted, taskMap)));
        Assert.assertFalse(
                scheduleTasksUpdateHint.hasDiff(new OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint(
                        scheduleTasksUpdateHint.getEnableManualSwapTableFlagCounts(),
                        scheduleTasksUpdateHint.getTaskStepsMap(), taskMap)));
        Assert.assertTrue(
                scheduleTasksUpdateHint.hasDiff(new OnlineSchemaChangeFlowableTask.ScheduleTasksUpdateHint(
                        scheduleTasksUpdateHint.getEnableManualSwapTableFlagCounts(),
                        scheduleTasksUpdateHint.getTaskStepsMap(), new HashMap() {
                            {
                                put(1L, TaskStatus.RUNNING.name());
                                put(2L, TaskStatus.RUNNING.name());
                                put(3L, TaskStatus.FAILED.name());
                            }
                        })));

    }

    private ScheduleTaskEntity createScheduleTaskEntity(long id, boolean manualSwapTableEnabled, String stepName) {
        ScheduleTaskEntity scheduleTask = new ScheduleTaskEntity();
        scheduleTask.setId(id);
        OnlineSchemaChangeScheduleTaskResult scheduleTaskResult1 = new OnlineSchemaChangeScheduleTaskResult();
        scheduleTaskResult1.setManualSwapTableEnabled(manualSwapTableEnabled);
        scheduleTaskResult1.setCurrentStep(stepName);
        if (null != stepName) {
            scheduleTask.setResultJson(JsonUtils.toJson(scheduleTaskResult1));
        }
        scheduleTask.setStatus(TaskStatus.RUNNING);
        return scheduleTask;
    }

    private static final class MockOSCFlowTask extends OnlineSchemaChangeFlowableTask {
        private Page<ScheduleTaskEntity> createSchedulerTaskEntity() {
            Page<ScheduleTaskEntity> mockPage = Mockito.mock(Page.class);
            ScheduleTaskEntity scheduleTask = new ScheduleTaskEntity();
            scheduleTask.setId(1L);
            scheduleTask.setStatus(TaskStatus.RUNNING);
            ScheduleTaskEntity scheduleTask2 = new ScheduleTaskEntity();
            scheduleTask2.setStatus(TaskStatus.PREPARING);
            scheduleTask2.setId(1024L);
            List<ScheduleTaskEntity> scheduleTaskEntities = Arrays.asList(scheduleTask);
            Mockito.when(mockPage.getSize()).thenReturn(scheduleTaskEntities.size());
            Mockito.when(mockPage.iterator()).thenReturn(scheduleTaskEntities.iterator());
            Mockito.when(mockPage.getContent()).thenReturn(Arrays.asList(scheduleTask2));
            return mockPage;
        }

        protected Page<ScheduleTaskEntity> list(Pageable pageable, Long scheduleId) {
            return createSchedulerTaskEntity();
        }
    }
}
