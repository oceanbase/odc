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
package com.oceanbase.odc.service.onlineschemachange.oscfms;

import java.util.Collections;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

/**
 * @author longpeng.zlp
 * @date 2024/9/23 11:56
 */
public class OscActionFsmBaseTest {
    private long expireTimeInSeconds = 3600;
    private OscActionFsm oscActionFsm = new OscActionFsm();
    private OscActionContext context = new OscActionContext();
    private ScheduleTaskEntity scheduleTaskEntity = new ScheduleTaskEntity();
    private ScheduleEntity scheduleEntity = new ScheduleEntity();
    private String state = OscStates.MONITOR_DATA_TASK.getState();
    private OnlineSchemaChangeScheduleTaskParameters parameters = new OnlineSchemaChangeScheduleTaskParameters();
    private OnlineSchemaChangeParameters onlineSchemaChangeParameters = new OnlineSchemaChangeParameters();
    private ScheduleTaskRepository scheduleTaskRepository;
    private FlowInstanceService flowInstanceService;

    @Before
    public void init() {
        context.setSchedule(scheduleEntity);
        context.setScheduleTask(scheduleTaskEntity);
        context.setParameter(onlineSchemaChangeParameters);
        scheduleEntity.setId(1L);
        scheduleTaskEntity.setId(1024L);
        scheduleTaskEntity.setStatus(TaskStatus.RUNNING);
        parameters.setState(OscStates.MONITOR_DATA_TASK.getState());
        onlineSchemaChangeParameters.setFlowInstanceId(1L);
        scheduleTaskEntity.setParametersJson(JsonUtils.toJson(parameters));
        scheduleTaskRepository = Mockito.mock(ScheduleTaskRepository.class);
        flowInstanceService = Mockito.mock(FlowInstanceService.class);
        oscActionFsm.scheduleTaskRepository = scheduleTaskRepository;
        oscActionFsm.flowInstanceService = flowInstanceService;
        oscActionFsm.oscTaskExpiredAfterSeconds = expireTimeInSeconds;
    }

    @Test
    public void testTaskExpired() {
        // not expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis()));
        Assert.assertFalse(oscActionFsm.isTaskExpired(context));
        // expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis() - expireTimeInSeconds * 1000 * 10));
        Assert.assertTrue(oscActionFsm.isTaskExpired(context));
    }

    @Test
    public void testTryHandleInvalidTaskWithTaskExpired() {
        // expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis() - expireTimeInSeconds * 1000 * 10));
        boolean handleResult = oscActionFsm.tryHandleInvalidTask(state, context);
        ArgumentCaptor<ScheduleTaskEntity> argumentCaptor = ArgumentCaptor.forClass(ScheduleTaskEntity.class);
        Assert.assertTrue(handleResult);
        Mockito.verify(scheduleTaskRepository).update(argumentCaptor.capture());
        ScheduleTaskEntity toVerify = argumentCaptor.getValue();
        Assert.assertEquals(toVerify.getStatus(), TaskStatus.FAILED);
        checkState(toVerify, OscStates.CLEAN_RESOURCE.getState());
    }

    @Test
    public void testTryHandleInvalidTaskWithTaskCanceled() {
        // not expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis()));
        scheduleTaskEntity.setStatus(TaskStatus.CANCELED);
        boolean handleResult = oscActionFsm.tryHandleInvalidTask(state, context);
        ArgumentCaptor<ScheduleTaskEntity> argumentCaptor = ArgumentCaptor.forClass(ScheduleTaskEntity.class);
        Assert.assertTrue(handleResult);
        Mockito.verify(scheduleTaskRepository).update(argumentCaptor.capture());
        ScheduleTaskEntity toVerify = argumentCaptor.getValue();
        Assert.assertEquals(toVerify.getStatus(), TaskStatus.CANCELED);
        checkState(toVerify, OscStates.CLEAN_RESOURCE.getState());
    }

    @Test
    public void testTryHandleInvalidTaskWithFlowCanceled() {
        // not expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis()));
        scheduleTaskEntity.setStatus(TaskStatus.RUNNING);
        Mockito.when(flowInstanceService.getStatus(ArgumentMatchers.any()))
                .thenReturn(Collections.singletonMap(1L, FlowStatus.CANCELLED));
        boolean handleResult = oscActionFsm.tryHandleInvalidTask(state, context);
        ArgumentCaptor<ScheduleTaskEntity> argumentCaptor = ArgumentCaptor.forClass(ScheduleTaskEntity.class);
        Assert.assertTrue(handleResult);
        Mockito.verify(scheduleTaskRepository).update(argumentCaptor.capture());
        ScheduleTaskEntity toVerify = argumentCaptor.getValue();
        Assert.assertEquals(toVerify.getStatus(), TaskStatus.CANCELED);
        checkState(toVerify, OscStates.CLEAN_RESOURCE.getState());
    }

    @Test
    public void testTryHandleInvalidTaskWithTaskAbnormal() {
        // not expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis()));
        scheduleTaskEntity.setStatus(TaskStatus.ABNORMAL);
        Mockito.when(flowInstanceService.getStatus(ArgumentMatchers.any()))
                .thenReturn(Collections.singletonMap(1L, FlowStatus.EXECUTION_ABNORMAL));
        boolean handleResult = oscActionFsm.tryHandleInvalidTask(state, context);
        Assert.assertTrue(handleResult);
        Mockito.verify(scheduleTaskRepository, Mockito.never()).update(ArgumentMatchers.any());
    }

    @Test
    public void testTryHandleInvalidTaskNormal() {
        // not expired
        scheduleTaskEntity.setCreateTime(new Date(System.currentTimeMillis()));
        scheduleTaskEntity.setStatus(TaskStatus.RUNNING);
        Mockito.when(flowInstanceService.getStatus(ArgumentMatchers.any()))
                .thenReturn(Collections.singletonMap(1L, FlowStatus.EXECUTING));
        boolean handleResult = oscActionFsm.tryHandleInvalidTask(state, context);
        Assert.assertFalse(handleResult);
        Mockito.verify(scheduleTaskRepository, Mockito.never()).update(ArgumentMatchers.any());
    }

    private void checkState(ScheduleTaskEntity scheduleTask, String expectState) {
        OnlineSchemaChangeScheduleTaskParameters parameters =
                JsonUtils.fromJson(scheduleTask.getParametersJson(), OnlineSchemaChangeScheduleTaskParameters.class);
        Assert.assertEquals(expectState, parameters.getState());
    }
}
