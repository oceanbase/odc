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

import static org.mockito.ArgumentMatchers.eq;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
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
            + "  ID BIGINT NOT NULL AUTO_INCREMENT,\n"
            + "  PRIMARY KEY (`ID`)\n"
            + ")";

    private static final String OB_ORACLE_TABLE_CREATE_TEMPLATE = "CREATE TABLE %s (\n"
            + "  ID NUMBER PRIMARY KEY\n"
            + ")";
    private static final String TABLE_DROP_TEMPLATE = "DROP TABLE %s";
    private static final String VIEW_CREATE_TEMPLATE = "CREATE VIEW %s AS SELECT 1 AS dummy FROM dual";
    private static final String VIEW_DROP_TEMPLATE = "DROP VIEW %s";
    private final static List<String> TABLE_NAME_LIST = Arrays.asList("TEST_TABLE_1", "TEST_TABLE_2", "TEST_TABLE_3");
    private final static List<String> VIEW_NAME_LIST = Arrays.asList("TEST_VIEW_10", "TEST_VIEW_20", "TEST_VIEW_30");

    private final static Long DATABASE_ID = 1L;

    private static final Long USER_ID = 1L;

    @BeforeClass
    public static void setUp() {
        clear();
        createTablesOrViewsByConnectType(ConnectType.OB_MYSQL, TABLE_NAME_LIST, OB_MYSQL_TABLE_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.OB_ORACLE, TABLE_NAME_LIST, OB_ORACLE_TABLE_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.MYSQL, TABLE_NAME_LIST, OB_MYSQL_TABLE_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.ORACLE, TABLE_NAME_LIST, OB_ORACLE_TABLE_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.OB_MYSQL, VIEW_NAME_LIST, VIEW_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.OB_ORACLE, VIEW_NAME_LIST, VIEW_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.MYSQL, VIEW_NAME_LIST, VIEW_CREATE_TEMPLATE);
        createTablesOrViewsByConnectType(ConnectType.ORACLE, VIEW_NAME_LIST, VIEW_CREATE_TEMPLATE);
    }

    @AfterClass
    public static void clear() {
        dropTablesOrViewsByConnectTypes(ConnectType.OB_MYSQL, VIEW_NAME_LIST, VIEW_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.OB_ORACLE, VIEW_NAME_LIST, VIEW_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.MYSQL, VIEW_NAME_LIST, VIEW_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.ORACLE, VIEW_NAME_LIST, VIEW_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.OB_MYSQL, TABLE_NAME_LIST, TABLE_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.OB_ORACLE, TABLE_NAME_LIST, TABLE_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.MYSQL, TABLE_NAME_LIST, TABLE_DROP_TEMPLATE);
        dropTablesOrViewsByConnectTypes(ConnectType.ORACLE, TABLE_NAME_LIST, TABLE_DROP_TEMPLATE);
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

    private List<DBObjectEntity> getTableEntities(DBObjectType dbObjectType, List<String> names) {
        return names.stream().map(name -> {
            DBObjectEntity entity = new DBObjectEntity();
            entity.setName(name);
            entity.setType(dbObjectType);
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

    private static void createTablesOrViewsByConnectType(ConnectType connectType, List<String> tableNames,
            String format) {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(connectType);
        SyncJdbcExecutor syncJdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        for (String name : tableNames) {
            syncJdbcExecutor.execute(String.format(format, name));
        }
    }

    private static void dropTablesOrViewsByConnectTypes(ConnectType connectType, List<String> tableNames,
            String format) {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(connectType);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        for (String name : tableNames) {
            try {
                jdbcOperations.execute(String.format(format, name));
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void testByConnectionTypeInIndividualSpace(ConnectType connectType)
            throws SQLException, InterruptedException {
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getIndivisualUser());
        testByConnectionType(connectType);
    }

    private void testByConnectionTypeInTeamSpace(ConnectType connectType) throws SQLException, InterruptedException {
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getTeamUser());
        Mockito.when(
                dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(Mockito.anyLong(), eq(DBObjectType.TABLE)))
                .thenReturn(getTableEntities(DBObjectType.TABLE, TABLE_NAME_LIST));
        Mockito.when(dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(Mockito.anyLong(), eq(DBObjectType.VIEW)))
                .thenReturn(getTableEntities(DBObjectType.VIEW, VIEW_NAME_LIST));
        Mockito.when(dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(Mockito.anyLong(),
                eq(DBObjectType.EXTERNAL_TABLE)))
                .thenReturn(Arrays.asList());
        testByConnectionType(connectType);
    }

    private void testByConnectionType(ConnectType connectType) throws SQLException, InterruptedException {
        Mockito.when(databaseService.detail(Mockito.any())).thenReturn(getDatabaseByConnectType(connectType));
        QueryTableParams params = QueryTableParams.builder()
                .databaseId(DATABASE_ID)
                .types(Arrays.asList(DBObjectType.TABLE, DBObjectType.VIEW, DBObjectType.EXTERNAL_TABLE))
                .includePermittedAction(false)
                .build();
        List<Table> list = tableService.list(params);
        Assert.assertFalse(list.isEmpty());
        List<String> tableList = list.stream().filter(table -> table.getType() == DBObjectType.TABLE).map(
                Table::getName).map(String::toUpperCase).collect(
                        Collectors.toList());
        List<String> viewList = list.stream().filter(table -> table.getType() == DBObjectType.VIEW).map(Table::getName)
                .map(String::toUpperCase).collect(
                        Collectors.toList());
        Assert.assertTrue(tableList.containsAll(TABLE_NAME_LIST));
        Assert.assertTrue(viewList.containsAll(VIEW_NAME_LIST));
    }

}
