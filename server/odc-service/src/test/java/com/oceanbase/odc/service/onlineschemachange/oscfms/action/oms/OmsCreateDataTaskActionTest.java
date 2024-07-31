/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties.OmsProperties;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscTestUtil;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.DataSourceOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ConnectionProvider;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;

/**
 * @author longpeng.zlp
 * @date 2024/7/29 19:36
 * @since 4.3.1
 */
public class OmsCreateDataTaskActionTest {
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    private ConnectionConfig config;
    private SyncJdbcExecutor syncJdbcExecutor;
    private ConnectionSession connectionSession;
    private OmsProjectOpenApiService omsProjectOpenApiService;
    private DataSourceOpenApiService dataSourceOpenApiService;

    @Before
    public void init() {
        onlineSchemaChangeProperties = new OnlineSchemaChangeProperties();
        onlineSchemaChangeProperties.setEnableFullVerify(false);
        OmsProperties omsProperties = new OmsProperties();
        omsProperties.setUrl("127.0.0.1:8089");
        omsProperties.setRegion("default");
        omsProperties.setAuthorization("auth");
        onlineSchemaChangeProperties.setOms(omsProperties);
        config = new ConnectionConfig();
        config.setType(ConnectType.OB_MYSQL);
        syncJdbcExecutor = Mockito.mock(SyncJdbcExecutor.class);
        // init  mock
        connectionSession = Mockito.mock(ConnectionSession.class);
        Mockito.when(connectionSession.getSyncJdbcExecutor(ArgumentMatchers.anyString())).thenReturn(syncJdbcExecutor);
        omsProjectOpenApiService = Mockito.mock(OmsProjectOpenApiService.class);
        Mockito.when(omsProjectOpenApiService.createProject(ArgumentMatchers.any())).thenReturn("testProjectID");
        dataSourceOpenApiService = Mockito.mock(DataSourceOpenApiService.class);
        Mockito.when(dataSourceOpenApiService.createOceanBaseDataSource(ArgumentMatchers.any())).thenReturn("testOmsID");
    }

    @Test
    public void testOmsCreateDataTaskAction() throws Exception {
        OmsCreateDataTaskAction omsCreateDataTaskAction = new OmsCreateDataTaskAction(dataSourceOpenApiService,
            omsProjectOpenApiService,
            onlineSchemaChangeProperties
            );
        OscActionContext oscActionContext = OscTestUtil.createOcsActionContext(DialectType.OB_MYSQL,
            OscStates.CREATE_DATA_TASK.getState(), TaskStatus.RUNNING
            );
        oscActionContext.setScheduleTaskRepository(Mockito.mock(ScheduleTaskRepository.class));
        oscActionContext.setConnectionProvider(new ConnectionProvider() {
            @Override
            public ConnectionConfig connectionConfig() {
                return config;
            }

            @Override
            public ConnectionSession createConnectionSession() {
                return connectionSession;
            }
        });
        OscActionResult actionResult = omsCreateDataTaskAction.execute(oscActionContext);
        // check state
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
        // check create result
        Assert.assertEquals(oscActionContext.getTaskParameter().getOmsDataSourceId(), "testOmsID");
        Assert.assertEquals(oscActionContext.getTaskParameter().getOmsProjectId(), "testProjectID");
    }

    @Test
    public void testOmsCreateDataTaskActionReentrant() throws Exception {

        OmsCreateDataTaskAction omsCreateDataTaskAction = new OmsCreateDataTaskAction(dataSourceOpenApiService,
            omsProjectOpenApiService,
            onlineSchemaChangeProperties
        );
        OscActionContext oscActionContext = OscTestUtil.createOcsActionContext(DialectType.OB_MYSQL,
            OscStates.CREATE_DATA_TASK.getState(), TaskStatus.RUNNING
        );
        oscActionContext.getTaskParameter().setOmsDataSourceId("newOmsDsID");
        oscActionContext.getTaskParameter().setOmsProjectId("newProjectID");
        oscActionContext.setScheduleTaskRepository(Mockito.mock(ScheduleTaskRepository.class));
        oscActionContext.setConnectionProvider(new ConnectionProvider() {
            @Override
            public ConnectionConfig connectionConfig() {
                return config;
            }

            @Override
            public ConnectionSession createConnectionSession() {
                return connectionSession;
            }
        });
        OscActionResult actionResult = omsCreateDataTaskAction.execute(oscActionContext);
        // check state
        Assert.assertEquals(actionResult.getNextState(), OscStates.MONITOR_DATA_TASK.getState());
        // check create result
        Assert.assertEquals(oscActionContext.getTaskParameter().getOmsDataSourceId(), "newOmsDsID");
        Assert.assertEquals(oscActionContext.getTaskParameter().getOmsProjectId(), "newProjectID");
    }
}
