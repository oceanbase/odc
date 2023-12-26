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
package com.oceanbase.odc.core.sql.execute;

import java.sql.ResultSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.execute.tool.TestDataSourceFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Test case for {@link GeneralAsyncJdbcExecutor}
 *
 * @author yh263208
 * @date 2021-11-13 00:14
 * @since ODC_release_3.2.2
 */
@Slf4j
public class GeneralAsyncJdbcExecutorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void execute_sleep1sStmt_executeSucceed() throws Exception {
        CloneableDataSourceFactory factory = getDataSourceFactory(DialectType.OB_MYSQL);
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            GeneralAsyncJdbcExecutor executor = getAyncJdbcExecutor(dataSource, factory);
            Future<Integer> future = executor.execute((StatementCallback<Integer>) stmt -> {
                try (ResultSet resultSet = stmt.executeQuery("select sleep(1) from dual")) {
                    Assert.assertTrue(resultSet.next());
                    return resultSet.getInt(1);
                }
            });
            Assert.assertEquals(0, (int) future.get());
        }
    }

    @Test
    public void execute_throwAnException_catchException() throws Exception {
        CloneableDataSourceFactory factory = getDataSourceFactory(DialectType.OB_MYSQL);
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            GeneralAsyncJdbcExecutor executor = getAyncJdbcExecutor(dataSource, factory);
            Future<Integer> future = executor.execute((StatementCallback<Integer>) statement -> {
                throw new RuntimeException("Test exception");
            });
            thrown.expectMessage("Test exception");
            thrown.expect(ExecutionException.class);
            future.get();
        }
    }

    private CloneableDataSourceFactory getDataSourceFactory(DialectType dialectType) {
        TestDataSourceFactory dataSourceFactory = new TestDataSourceFactory(dialectType);
        dataSourceFactory.setAutoCommit(true);
        return dataSourceFactory;
    }

    private GeneralAsyncJdbcExecutor getAyncJdbcExecutor(DataSource dataSource,
            CloneableDataSourceFactory dataSourceFactory) {
        SqlExecuteTaskManager taskManager =
                new DefaultSqlExecuteTaskManager(3,
                        "GeneralAsyncJdbcExecutor", 10, TimeUnit.SECONDS);
        return new GeneralAsyncJdbcExecutor(dataSource, dataSourceFactory, taskManager, new TestSessionOperations());
    }

}
