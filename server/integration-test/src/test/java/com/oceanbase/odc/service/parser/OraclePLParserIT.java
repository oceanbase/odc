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
package com.oceanbase.odc.service.parser;

import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.constant.PLObjectType;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;

import lombok.SneakyThrows;

/**
 * @author wenniu.ly
 * @date 2021/12/24
 */
public class OraclePLParserIT {
    private static final String basePath = "src/test/resources/parser/packages/";

    @Test
    public void test() {
        String pl = "create or replace package t_package  is v1 number; \n" + "type cur_emp is ref cursor;\n"
                + "procedure append_proc(p1 in out varchar2, p2 number);\n"
                + "function append_fun(p2 out varchar2) return varchar2;\n" + "end;";
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals(1, result.getTypeList().size());
        Assert.assertEquals(PLObjectType.CURSOR_TYPE.name(), result.getTypeList().get(0).getTypeName());
    }

    @Test
    public void test_parse_dbms_crypto() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_crypto.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
        Assert.assertEquals(12, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_dbms_crypto_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_crypto_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
    }

    @Test
    public void test_parse_dbms_debug() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_debug.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(15, result.getFunctionList().size());
        Assert.assertEquals(11, result.getProcedureList().size());
        Assert.assertEquals(73, result.getVaribaleList().size());
        Assert.assertEquals(6, result.getTypeList().size());
    }

    @Test
    public void test_parse_dbms_debug_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_debug_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_lob() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_lob.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(8, result.getFunctionList().size());
        Assert.assertEquals(23, result.getProcedureList().size());
        Assert.assertEquals(5, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_dbms_lob_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_lob_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_lock() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_lock.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(1, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_lock_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_lock_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(1, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_metadata() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_metadata.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
    }

    @Test
    public void test_parse_dbms_metadata_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_metadata_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
    }

    @Test
    public void test_parse_dbms_output() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_output.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(6, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_output_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_output_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(9, result.getVaribaleList().size());
        Assert.assertEquals(7, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_random() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_random.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
        Assert.assertEquals(2, result.getProcedureList().size());
        Assert.assertEquals(1, result.getTypeList().size());
    }

    @Test
    public void test_parse_dbms_random_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_random_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(19, result.getVaribaleList().size());
        Assert.assertEquals(5, result.getFunctionList().size());
        Assert.assertEquals(2, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_resource_manager() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_resource_manager.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(2, result.getVaribaleList().size());
        Assert.assertEquals(8, result.getProcedureList().size());
    }

    @Test
    public void test_parse_dbms_resource_manager_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_resource_manager_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_spm() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_spm.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_spm_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_spm_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_sql() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_sql.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_sql_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_sql_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_standard() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_standard.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_utility() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_utility.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_utility_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_utility_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_utl_encode() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_utl_encode.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_utl_encode_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_utl_encode_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_warning() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_warning.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_warning_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_warning_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_xa() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_xa.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_dbms_xa_body() {
        String pl = loadSqlFromFile(basePath + "dbms/dbms_xa_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_ereport_finance() {
        String pl = loadSqlFromFile(basePath + "sa/pkg_ereport_finance_due_new.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_components() {
        String pl = loadSqlFromFile(basePath + "sa/sa_components.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
        Assert.assertEquals(3, result.getProcedureList().size());
    }

    @Test
    public void test_parse_sa_components_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_components_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(3, result.getProcedureList().size());
    }

    @Test
    public void test_parse_sa_label_admin() {
        String pl = loadSqlFromFile(basePath + "sa/sa_label_admin.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_label_admin_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_label_admin_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_policy_admin() {
        String pl = loadSqlFromFile(basePath + "sa/sa_policy_admin.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_policy_admin_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_policy_admin_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_session() {
        String pl = loadSqlFromFile(basePath + "sa/sa_session.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_session_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_session_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_sysdba() {
        String pl = loadSqlFromFile(basePath + "sa/sa_sysdba.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_sysdba_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_sysdba_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_user_admin() {
        String pl = loadSqlFromFile(basePath + "sa/sa_user_admin.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE.getName(), result.getPlType());
    }

    @Test
    public void test_parse_sa_user_admin_body() {
        String pl = loadSqlFromFile(basePath + "sa/sa_user_admin_body.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
    }

    @SneakyThrows
    private String loadSqlFromFile(String path) {
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            int available = input.available();
            byte[] bytes = new byte[available];
            input.read(bytes);
            return new String(bytes);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    @Test
    public void test_parse_APK_COMMON_DATA_IMPORT() {
        String pl = loadSqlFromFile(basePath + "qa/APK_COMMON_DATA_IMPORT-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
        Assert.assertEquals(4, result.getProcedureList().size());
        Assert.assertEquals(2, result.getTypeList().size());
        Assert.assertEquals(40, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_BILL_TEMPLATE() {
        String pl = loadSqlFromFile(basePath + "qa/BILL_TEMPLATE-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(2, result.getFunctionList().size());
        Assert.assertEquals(12, result.getProcedureList().size());
        Assert.assertEquals(1, result.getTypeList().size());
        Assert.assertEquals(48, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_BP() {
        String pl = loadSqlFromFile(basePath + "qa/BP-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
        Assert.assertEquals(2, result.getProcedureList().size());
        Assert.assertEquals(25, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_CDRFILELOGCHECK() {
        String pl = loadSqlFromFile(basePath + "qa/CDRFILELOGCHECK-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
        Assert.assertEquals(2, result.getProcedureList().size());
        Assert.assertEquals(6, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_G_CORE() {
        String pl = loadSqlFromFile(basePath + "qa/G_CORE-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(2, result.getFunctionList().size());
        Assert.assertEquals(26, result.getProcedureList().size());
        Assert.assertEquals(84, result.getVaribaleList().size());
        Assert.assertEquals(22, result.getTypeList().size());
    }

    @Test
    public void test_parse_PKG_CDRBALANCE_CHECK() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_CDRBALANCE_CHECK-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
        Assert.assertEquals(3, result.getProcedureList().size());
        Assert.assertEquals(29, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_CHECK_PARAM() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_CHECK_PARAM-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(5, result.getFunctionList().size());
        Assert.assertEquals(1, result.getProcedureList().size());
        Assert.assertEquals(17, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_DATAFILE() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_DATAFILE-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(10, result.getProcedureList().size());
        Assert.assertEquals(101, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_GET_SPLIT_REGION_LIST() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_GET_SPLIT_REGION_LIST-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(1, result.getFunctionList().size());
        Assert.assertEquals(7, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_MASKCHECK() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_MASKCHECK-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(4, result.getFunctionList().size());
        Assert.assertEquals(1, result.getTypeList().size());
        Assert.assertEquals(14, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_RERATING() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_RERATING-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(3, result.getFunctionList().size());
        Assert.assertEquals(4, result.getProcedureList().size());
        Assert.assertEquals(30, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_ROAM() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_ROAM-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(2, result.getProcedureList().size());
        Assert.assertEquals(11, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_SOX_STAT() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_SOX_STAT-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(2, result.getTypeList().size());
        Assert.assertEquals(3, result.getProcedureList().size());
        Assert.assertEquals(25, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_TEMPALTE_DEF() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_TEMPALTE_DEF-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(7, result.getProcedureList().size());
        Assert.assertEquals(17, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_VALIDUSER() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_VALIDUSER-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(2, result.getFunctionList().size());
        Assert.assertEquals(8, result.getProcedureList().size());
        Assert.assertEquals(5, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_PKG_WORKORDER() {
        String pl = loadSqlFromFile(basePath + "qa/PKG_WORKORDER-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(22, result.getProcedureList().size());
        Assert.assertEquals(31, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_ROAMPROC() {
        String pl = loadSqlFromFile(basePath + "qa/ROAMPROC-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(4, result.getFunctionList().size());
        Assert.assertEquals("imsi", result.getFunctionList().get(0).getParams().get(0).getParamName());
        Assert.assertEquals(DBPLParamMode.IN,
                result.getFunctionList().get(0).getParams().get(0).getParamMode());
        Assert.assertEquals(2, result.getProcedureList().size());
        Assert.assertEquals(12, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_TU() {
        String pl = loadSqlFromFile(basePath + "qa/TU-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(6, result.getFunctionList().size());
        Assert.assertEquals(15, result.getProcedureList().size());
        Assert.assertEquals(20, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_TU_BAK() {
        String pl = loadSqlFromFile(basePath + "qa/TU_BAK-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(6, result.getFunctionList().size());
        Assert.assertEquals(12, result.getProcedureList().size());
        Assert.assertEquals(15, result.getVaribaleList().size());
    }

    @Test
    public void test_parse_UTIL() {
        String pl = loadSqlFromFile(basePath + "qa/UTIL-schema.sql");
        ParseOraclePLResult result = PLParser.parseOracle(pl);
        Assert.assertEquals(DBObjectType.PACKAGE_BODY.getName(), result.getPlType());
        Assert.assertEquals(3, result.getFunctionList().size());
        Assert.assertEquals(4, result.getProcedureList().size());
        Assert.assertEquals(25, result.getVaribaleList().size());
    }
}
