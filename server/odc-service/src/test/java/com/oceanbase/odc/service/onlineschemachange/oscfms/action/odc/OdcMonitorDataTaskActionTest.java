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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.PrecheckResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;

/**
 * @author longpeng.zlp
 * @date 2025/4/7 10:28
 */
public class OdcMonitorDataTaskActionTest {
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    private OdcMonitorDataTaskAction dataTaskAction;


    @Before
    public void init() {
        onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        dataTaskAction = new OdcMonitorDataTaskAction(onlineSchemaChangeProperties);
    }

    @Test
    public void testParseResponse1() {
        String response = "{\"errorMessage\":null,"
                + "\"responseData\":{"
                + "\"data\":\"{"
                + "\\\"checkpoint\\\":\\\"1743994588\\\","
                + "\\\"enableIncrementMigrator\\\":\\\"true\\\","
                + "\\\"estimateMigrateRows\\\":\\\"380\\\","
                + "\\\"enableFullMigrator\\\":\\\"true\\\","
                + "\\\"fullMigratorProgress\\\":\\\"100\\\","
                + "\\\"fullMigratorDone\\\":\\\"true\\\","
                + "\\\"tableTotalRows\\\":\\\"370\\\""
                + "}\""
                + "},"
                + "\"success\":true}";
        SupervisorResponse supervisorResponse = JsonUtils.fromJson(response, SupervisorResponse.class);
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class)) {
            commandUtil.when(() -> OscCommandUtil.monitorTask(ArgumentMatchers.any())).thenReturn(supervisorResponse);
            ProjectStepResult result = dataTaskAction.getProjectStepResultInner("url");
            Assert.assertEquals(result.getPreCheckResult(), PrecheckResult.FINISHED);
            Assert.assertEquals(result.getTaskStatus(), TaskStatus.RUNNING);
            Assert.assertEquals(result.getFullVerificationResult(), FullVerificationResult.UNCHECK);
            Assert.assertEquals(result.getFullTransferEstimatedCount().longValue(), 380);
            Assert.assertEquals(result.getFullTransferFinishedCount().longValue(), 370);
            Assert.assertEquals(Double.valueOf(result.getFullTransferProgressPercentage()).intValue(), 100);
            Assert.assertEquals(result.getCurrentStepStatus(), OmsStepStatus.RUNNING.name());
            Assert.assertEquals(result.getIncrementCheckpoint().longValue(), 1743994588);
            Assert.assertEquals(Double.valueOf(result.getTaskPercentage()).intValue(), 95);
            Assert.assertEquals(result.getCurrentStep(), OscStepName.TRANSFER_APP_SWITCH.name());
        }
    }

    @Test
    public void testParseResponse2() {
        String response = "{\"errorMessage\":null,"
                + "\"responseData\":{"
                + "\"data\":\"{"
                + "\\\"checkpoint\\\":\\\"1743994588\\\","
                + "\\\"enableIncrementMigrator\\\":\\\"true\\\","
                + "\\\"estimateMigrateRows\\\":\\\"380\\\","
                + "\\\"enableFullMigrator\\\":\\\"true\\\","
                + "\\\"fullMigratorProgress\\\":\\\"100\\\","
                + "\\\"fullMigratorDone\\\":\\\"false\\\","
                + "\\\"tableTotalRows\\\":\\\"370\\\""
                + "}\""
                + "},"
                + "\"success\":true}";
        SupervisorResponse supervisorResponse = JsonUtils.fromJson(response, SupervisorResponse.class);
        try (MockedStatic<OscCommandUtil> commandUtil = Mockito.mockStatic(OscCommandUtil.class)) {
            commandUtil.when(() -> OscCommandUtil.monitorTask(ArgumentMatchers.any())).thenReturn(supervisorResponse);
            ProjectStepResult result = dataTaskAction.getProjectStepResultInner("url");
            Assert.assertEquals(result.getCurrentStep(), OscStepName.FULL_TRANSFER.name());
        }
    }

    @Test
    public void testProjectReady() {
        ProjectStepResult result = new ProjectStepResult();
        result.setCurrentStep(OscStepName.TRANSFER_APP_SWITCH.name());
        result.setIncrementCheckpoint(System.currentTimeMillis() / 1000);
        Assert.assertTrue(dataTaskAction.isMigrateTaskReady(result));
        result.setIncrementCheckpoint(System.currentTimeMillis() / 1000 - 100000);
        Assert.assertFalse(dataTaskAction.isMigrateTaskReady(result));
        result.setCurrentStep(OscStepName.FULL_TRANSFER.name());
        result.setIncrementCheckpoint(System.currentTimeMillis() / 1000);
        Assert.assertFalse(dataTaskAction.isMigrateTaskReady(result));
    }
}
