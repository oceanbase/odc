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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscTestUtil;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

/**
 * @author longpeng.zlp
 * @date 2024/7/30 16:34
 * @since 4.3.1
 */
public class OdcSwapTableActionTest {
    private DBSessionManageFacade dbSessionManageFacade;
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    private OnlineSchemaChangeScheduleTaskParameters onlineSchemaChangeScheduleTaskParameters;
    private OdcSwapTableAction odcSwapTableAction;
    private OdcMonitorDataTaskAction odcMonitorDataTaskAction;
    private ProjectStepResult projectStepResult;

    @Before
    public void init() {
        dbSessionManageFacade = Mockito.mock(DBSessionManageFacade.class);
        onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        onlineSchemaChangeProperties.setEnableFullVerify(false);
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        onlineSchemaChangeScheduleTaskParameters = new OnlineSchemaChangeScheduleTaskParameters();
        onlineSchemaChangeScheduleTaskParameters.setUid("uid");
        onlineSchemaChangeScheduleTaskParameters.setOmsProjectId("projectID");
        onlineSchemaChangeScheduleTaskParameters.setDatabaseName("db");
        odcMonitorDataTaskAction = Mockito.mock(OdcMonitorDataTaskAction.class);
        odcSwapTableAction =
                new OdcSwapTableAction(dbSessionManageFacade, onlineSchemaChangeProperties, odcMonitorDataTaskAction);
        projectStepResult = new ProjectStepResult();
        projectStepResult.setIncrementCheckpoint(System.currentTimeMillis() / 1000 + 10);
    }

    @Test
    public void testCheckOdcProjectNotReady() throws Exception {
        OscActionContext context = OscTestUtil.createOcsActionContext(DialectType.OB_MYSQL,
                OscStates.SWAP_TABLE.getState(), TaskStatus.RUNNING);
        Mockito.when(odcMonitorDataTaskAction.getProjectStepResultInner(ArgumentMatchers.any()))
                .thenReturn(projectStepResult);
        Mockito.when(odcMonitorDataTaskAction.isMigrateTaskReady(ArgumentMatchers.any())).thenReturn(false);
        OscActionResult result = odcSwapTableAction.execute(context);
        Assert.assertEquals(result.getNextState(), OscStates.SWAP_TABLE.getState());
        Assert.assertFalse(odcSwapTableAction.isIncrementDataAppliedDone(null, onlineSchemaChangeScheduleTaskParameters,
                Collections.emptyMap(), 1000));
    }

    @Test
    public void testSwapTableReady() {
        Mockito.when(odcMonitorDataTaskAction.getProjectStepResultInner(ArgumentMatchers.any()))
                .thenReturn(projectStepResult);
        Mockito.when(odcMonitorDataTaskAction.isMigrateTaskReady(ArgumentMatchers.any())).thenReturn(true);
        Assert.assertTrue(odcSwapTableAction.isIncrementDataAppliedDone(null, onlineSchemaChangeScheduleTaskParameters,
                Collections.emptyMap(), 1000));
    }
}
