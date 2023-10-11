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
package com.oceanbase.odc.service.shadowtable;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Objects;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncReq;

public class ShadowTableComparingServiceTest extends ServiceTestEnv {
    @Autowired
    private ShadowTableComparingService shadowTableComparingService;

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private DatabaseService databaseService;

    private static ConnectionSession connectionSession;

    private final static Long CONNECTION_ID = 1L;

    @BeforeClass
    public static void setUp() throws Exception {
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(
                "drop table if exists t1; "
                        + "drop table if exists t2;"
                        + "drop table if exists t3;"
                        + "drop table if exists __t_t3;"
                        + "create table t1(a varchar(20));"
                        + "create table t2(a varchar(20));"
                        + "create table t3(a varchar(20));"
                        + "create table __t_t3(a varchar(20));");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (Objects.nonNull(connectionSession)) {
            connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY).update(
                    "drop table if exists t1;"
                            + "drop table if exists t2;"
                            + "drop table if exists t3;"
                            + "drop table if exists __t_t3;");
        }
    }

    @Test
    public void createShadowTableSync_HaveOriTablesNoDesTables_Success() {
        ConnectionConfig config = buildTestConnection(DialectType.OB_MYSQL);
        Database database = new Database();
        database.setId(1L);
        database.setName(config.getDefaultSchema());
        Project project = new Project();
        project.setId(1L);
        database.setProject(project);
        database.setDataSource(config);
        Mockito.when(databaseService.detail(1L)).thenReturn(database);
        when(connectionService.getForConnectionSkipPermissionCheck(eq(CONNECTION_ID))).thenReturn(config);
        ShadowTableSyncReq req = new ShadowTableSyncReq();
        req.setConnectionId(CONNECTION_ID);
        req.setDatabaseId(1L);
        req.setSchemaName(config.getDefaultSchema());
        req.setOriginTableNames(Arrays.asList("t1", "t2"));
        req.setDestTableNames(Arrays.asList("__t_t1", "__t_t2"));
        String taskId = shadowTableComparingService.createShadowTableSync(req);
        await().atMost(20, SECONDS)
                .until(() -> shadowTableComparingService.listShadowTableSyncs(Long.valueOf(taskId)).isCompleted());
    }

    @Test
    public void createShadowTableSync_HaveOriTablesHaveDesTables_Success() {
        ConnectionConfig config = buildTestConnection(DialectType.OB_MYSQL);
        Database database = new Database();
        database.setId(1L);
        database.setName(config.getDefaultSchema());
        Project project = new Project();
        project.setId(1L);
        database.setProject(project);
        database.setDataSource(config);
        Mockito.when(databaseService.detail(1L)).thenReturn(database);
        when(connectionService.getForConnectionSkipPermissionCheck(eq(CONNECTION_ID))).thenReturn(config);
        ShadowTableSyncReq req = new ShadowTableSyncReq();
        req.setConnectionId(CONNECTION_ID);
        req.setDatabaseId(1L);
        req.setSchemaName(config.getDefaultSchema());
        req.setOriginTableNames(Arrays.asList("t3"));
        req.setDestTableNames(Arrays.asList("__t_t3"));
        String taskId = shadowTableComparingService.createShadowTableSync(req);
        await().atMost(20, SECONDS)
                .until(() -> shadowTableComparingService.listShadowTableSyncs(Long.valueOf(taskId)).isCompleted());
    }

    @Test
    public void createShadowTableSync_NoOriTables_Success() {
        ConnectionConfig config = buildTestConnection(DialectType.OB_MYSQL);
        Database database = new Database();
        database.setId(1L);
        database.setName(config.getDefaultSchema());
        Project project = new Project();
        project.setId(1L);
        database.setProject(project);
        database.setDataSource(config);
        Mockito.when(databaseService.detail(1L)).thenReturn(database);
        when(connectionService.getForConnectionSkipPermissionCheck(eq(CONNECTION_ID))).thenReturn(config);
        ShadowTableSyncReq req = new ShadowTableSyncReq();
        req.setConnectionId(CONNECTION_ID);
        req.setDatabaseId(1L);
        req.setSchemaName(config.getDefaultSchema());
        req.setOriginTableNames(Arrays.asList("not_exists_table"));
        req.setDestTableNames(Arrays.asList("__t_not_exists_table"));
        String taskId = shadowTableComparingService.createShadowTableSync(req);
        await().atMost(20, SECONDS)
                .until(() -> shadowTableComparingService.listShadowTableSyncs(Long.valueOf(taskId)).isCompleted());
    }

    private static ConnectionConfig buildTestConnection(DialectType dialectType) {
        ConnectionConfig connection = TestConnectionUtil.getTestConnectionConfig(ConnectType.from(dialectType));
        connection.setId(CONNECTION_ID);
        return connection;
    }
}
