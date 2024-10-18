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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
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
    @Autowired
    private DBPLModifyHelper dbplModifyHelper;

    @Test
    public void editProcedureForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
            ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestProcedure = "DROP PROCEDURE IF EXISTS ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropTestProcedure);
        String createTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num INT, OUT square INT)\n"
                                     + "BEGIN\n"
                                     + "    SET square = num * num;\n"
                                     + "END";
        syncJdbcExecutor.execute(createTestProcedure);
        String dropODCTempProcedure =
            "DROP PROCEDURE IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE;
        syncJdbcExecutor.execute(dropODCTempProcedure);
        String editTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num1 INT, OUT square1 INT)\n"
                                   + "BEGIN\n"
                                   + "    SET square1 = num1 * num1;\n"
                                   + "END";
        exeuteEditPL(testConnectionSession, editTestProcedure);

        syncJdbcExecutor.execute(dropTestProcedure);
        syncJdbcExecutor.execute(dropODCTempProcedure);
    }


    @Test
    public void editProcedureForOBMysql_odcTempProcedureHaveExisted_failResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
            ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestProcedure = "DROP PROCEDURE IF EXISTS ODC_TEST_PROCEDURE;";
        syncJdbcExecutor.execute(dropTestProcedure);
        String createTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num INT, OUT square INT)\n"
                                     + "BEGIN\n"
                                     + "    SET square = num * num;\n"
                                     + "END";
        syncJdbcExecutor.execute(createTestProcedure);
        String dropODCTempProcedure =
            "DROP PROCEDURE IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE;
        syncJdbcExecutor.execute(dropODCTempProcedure);
        String createTempProcedure = "CREATE PROCEDURE " + DBPLModifyHelper.ODC_TEMPORARY_PROCEDURE
                                     + "(IN num INT, OUT square INT)\n"
                                     + "BEGIN\n"
                                     + "    SET square = num * num;\n"
                                     + "END";
        syncJdbcExecutor.execute(createTempProcedure);
        String editTestProcedure = "CREATE PROCEDURE ODC_TEST_PROCEDURE(IN num1 INT, OUT square1 INT)\n"
                                   + "BEGIN\n"
                                   + "    SET square1 = num1 * num1;\n"
                                   + "END";

        syncJdbcExecutor.execute(dropTestProcedure);
        syncJdbcExecutor.execute(dropODCTempProcedure);
    }

    @Test
    public void editFunctionForOBMysql_normal_successResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
            ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestFunction = "DROP FUNCTION IF EXISTS ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropTestFunction);
        String createTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num INT) \n"
                                    + "RETURNS INT\n"
                                    + "BEGIN\n"
                                    + "    RETURN num * num * num;\n"
                                    + "END";
        syncJdbcExecutor.execute(createTestFunction);
        String dropODCTempFunction =
            "DROP FUNCTION IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_FUNCTION;
        syncJdbcExecutor.execute(dropODCTempFunction);
        String editTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num1 INT) \n"
                                  + "RETURNS INT\n"
                                  + "BEGIN\n"
                                  + "    RETURN num1 * num1 * num1;\n"
                                  + "END";

        syncJdbcExecutor.execute(dropTestFunction);
        syncJdbcExecutor.execute(dropODCTempFunction);
    }

    @Test
    public void editFunctionForOBMysql_odcTempFunctionHaveExisted_failResultResult() throws Exception {
        ConnectionSession testConnectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        SyncJdbcExecutor syncJdbcExecutor = testConnectionSession.getSyncJdbcExecutor(
            ConnectionSessionConstants.CONSOLE_DS_KEY);
        String dropTestFunction = "DROP FUNCTION IF EXISTS ODC_TEST_FUNCTION;";
        syncJdbcExecutor.execute(dropTestFunction);
        String createTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num INT) \n"
                                    + "RETURNS INT\n"
                                    + "BEGIN\n"
                                    + "    RETURN num * num * num;\n"
                                    + "END";
        syncJdbcExecutor.execute(createTestFunction);
        String dropODCTempFunction =
            "DROP FUNCTION IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_FUNCTION;
        syncJdbcExecutor.execute(dropODCTempFunction);
        String createODCTempFunction =
            "CREATE FUNCTION " + DBPLModifyHelper.ODC_TEMPORARY_FUNCTION + "(num INT) \n"
            + "RETURNS INT\n"
            + "BEGIN\n"
            + "    RETURN num * num * num;\n"
            + "END";
        syncJdbcExecutor.execute(createODCTempFunction);
        String editTestFunction = "CREATE FUNCTION ODC_TEST_FUNCTION(num1 INT) \n"
                                  + "RETURNS INT\n"
                                  + "BEGIN\n"
                                  + "    RETURN num1 * num1 * num1;\n"
                                  + "END";

        syncJdbcExecutor.execute(dropTestFunction);
        syncJdbcExecutor.execute(dropODCTempFunction);
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
        String dropTestTrigger = "DROP TRIGGER IF EXISTS ODC_TEST_TRIGGER;";
        syncJdbcExecutor.execute(dropTestTrigger);
        String dropODCTempTestTrigger =
            "DROP TRIGGER IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER;
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        String dropTestTable = "DROP TABLE IF EXISTS ODC_TEST_TRIGGER_TABLE;";
        syncJdbcExecutor.execute(dropTestTable);
        String createTestTable = "CREATE TABLE ODC_TEST_TRIGGER_TABLE (\n"
                                 + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                                 + "    value INT\n"
                                 + ");";
        syncJdbcExecutor.execute(createTestTable);
        String createTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                                   + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                                   + "FOR EACH ROW\n"
                                   + "BEGIN\n"
                                   + "    SET NEW.value = NEW.value * 1;\n"
                                   + "END";
        syncJdbcExecutor.execute(createTestTrigger);
        String editTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                                 + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                                 + "FOR EACH ROW\n"
                                 + "BEGIN\n"
                                 + "    SET NEW.value = NEW.value * 2;\n"
                                 + "END";

        syncJdbcExecutor.execute(dropTestTrigger);
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        syncJdbcExecutor.execute(dropTestTable);
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
        String dropODCTempTestTrigger =
            "DROP TRIGGER IF EXISTS " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER;
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        String dropTestTable = "DROP TABLE IF EXISTS ODC_TEST_TRIGGER_TABLE;";
        syncJdbcExecutor.execute(dropTestTable);
        String createTestTable = "CREATE TABLE ODC_TEST_TRIGGER_TABLE (\n"
                                 + "    id INT AUTO_INCREMENT PRIMARY KEY,\n"
                                 + "    value INT\n"
                                 + ");";
        syncJdbcExecutor.execute(createTestTable);
        String createTestTrigger = "CREATE TRIGGER ODC_TEST_TRIGGER\n"
                                   + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
                                   + "FOR EACH ROW\n"
                                   + "BEGIN\n"
                                   + "    SET NEW.value = NEW.value * 1;\n"
                                   + "END";
        syncJdbcExecutor.execute(createTestTrigger);
        String createODCTempTrigger =
            "CREATE TRIGGER " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER
            + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
            + "FOR EACH ROW\n"
            + "BEGIN\n"
            + "    SET NEW.value = NEW.value * 1;\n"
            + "END";
        syncJdbcExecutor.execute(createODCTempTrigger);
        String editTestTrigger =
            "CREATE TRIGGER " + DBPLModifyHelper.ODC_TEMPORARY_TRIGGER + "\n"
            + "BEFORE INSERT ON ODC_TEST_TRIGGER_TABLE\n"
            + "FOR EACH ROW\n"
            + "BEGIN\n"
            + "    SET NEW.value = NEW.value * 2;\n"
            + "END";

        syncJdbcExecutor.execute(dropTestTrigger);
        syncJdbcExecutor.execute(dropODCTempTestTrigger);
        syncJdbcExecutor.execute(dropTestTable);
    }


    private void exeuteEditPL(ConnectionSession testConnectionSession, String editTestProcedure)
        throws InterruptedException {
        EditPLReq editPLReq = new EditPLReq();
        editPLReq.setSql(editTestProcedure);
        editPLReq.setPlType(DBObjectType.PROCEDURE);
        editPLReq.setPlName("ODC_TEST_PROCEDURE");
        EditPLResp editPLResp = dbplModifyHelper.editPL(testConnectionSession, editPLReq, true);
    }

}
