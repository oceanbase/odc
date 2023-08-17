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
package com.oceanbase.odc.service.session;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.PluginTestEnv;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.GeneralAsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;
import com.oceanbase.odc.service.connection.util.DefaultConnectionExtensionExecutor;

/**
 * Test case for {@link GeneralAsyncJdbcExecutor}
 *
 * @author yh263208
 * @date 2021-11-13 00:14
 * @since ODC_release_3.2.2
 */
public class OdcStatementCallBackTest extends PluginTestEnv {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void execute_inputOdcStatementCallBack_executeSucceed() throws Exception {
        List<String> sqls = Arrays.asList("select 1+3 from dual", "select 'hello,world' from dual");
        ConnectType connectType = ConnectType.OB_ORACLE;
        CloneableDataSourceFactory factory = getDataSourceFactory(connectType.getDialectType());
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            GeneralAsyncJdbcExecutor executor = getAyncJdbcExecutor(dataSource, factory, connectType.getDialectType());
            Future<List<JdbcGeneralResult>> future = executor.execute(generateStatementCallback(sqls, connectType));
            List<JdbcGeneralResult> results = future.get();
            Assert.assertEquals(2, results.size());
            for (JdbcGeneralResult result : results) {
                Assert.assertSame(result.getStatus(), SqlExecuteStatus.SUCCESS);
            }
            JdbcGeneralResult queryJdbcResult = results.get(0);
            JdbcGeneralResult queryJdbcResult1 = results.get(1);
            Assert.assertEquals("4", queryJdbcResult.getQueryResult().get(0, 0));
            Assert.assertEquals("hello,world", queryJdbcResult1.getQueryResult().get(0, 0));
        }
    }

    @Test
    public void execute_inputSqlWithSyntaxError_catchException() throws Exception {
        List<String> sqls = Arrays.asList("select xxx", "select 'hello,world' from dual");
        ConnectType connectType = ConnectType.OB_ORACLE;
        CloneableDataSourceFactory factory = getDataSourceFactory(connectType.getDialectType());
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            GeneralAsyncJdbcExecutor executor = getAyncJdbcExecutor(dataSource, factory, connectType.getDialectType());
            Future<List<JdbcGeneralResult>> future = executor.execute(generateStatementCallback(sqls, connectType));

            List<JdbcGeneralResult> results = future.get();
            Assert.assertEquals(2, results.size());

            JdbcGeneralResult canceledResult = results.get(1);
            Assert.assertEquals(SqlExecuteStatus.CANCELED, canceledResult.getStatus());

            JdbcGeneralResult failedResult = results.get(0);
            Assert.assertEquals(SqlExecuteStatus.FAILED, failedResult.getStatus());

            thrown.expect(SQLException.class);
            thrown.expectMessage("You have an error in your SQL syntax; check the manual that corresponds to your "
                    + "OceanBase version for the right syntax to use near 'xxx' at line 1");
            failedResult.getQueryResult();
        }
    }

    private OdcStatementCallBack generateStatementCallback(List<String> sqls, ConnectType connectType) {
        ConnectionSession connectionSession = new TestConnectionSession(
                "12", new ByteArrayInputStream("abcd".getBytes()), connectType);
        connectionSession.setAttribute(ConnectionSessionConstants.OB_VERSION, "2.2.77");
        ConnectionSessionUtil.initConsoleSessionTimeZone(connectionSession, "Asia/Shanghai");
        return new OdcStatementCallBack(SqlTuple.newTuples(sqls), connectionSession, true, 1000);
    }

    private CloneableDataSourceFactory getDataSourceFactory(DialectType dialectType) {
        TestDataSourceFactory dataSourceFactory = new TestDataSourceFactory(dialectType);
        dataSourceFactory.setAutoCommit(true);
        return dataSourceFactory;
    }

    private GeneralAsyncJdbcExecutor getAyncJdbcExecutor(DataSource dataSource,
            CloneableDataSourceFactory dataSourceFactory, DialectType dialectType) {
        SqlExecuteTaskManager taskManager =
                new DefaultSqlExecuteTaskManager(3, "GeneralAsyncJdbcExecutor", 10, TimeUnit.SECONDS);
        return new GeneralAsyncJdbcExecutor(dataSource, dataSourceFactory, taskManager,
                new DefaultConnectionExtensionExecutor(dialectType));
    }

}
