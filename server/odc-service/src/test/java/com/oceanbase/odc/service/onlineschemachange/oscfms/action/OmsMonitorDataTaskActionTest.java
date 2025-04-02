/*
 * Copyright (c) 2025 OceanBase.
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
import org.mockito.Mockito;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.exception.OscException;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.model.RateLimiterConfig;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscTestUtil;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsMonitorDataTaskAction;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

/**
 * @author longpeng.zlp
 * @date 2024/7/30 14:31
 * @since 4.3.1
 */
public class OmsMonitorDataTaskActionTest {
    private OmsProjectOpenApiService projectOpenApiService;
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    private OscActionContext context;

    @Before
    public void init() {
        projectOpenApiService = Mockito.mock(OmsProjectOpenApiService.class);
        onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        context = new OscActionContext();
    }

    @Test
    public void testHandleOmsProjectStepResultAutoReady() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        result.setFullTransferProgressPercentage(100.0);
        result.setFullVerificationResult(FullVerificationResult.UNCHECK);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.DONE), result,
                SwapTableType.AUTO, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.SWAP_TABLE.getState());
    }

    @Test
    public void testHandleOmsProjectStepResultManualReady() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        result.setFullTransferProgressPercentage(100.0);
        result.setFullVerificationResult(FullVerificationResult.UNCHECK);
        result.setManualSwapTableStarted(false);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.DONE), result,
                SwapTableType.MANUAL, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
        Assert.assertTrue(result.isManualSwapTableEnabled());
    }

    @Test
    public void testHandleOmsProjectStepResultManualReady2() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        result.setFullTransferProgressPercentage(100.0);
        result.setFullVerificationResult(FullVerificationResult.UNCHECK);
        result.setManualSwapTableStarted(true);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.DONE), result,
                SwapTableType.MANUAL, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.SWAP_TABLE.getState());
    }

    @Test
    public void testHandleOmsProjectStepResultNotReady() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        result.setFullTransferProgressPercentage(90.0);
        result.setFullVerificationResult(FullVerificationResult.UNCHECK);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.DONE), result,
                SwapTableType.AUTO, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
    }

    @Test
    public void testHandleOmsProjectStepResultNotReady1() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.RUNNING), result,
                SwapTableType.AUTO, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
    }

    @Test(expected = OscException.class)
    public void testHandleOmsProjectStepResultNotReady2() {
        ScheduleTaskEntity scheduleTask = OscTestUtil.createScheduleTaskEntity(TaskStatus.RUNNING);
        OnlineSchemaChangeScheduleTaskResult result = OscTestUtil.createTaskResult(DialectType.OB_MYSQL);
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        OscActionResult actionResult = omsMonitorDataTaskAction.handleProjectStepResult(context,
                createProjectStepResult(TaskStatus.FAILED), result,
                SwapTableType.AUTO, scheduleTask);
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
    }

    @Test
    public void testOmsMonitorDataTaskSwitchToModifyDataState() throws Exception {
        OmsMonitorDataTaskAction omsMonitorDataTaskAction =
                new OmsMonitorDataTaskAction(projectOpenApiService, onlineSchemaChangeProperties);
        context = OscTestUtil.createOcsActionContext(DialectType.OB_MYSQL, OscStates.MONITOR_DATA_TASK.getState(),
                TaskStatus.RUNNING);
        RateLimiterConfig r1 = new RateLimiterConfig();
        r1.setDataSizeLimit(100);
        r1.setRowLimit(100);
        RateLimiterConfig r2 = new RateLimiterConfig();
        r2.setDataSizeLimit(1000);
        r2.setRowLimit(1000);
        context.getTaskParameter().setRateLimitConfig(r1);
        context.getParameter().setRateLimitConfig(r2);
        OscActionResult actionResult = omsMonitorDataTaskAction.execute(context);
        Assert.assertEquals(actionResult.getNextState(), OscStates.MODIFY_DATA_TASK.getState());
    }

    private ProjectStepResult createProjectStepResult(TaskStatus taskStatus) {
        ProjectStepResult ret = new ProjectStepResult();
        ret.setTaskStatus(taskStatus);
        ret.setFullVerificationResult(FullVerificationResult.UNCHECK);
        return ret;
    }
}
