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
package com.oceanbase.odc.service.db;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.connection.table.model.QueryTableParams;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/9/9 14:50
 * @since: 4.3.3
 */
public class TableServiceTest extends ServiceTestEnv {
    @Autowired
    private TableService tableService;
    @MockBean
    private DBObjectRepository dbObjectRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private DatabaseService databaseService;
    @MockBean
    private DBResourcePermissionHelper dbResourcePermissionHelper;

    private static final String OB_MYSQL_TABLE_CREATE_TEMPLATE = "CREATE TABLE IF NOT EXISTS %s (\n"
            + "  id BIGINT NOT NULL AUTO_INCREMENT,\n"
            + "  PRIMARY key (`id`)\n"
            + ")";

    private static final String OB_ORACLE_TABLE_CREATE_TEMPLATE = "CREATE TABLE %s (\n"
            + "  id NUMBER PRIMARY KEY\n"
            + ")";

    private final static List<String> TABLE_NAME_LIST = Arrays.asList("test_table_1", "test_table_2", "test_table_3");

    private final static Long DATABASE_ID = 1L;

    private static final Long USER_ID = 1L;

    @BeforeClass
    public static void setUp() {
        createTablesByConnectType(ConnectType.OB_MYSQL, TABLE_NAME_LIST, OB_MYSQL_TABLE_CREATE_TEMPLATE);
        createTablesByConnectType(ConnectType.OB_ORACLE, TABLE_NAME_LIST, OB_ORACLE_TABLE_CREATE_TEMPLATE);
        createTablesByConnectType(ConnectType.MYSQL, TABLE_NAME_LIST, OB_MYSQL_TABLE_CREATE_TEMPLATE);
        createTablesByConnectType(ConnectType.ORACLE, TABLE_NAME_LIST, OB_ORACLE_TABLE_CREATE_TEMPLATE);
    }

    @AfterClass
    public static void clear() {
        dropTablesByConnectTypes(ConnectType.OB_MYSQL, TABLE_NAME_LIST);
        dropTablesByConnectTypes(ConnectType.OB_ORACLE, TABLE_NAME_LIST);
        dropTablesByConnectTypes(ConnectType.MYSQL, TABLE_NAME_LIST);
        dropTablesByConnectTypes(ConnectType.ORACLE, TABLE_NAME_LIST);
    }

    @Test
    public void list_whenConnectionTypeIsOBMysqlAndTypesIsEmpty_getEmptyList()
            throws SQLException, InterruptedException {
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(DATABASE_ID)
                .types(Collections.emptyList())
                .includePermittedAction(false)
                .build();
        List<Table> list = tableService.list(params);
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void list_whenConnectionTypeIsOBMysqlAndOrganizationTypeIsIndividual_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInIndividualSpace(ConnectType.OB_MYSQL);
    }

    @Test
    public void list_whenConnectionTypeIsOBMysqlAndOrganizationTypeIsTeam_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInTeamSpace(ConnectType.OB_MYSQL);
    }

    @Test
    public void list_whenConnectionTypeIsMysqlAndOrganizationTypeIsIndividual_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInIndividualSpace(ConnectType.MYSQL);
    }

    @Test
    public void list_whenConnectionTypeIsMysqlAndOrganizationTypeIsTeam_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInTeamSpace(ConnectType.MYSQL);
    }

    @Test
    public void list_whenConnectionTypeIsOBOracleAndOrganizationTypeIsIndividual_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInIndividualSpace(ConnectType.OB_ORACLE);
    }

    @Test
    public void list_whenConnectionTypeIsOBOracleAndOrganizationTypeIsTeam_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInTeamSpace(ConnectType.OB_ORACLE);
    }

    @Test
    public void list_whenConnectionTypeIsOracleAndOrganizationTypeIsIndividual_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInIndividualSpace(ConnectType.ORACLE);
    }

    @Test
    public void list_whenConnectionTypeIsOracleAndOrganizationTypeIsTeam_succeed()
            throws SQLException, InterruptedException {
        testByConnectionTypeInTeamSpace(ConnectType.ORACLE);
    }

    private List<DBObjectEntity> getTableEntities() {
        return TABLE_NAME_LIST.stream().map(name -> {
            DBObjectEntity entity = new DBObjectEntity();
            entity.setName(name);
            entity.setType(DBObjectType.TABLE);
            entity.setDatabaseId(DATABASE_ID);
            return entity;
        }).collect(java.util.stream.Collectors.toList());
    }

    private User getIndivisualUser() {
        User user = User.of(USER_ID);
        user.setOrganizationType(OrganizationType.INDIVIDUAL);
        return user;
    }

    private User getTeamUser() {
        User user = User.of(USER_ID);
        user.setOrganizationType(OrganizationType.TEAM);
        return user;
    }

    private Database getDatabaseByConnectType(ConnectType connectType) {
        Database database = new Database();
        database.setId(DATABASE_ID);
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(connectType);
        ConnectionConfig connectionConfig = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        String currentSchema = ConnectionSessionUtil.getCurrentSchema(session);
        database.setName(currentSchema);
        database.setDataSource(connectionConfig);
        return database;
    }

    private boolean containsAllIgnoreCase(List<String> list, List<String> toCheck) {
        for (String item : toCheck) {
            boolean found = false;
            for (String listItem : list) {
                if (listItem.equalsIgnoreCase(item)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static void createTablesByConnectType(ConnectType connectType, List<String> tableNames, String format) {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(connectType);
        SyncJdbcExecutor syncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        for (String name : tableNames) {
            syncJdbcExecutor.execute(String.format(format, name));
        }
    }

    private static void dropTablesByConnectTypes(ConnectType connectType, List<String> tableNames) {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(connectType);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        for (String name : tableNames) {
            jdbcOperations.execute(String.format("drop table %s", name));
        }
    }

    private void testByConnectionTypeInIndividualSpace(ConnectType connectType)
            throws SQLException, InterruptedException {
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getIndivisualUser());
        Mockito.when(databaseService.detail(Mockito.any())).thenReturn(getDatabaseByConnectType(connectType));
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(DATABASE_ID)
                .types(Collections.singletonList(DBObjectType.TABLE))
                .includePermittedAction(false)
                .build();
        List<Table> list = tableService.list(params);
        Assert.assertFalse(list.isEmpty());
        List<String> nameList = list.stream().map(Table::getName).collect(Collectors.toList());
        Assert.assertTrue(containsAllIgnoreCase(nameList, TABLE_NAME_LIST));
    }

    private void testByConnectionTypeInTeamSpace(ConnectType obMysql) throws SQLException, InterruptedException {
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getTeamUser());
        Mockito.when(databaseService.detail(Mockito.any())).thenReturn(getDatabaseByConnectType(obMysql));
        Mockito.when(dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(Mockito.anyLong(), Mockito.any()))
                .thenReturn(getTableEntities());
        Mockito.when(dbResourcePermissionHelper.getTablePermissions(Mockito.any())).thenReturn(new HashMap<>());
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(DATABASE_ID)
                .types(Collections.singletonList(DBObjectType.TABLE))
                .includePermittedAction(false)
                .build();
        List<Table> list = tableService.list(params);
        Assert.assertFalse(list.isEmpty());
        List<String> nameList = list.stream().map(Table::getName).collect(Collectors.toList());
        Assert.assertTrue(containsAllIgnoreCase(nameList, TABLE_NAME_LIST));
    }

}
