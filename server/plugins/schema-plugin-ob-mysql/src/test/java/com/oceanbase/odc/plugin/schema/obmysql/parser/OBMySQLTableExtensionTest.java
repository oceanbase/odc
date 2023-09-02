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

package com.oceanbase.odc.plugin.schema.obmysql.parser;

import java.sql.Connection;
import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.connect.obmysql.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

/**
 * @author yaobin
 * @date 2023-09-02
 * @since 4.2.0
 */
public class OBMySQLTableExtensionTest {

    private static final TestDBConfiguration configuration =
            TestDBConfigurations.getInstance().getTestOBMysqlConfiguration();
    private static Connection connection;
    private static TableExtensionPoint tableEp;

    @BeforeClass
    public static void setUp() throws Exception {
        connection = configuration.getDataSource().getConnection();
        tableEp = new OBMySQLTableExtension();
    }


    @Test
    public void test_checkLowerCaseTableExist() {
        String t = "check_table0";
        JdbcOperationsUtil.getJdbcOperations(connection).execute(getCreateSql(t));
        try {
            boolean checkResult = tableEp.checkTableExist(connection, configuration.getDefaultDBName(), t);
            Assert.assertTrue(checkResult);
        } finally {
            JdbcOperationsUtil.getJdbcOperations(connection).execute(getDropSql(t));
        }
    }

    @Test
    public void test_checkUpperCaseTableExist() {
        String t = "CHECK_TABLE1";
        JdbcOperationsUtil.getJdbcOperations(connection).execute(getCreateSql(t));
        try {
            boolean checkResult = tableEp.checkTableExist(connection, configuration.getDefaultDBName(), t);
            Assert.assertTrue(checkResult);
        } finally {
            JdbcOperationsUtil.getJdbcOperations(connection).execute(getDropSql(t));
        }
    }

    @Test
    public void test_checkUpperCaseTableWithQuoteExist() {
        String t = "`CHECK_TABLE2`";
        JdbcOperationsUtil.getJdbcOperations(connection).execute(getCreateSql(t));
        try {
            boolean checkResult = tableEp.checkTableExist(connection, configuration.getDefaultDBName(), t);
            Assert.assertTrue(checkResult);
        } finally {
            JdbcOperationsUtil.getJdbcOperations(connection).execute(getDropSql(t));
        }
    }

    @Test
    public void test_checkLowerCaseTableWithQuoteExist() {
        String t = "`check_table3`";
        JdbcOperationsUtil.getJdbcOperations(connection).execute(getCreateSql(t));
        try {
            boolean checkResult = tableEp.checkTableExist(connection, configuration.getDefaultDBName(), t);
            Assert.assertTrue(checkResult);
        } finally {
            JdbcOperationsUtil.getJdbcOperations(connection).execute(getDropSql(t));
        }
    }

    @Test
    public void test_checkRandomTableNotExist() {
        String t = StringUtils.uuidNoHyphen();
        boolean checkResult = tableEp.checkTableExist(connection, configuration.getDefaultDBName(), t);
        Assert.assertFalse(checkResult);
    }


    private String getCreateSql(String tableName) {
        return MessageFormat.format("CREATE TABLE {0} (id int(11))", tableName);
    }

    private String getDropSql(String tableName) {
        return MessageFormat.format("DROP TABLE {0} ", tableName);
    }

}
