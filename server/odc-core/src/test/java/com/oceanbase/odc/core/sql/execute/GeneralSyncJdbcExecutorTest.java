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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.tool.TestDataSourceFactory;

/**
 * @author yaobin
 * @date 2023-01-13
 * @since 4.1.0
 */
public class GeneralSyncJdbcExecutorTest {

    @Test
    public void queryForStream_normalSQLWithoutPS_executeSucceed() {
        CloneableDataSourceFactory dataSourceFactory = getDataSourceFactory();
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) dataSourceFactory.getDataSource()) {
            GeneralSyncJdbcExecutor executor = new GeneralSyncJdbcExecutor(dataSource);
            queryForStream(() -> executor.queryForStream("select 1+2", (rs, i) -> rs.getInt(1)));
        }
    }

    @Test
    public void queryForStream_normalSQLWithPS_executeSucceed() {
        CloneableDataSourceFactory dataSourceFactory = getDataSourceFactory();
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) dataSourceFactory.getDataSource()) {
            GeneralSyncJdbcExecutor executor = new GeneralSyncJdbcExecutor(dataSource);
            queryForStream(() -> executor.queryForStream((conn) -> conn.prepareStatement("select 1+2"),
                    (rs, i) -> rs.getInt(1)));
        }
    }

    @Test
    public void queryForStream_prepareStatSql_executeSucceed() {
        CloneableDataSourceFactory dataSourceFactory = getDataSourceFactory();
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) dataSourceFactory.getDataSource()) {
            GeneralSyncJdbcExecutor executor = new GeneralSyncJdbcExecutor(dataSource);
            queryForStream(() -> executor.queryForStream("select 1+2", null, (rs, i) -> rs.getInt(1)));
        }
    }

    @Test
    public void queryForStream_prepareStatSqlArgs_executeSucceed() {
        CloneableDataSourceFactory dataSourceFactory = getDataSourceFactory();
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) dataSourceFactory.getDataSource()) {
            GeneralSyncJdbcExecutor executor = new GeneralSyncJdbcExecutor(dataSource);
            queryForStream(() -> executor.queryForStream("select 1+2", (rs, i) -> rs.getInt(1), new Object[] {}));
        }
    }

    private void queryForStream(Supplier<Stream<Integer>> supplier) {
        Stream<Integer> result = supplier.get();
        List<Integer> li = result.collect(Collectors.toList());
        Assert.assertEquals(li.size(), 1);
        Assert.assertEquals(li.get(0).longValue(), 3);
    }

    private CloneableDataSourceFactory getDataSourceFactory() {
        TestDataSourceFactory dataSourceFactory = new TestDataSourceFactory(DialectType.OB_MYSQL);
        dataSourceFactory.setAutoCommit(true);
        return dataSourceFactory;
    }

}
