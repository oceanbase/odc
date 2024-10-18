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

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.db.model.EditPLReq;
import com.oceanbase.odc.service.db.model.EditPLResp;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/10/18 10:53
 * @since: 4.3.3
 */
public class DBPLModifyHelperTest extends ServiceTestEnv {
    private static final String ODC_TEST_PROCEDURE = "ODC_TEST_PROCEDURE";
    private static final String ODC_TEST_FUNCTION = "ODC_TEST_FUNCTION";
    private static final String ODC_TEST_TRIGGER = "ODC_TEST_TRIGGER";
    private static final String ODC_TEST_TRIGGER_TABLE = "ODC_TEST_TRIGGER_TABLE";

    @Autowired
    private DBPLModifyHelper dbplModifyHelper;


    @BeforeClass
    public static void setUp() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        // prepare test environment for procedure
        executeDropPLSql(DBObjectType.PROCEDURE, ODC_TEST_PROCEDURE);
        executeDropPLSql(DBObjectType.PROCEDURE, DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE);
        String createTestProcedure = "CREATE PROCEDURE " + ODC_TEST_PROCEDURE + "(IN num INT, OUT square INT)\n"
                + "BEGIN\n"
                + "    SET square = num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestProcedure);
        // prepare test environment for function
        executeDropPLSql(DBObjectType.FUNCTION, ODC_TEST_FUNCTION);
        executeDropPLSql(DBObjectType.FUNCTION, DBPLModifyHelper.ODC_TEMPORARY_FUNCTION);
        String createTestFunction = "CREATE FUNCTION " + ODC_TEST_FUNCTION + "(num INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num * num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTestFunction);
        // prepare test environment for trigger
        executeDropPLSql(DBObjectType.TRIGGER, ODC_TEST_TRIGGER);
        executeDropPLSql(DBObjectType.TRIGGER, DBPLModifyHelper.ODC_TEMPORARY_TRIGGER);
        executeDropPLSql(DBObjectType.TABLE, ODC_TEST_TRIGGER_TABLE);
        String createTestTable = "CREATE TABLE " + ODC_TEST_TRIGGER_TABLE + "(\n"
                + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                + "    value INT\n"
                + ");";
        syncJdbcExecutor.execute(createTestTable);
        String createTestTrigger = "CREATE TRIGGER " + ODC_TEST_TRIGGER + "\n"
                + "BEFORE INSERT ON " + ODC_TEST_TRIGGER_TABLE + "\n"
                + "FOR EACH ROW\n"
                + "BEGIN\n"
                + "    SET NEW.value = NEW.value * 1;\n"
                + "END";
        syncJdbcExecutor.execute(createTestTrigger);
    }


    @AfterClass
    public static void clear() throws Exception {
        // clear test environment for procedure
        executeDropPLSql(DBObjectType.PROCEDURE, ODC_TEST_PROCEDURE);
        executeDropPLSql(DBObjectType.PROCEDURE, DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE);
        // clear test environment for function
        executeDropPLSql(DBObjectType.FUNCTION, ODC_TEST_FUNCTION);
        executeDropPLSql(DBObjectType.FUNCTION, DBPLModifyHelper.ODC_TEMPORARY_FUNCTION);
        // clear test environment for trigger
        executeDropPLSql(DBObjectType.TRIGGER, ODC_TEST_TRIGGER);
        executeDropPLSql(DBObjectType.TRIGGER, DBPLModifyHelper.ODC_TEMPORARY_TRIGGER);
        executeDropPLSql(DBObjectType.TABLE, ODC_TEST_TRIGGER_TABLE);
    }

    @Test
    public void editProcedureForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        String editTestProcedure = "CREATE PROCEDURE " + ODC_TEST_PROCEDURE + "(IN num1 INT, OUT square1 INT)\n"
                + "BEGIN\n"
                + "    SET square1 = num1 * num1;\n"
                + "END";
        EditPLResp editPLResp = executeEditPL(testConnectionSession, editTestProcedure, ODC_TEST_PROCEDURE,
                DBObjectType.PROCEDURE);
        assertNotNull(editPLResp);
        Assert.assertFalse(editPLResp.isShouldIntercepted());
    }


    @Test
    public void editProcedureForOBMysql_odcTempProcedureHaveExisted_failResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String createTempProcedure = "CREATE PROCEDURE " + DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE
                + "(IN num INT, OUT square INT)\n"
                + "BEGIN\n"
                + "    SET square = num * num;\n"
                + "END";
        syncJdbcExecutor.execute(createTempProcedure);
        String editTestProcedure = "CREATE PROCEDURE " + ODC_TEST_PROCEDURE + "(IN num1 INT, OUT square1 INT)\n"
                + "BEGIN\n"
                + "    SET square1 = num1 * num1;\n"
                + "END";
        assertThrows(BadSqlGrammarException.class,
                () -> executeEditPL(testConnectionSession, editTestProcedure, ODC_TEST_PROCEDURE,
                        DBObjectType.PROCEDURE));
        executeDropPLSql(DBObjectType.PROCEDURE, DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE);
    }

    @Test
    public void editFunctionForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String editTestFunction = "CREATE FUNCTION " + ODC_TEST_FUNCTION + "(num1 INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num1 * num1 * num1;\n"
                + "END";
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(testConnectionSession),
                DBPLModifyHelper.OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
            assertThrows(BadRequestException.class,
                    () -> executeEditPL(testConnectionSession, editTestFunction, ODC_TEST_FUNCTION,
                            DBObjectType.FUNCTION));
        } else {
            EditPLResp editPLResp = executeEditPL(testConnectionSession, editTestFunction, ODC_TEST_FUNCTION,
                    DBObjectType.FUNCTION);
            assertNotNull(editPLResp);
            Assert.assertFalse(editPLResp.isShouldIntercepted());
        }
    }

    @Test
    public void editFunctionForOBMysql_odcTempFunctionHaveExisted_failResultResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String createODCTempFunction =
                "CREATE FUNCTION " + DBPLModifyHelper.ODC_TEMPORARY_FUNCTION + "(num INT) \n"
                        + "RETURNS INT\n"
                        + "BEGIN\n"
                        + "    RETURN num * num * num;\n"
                        + "END";
        syncJdbcExecutor.execute(createODCTempFunction);
        String editTestFunction = "CREATE FUNCTION " + ODC_TEST_FUNCTION + "(num1 INT) \n"
                + "RETURNS INT\n"
                + "BEGIN\n"
                + "    RETURN num1 * num1 * num1;\n"
                + "END";
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(testConnectionSession),
                DBPLModifyHelper.OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
            assertThrows(BadRequestException.class,
                    () -> executeEditPL(testConnectionSession, editTestFunction, ODC_TEST_FUNCTION,
                            DBObjectType.FUNCTION));
        } else {
            assertThrows(BadSqlGrammarException.class,
                    () -> executeEditPL(testConnectionSession, editTestFunction, ODC_TEST_FUNCTION,
                            DBObjectType.FUNCTION));
        }
        executeDropPLSql(DBObjectType.FUNCTION, DBPLModifyHelper.ODC_TEMPORARY_FUNCTION);
    }

    @Test
    public void editTriggerForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        if (VersionUtils.isLessThan(ConnectionSessionUtil.getVersion(testConnectionSession),
                DBPLModifyHelper.OB_VERSION_SUPPORT_MULTIPLE_SAME_TRIGGERS)) {
            return;
        }
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String editTestTrigger = "CREATE TRIGGER " + ODC_TEST_TRIGGER + "\n"
                + "BEFORE INSERT ON " + ODC_TEST_TRIGGER_TABLE + "\n"
                + "FOR EACH ROW\n"
                + "BEGIN\n"
                + "    SET NEW.value = NEW.value * 2;\n"
                + "END";

        EditPLResp editPLResp = executeEditPL(testConnectionSession, editTestTrigger, ODC_TEST_TRIGGER,
                DBObjectType.TRIGGER);
        assertNotNull(editPLResp);
        Assert.assertFalse(editPLResp.isShouldIntercepted());
    }

    @Test
    public void editTriggerForOBMysql_odcTempTriggerHaveExisted_failResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        if (VersionUtils.isLessThanOrEqualsTo(ConnectionSessionUtil.getVersion(testConnectionSession), "4.2")) {
            return;
        }
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestTrigger =
                "DROP TRIGGER IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER;
        syncJdbcExecutor.execute(dropTestTrigger);
        String createODCTempTrigger =
                "CREATE TRIGGER " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER
                        + "BEFORE INSERT ON " + ODC_TEST_TRIGGER_TABLE + "\n"
                        + "FOR EACH ROW\n"
                        + "BEGIN\n"
                        + "    SET NEW.value = NEW.value * 1;\n"
                        + "END";
        syncJdbcExecutor.execute(createODCTempTrigger);
        String editTestTrigger =
                "CREATE TRIGGER " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER + "\n"
                        + "BEFORE INSERT ON " + ODC_TEST_TRIGGER_TABLE + "\n"
                        + "FOR EACH ROW\n"
                        + "BEGIN\n"
                        + "    SET NEW.value = NEW.value * 2;\n"
                        + "END";

        assertThrows(BadSqlGrammarException.class,
                () -> executeEditPL(testConnectionSession, editTestTrigger, ODC_TEST_TRIGGER,
                        DBObjectType.TRIGGER));
        executeDropPLSql(DBObjectType.TRIGGER, DBPLModifyHelper.ODC_TEMPORARY_TRIGGER);
    }


    private EditPLResp executeEditPL(ConnectionSession testConnectionSession, String editTestPL, String plName,
            DBObjectType plType)
            throws InterruptedException {
        EditPLReq editPLReq = new EditPLReq();
        editPLReq.setSql(editTestPL);
        editPLReq.setPlType(plType);
        editPLReq.setPlName(plName);
        return dbplModifyHelper.editPL(testConnectionSession, editPLReq, false);
    }

    private static void executeDropPLSql(DBObjectType plType, String plName) {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestProcedure = "DROP " + plType + " IF EXISTS " + plName;
        syncJdbcExecutor.execute(dropTestProcedure);
    }
}
