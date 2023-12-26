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

package com.oceanbase.odc.service.onlineschemachange.monitor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-10-12
 * @since 4.2.3
 */
@Slf4j
public class DBUserMonitorExecutor {

    private final AtomicBoolean started = new AtomicBoolean(false);
    private ExecutorService executorService;
    private DBUserMonitor dbUserMonitor;
    private final List<String> toMonitorUsers;
    private final ConnectionConfig connectionConfig;

    public DBUserMonitorExecutor(ConnectionConfig connectionConfig, List<String> toMonitorUsers) {
        this.connectionConfig = connectionConfig;
        this.toMonitorUsers = toMonitorUsers;
    }

    public void start() {
        if (CollectionUtils.isEmpty(toMonitorUsers)) {
            log.info("To monitor users is null, do not start db user status monitor.");
        }
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("DB user status monitor has been started.");
        }

        executorService = Executors.newSingleThreadExecutor();
        DBUserMonitorFactory userLogStatusMonitorFactory = new DBUserLogStatusMonitorFactory();
        dbUserMonitor = userLogStatusMonitorFactory.generateDBUserMonitor(connectionConfig,
                toMonitorUsers, 200, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        executorService.execute(dbUserMonitor);
    }

    public void stop() {
        if (CollectionUtils.isEmpty(toMonitorUsers)) {
            return;
        }
        if (!started.compareAndSet(true, false)) {
            log.info("DB user status monitor has not started, stop is ignored.");
            return;
        }
        try {
            if (dbUserMonitor != null) {
                dbUserMonitor.stop();
            }
        } finally {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }

    }
}
