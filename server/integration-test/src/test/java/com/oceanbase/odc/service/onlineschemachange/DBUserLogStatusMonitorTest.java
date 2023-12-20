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

package com.oceanbase.odc.service.onlineschemachange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.monitor.DBUserLogStatusMonitorFactory;
import com.oceanbase.odc.service.onlineschemachange.monitor.DBUserMonitor;

/**
 * @author yaobin
 * @date 2023-10-10
 * @since 4.2.3
 */
public class DBUserLogStatusMonitorTest {

    private static ConnectionSession obMySqlConnSession;
    private static ConnectionSession obOracleConnSession;

    @BeforeClass
    public static void startUp() {
        obMySqlConnSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        obOracleConnSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
    }

    @Test
    public void test_monitor_ob_mysql_successful() throws InterruptedException {
        List<String> toMonitorUsers = new ArrayList<>();
        toMonitorUsers.add("root");
        toMonitorUsers.add("root1");
        doMonitor(toMonitorUsers, obMySqlConnSession);
    }

    @Test
    public void test_monitor_ob_oracle_successful() throws InterruptedException {
        List<String> toMonitorUsers = new ArrayList<>();
        toMonitorUsers.add("SYS");
        toMonitorUsers.add("oceanbase");
        doMonitor(toMonitorUsers, obOracleConnSession);
    }

    private static void doMonitor(List<String> toMonitorUsers, ConnectionSession connectionSession)
            throws InterruptedException {
        Integer period = 200;
        Integer timeout = Integer.MAX_VALUE;
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        DBUserLogStatusMonitorFactory monitorFactory = new DBUserLogStatusMonitorFactory();
        DBUserMonitor dbUserMonitor = monitorFactory.generateDBUserMonitor(
                (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession),
                toMonitorUsers, period, timeout, timeUnit);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.execute(dbUserMonitor);
            Assert.assertFalse(dbUserMonitor.isDone());
            Thread.sleep(1000);
            dbUserMonitor.stop();
            Thread.sleep(2000);
            Assert.assertTrue(dbUserMonitor.isDone());
        } finally {
            executorService.shutdownNow();
        }
    }
}
