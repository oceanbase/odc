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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscTestUtil;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsCleanResourcesAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

/**
 * @author longpeng.zlp
 * @date 2024/7/30 13:50
 * @since 4.3.1
 */
public class CleanResourcesActionBaseTest {
    private OmsProjectOpenApiService omsProjectOpenApiService;


    @Before
    public void init() {
        omsProjectOpenApiService = Mockito.mock(OmsProjectOpenApiService.class);
        Mockito.when(omsProjectOpenApiService.createProject(ArgumentMatchers.any())).thenReturn("testProjectID");
    }

    @Test
    public void testDeterminateNextState1() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RELEASED);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        ScheduleEntity schedule = OscTestUtil.createScheduleEntity();
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.DONE);
        OscActionResult actionResult = omsCleanResourcesAction.determinateNextState(scheduleTask, schedule);
        // switch to yield if done context
        Assert.assertEquals(actionResult.getNextState(), OscStates.YIELD_CONTEXT.getState());
    }

    @Test
    public void testDeterminateNextState2() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RELEASED);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        ScheduleEntity schedule = OscTestUtil.createScheduleEntity();
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.CANCELED);
        OscActionResult actionResult = omsCleanResourcesAction.determinateNextState(scheduleTask, schedule);
        // switch to complete if canceled
        Assert.assertEquals(actionResult.getNextState(), OscStates.COMPLETE.getState());
    }

    @Test
    public void testDeterminateNextState3() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RELEASED);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        ScheduleEntity schedule = OscTestUtil.createScheduleEntity();
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = new OnlineSchemaChangeParameters();
        onlineSchemaChangeParameters.setErrorStrategy(TaskErrorStrategy.CONTINUE);
        schedule.setJobParametersJson(JsonUtils.toJson(onlineSchemaChangeParameters));
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.FAILED);
        OscActionResult actionResult = omsCleanResourcesAction.determinateNextState(scheduleTask, schedule);
        // switch to yield context if ignore error
        Assert.assertEquals(actionResult.getNextState(), OscStates.YIELD_CONTEXT.getState());
    }

    @Test
    public void testDeterminateNextState4() {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RELEASED);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        ScheduleEntity schedule = OscTestUtil.createScheduleEntity();
        OnlineSchemaChangeParameters onlineSchemaChangeParameters = new OnlineSchemaChangeParameters();
        onlineSchemaChangeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
        schedule.setJobParametersJson(JsonUtils.toJson(onlineSchemaChangeParameters));
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.FAILED);
        OscActionResult actionResult = omsCleanResourcesAction.determinateNextState(scheduleTask, schedule);
        // switch to complete if not ignore error
        Assert.assertEquals(actionResult.getNextState(), OscStates.COMPLETE.getState());
    }

    @Test
    public void testOmsCleanResourceAction() throws Exception {
        OmsProjectProgressResponse ret = new OmsProjectProgressResponse();
        ret.setStatus(OmsProjectStatusEnum.RUNNING);
        Mockito.when(omsProjectOpenApiService.describeProjectProgress(ArgumentMatchers.any())).thenReturn(ret);
        OmsCleanResourcesAction omsCleanResourcesAction = new OmsCleanResourcesAction(omsProjectOpenApiService);
        OscActionContext oscActionContext = OscTestUtil.createOcsActionContext(DialectType.OB_MYSQL,
                OscStates.CLEAN_RESOURCE.getState(), TaskStatus.RUNNING);
        oscActionContext.getTaskParameter().setOmsProjectId("omsProjectID");
        oscActionContext.getTaskParameter().setUid("omsUid");
        OscActionResult oscActionResult = omsCleanResourcesAction.execute(oscActionContext);
        Assert.assertEquals(oscActionResult.getNextState(), OscStates.CLEAN_RESOURCE.getState());
    }
}
