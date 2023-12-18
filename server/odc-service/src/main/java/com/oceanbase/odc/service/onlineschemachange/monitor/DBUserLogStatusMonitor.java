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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeContextHolder;
import com.oceanbase.odc.service.onlineschemachange.ddl.DBUser;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscDBAccessor;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapper;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapperGenerator;
import com.oceanbase.odc.service.onlineschemachange.logger.DefaultTableFactory;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-09-27
 * @since 4.2.3
 */
@Slf4j
public class DBUserLogStatusMonitor implements DBUserMonitor {

    private final List<String> toMonitorUsers;
    // Indicate monitor be call stop
    private final AtomicBoolean stopped;
    // Indicate monitor start method is done
    private final AtomicBoolean done;
    private final OscDBAccessor dbSchemaAccessor;
    private final DBStatsAccessor dbStatsAccessor;

    private final Integer period;
    private final Integer timeout;
    private final TimeUnit timeUnit;
    private final ConnectionSession connSession;
    private final Map<String, String> mdcContext;

    public DBUserLogStatusMonitor(ConnectionConfig connConfig, List<String> toMonitorUsers,
            Integer period, Integer timeout, TimeUnit timeUnit) {
        this.toMonitorUsers = toMonitorUsers;
        // Generate a new ConnectionSession in monitor
        this.connSession = new DefaultConnectSessionFactory(connConfig).generateSession();
        OscFactoryWrapper oscFactoryWrapper = OscFactoryWrapperGenerator.generate(connConfig.getDialectType());
        this.dbSchemaAccessor = oscFactoryWrapper.getOscDBAccessorFactory().generate(connSession);
        this.dbStatsAccessor = DBStatsAccessors.create(connSession);
        this.stopped = new AtomicBoolean(false);
        this.done = new AtomicBoolean(false);
        this.period = period;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.mdcContext = MDC.getCopyOfContextMap();
    }

    @Override
    public void start() {
        log.info("DB user status monitor has started.");
        try {
            Await.await().period(period)
                    .timeout(timeout)
                    .timeUnit(timeUnit)
                    .until(this::logAccountStatus)
                    .build()
                    .start();
        } catch (CompletionException e) {
            if ((e.getCause() instanceof InterruptedException)) {
                // If Await is interrupted, monitor call logAccountStatus once again to
                // show the latest user lock info.
                logAccountStatus();
            } else {
                log.warn("DB user status monitor occur error: ", e);
            }
        } finally {
            if (connSession != null) {
                connSession.expire();
            }
            done.set(true);
            log.info("DB user status monitor has done.");
        }
    }

    @Override
    public void stop() {
        log.info("DB user status monitor is stopping.");
        stopped.set(true);
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public void run() {
        OnlineSchemaChangeContextHolder.retrace(mdcContext);
        try {
            start();
        } finally {
            OnlineSchemaChangeContextHolder.clear();
        }
    }

    private boolean logAccountStatus() {
        List<DBUser> userLockedStatus = dbSchemaAccessor.listUsers(toMonitorUsers);
        Map<String, Long> sessionMap = dbStatsAccessor.listAllSessions().stream()
                .collect(Collectors.groupingBy(DBSession::getUsername, Collectors.counting()));

        List<String> tableColumns = new ArrayList<>();
        userLockedStatus.forEach(u -> {
            tableColumns.add(u.getName());
            tableColumns.add(u.getAccountLocked().name());
            Long sessionCounts = sessionMap.get(u.getName());
            tableColumns.add(Objects.isNull(sessionCounts) ? 0 + "" : sessionCounts + "");
        });

        List<String> headers = Lists.newArrayList("username", "status", "session");
        Table table = new DefaultTableFactory().generateTable(3, headers, tableColumns);
        log.info("\n" + table.render().toString() + "\n");
        return stopped.get();
    }
}
