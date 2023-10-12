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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.CREATOR_ID;
import static com.oceanbase.odc.core.shared.constant.OdcConstants.FLOW_TASK_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeContextHolder;
import com.oceanbase.odc.service.onlineschemachange.logger.DefaultTableFactory;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBUserDetailIdentity;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
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
    private final AtomicBoolean stopped;
    private final DBSchemaAccessor dbSchemaAccessor;
    private final DBStatsAccessor dbStatsAccessor;
    private final Map<String, Object> logParameter;

    private final Integer period;
    private final Integer timeout;
    private final TimeUnit timeUnit;

    public DBUserLogStatusMonitor(ConnectionSession connSession, List<String> toMonitorUsers,
            Map<String, Object> logParameter, Integer period, Integer timeout, TimeUnit timeUnit) {
        this.toMonitorUsers = toMonitorUsers;
        this.logParameter = logParameter;
        this.dbSchemaAccessor = DBSchemaAccessors.create(connSession);
        this.dbStatsAccessor = DBStatsAccessors.create(connSession);
        this.stopped = new AtomicBoolean(false);
        this.period = period;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public void start() {
        Await.await().period(period)
                .timeout(timeout)
                .timeUnit(timeUnit)
                .until(this::logAccountStatus)
                .build()
                .start();
        // This monitor be called stopped or timeout
        stopped.set(true);
    }

    @Override
    public void stop() {
        stopped.set(true);
    }

    @Override
    public boolean isDone() {
        return stopped.get();
    }

    @Override
    public void run() {
        if (logParameter != null) {
            OnlineSchemaChangeContextHolder.trace((String) (logParameter.get(CREATOR_ID)),
                    (String) logParameter.get(FLOW_TASK_ID),
                    (String) logParameter.get(OdcConstants.ORGANIZATION_ID));
        }
        try {
            start();
        } finally {
            if (logParameter != null) {
                OnlineSchemaChangeContextHolder.clear();
            }
        }
    }

    private boolean logAccountStatus() {
        List<DBUserDetailIdentity> userLockedStatus = dbSchemaAccessor.listUsersDetail()
                .stream().filter(a -> toMonitorUsers.contains(a.getName()))
                .collect(Collectors.toList());
        Map<String, Long> sessionMap = dbStatsAccessor.listAllSessions().stream()
                .collect(Collectors.groupingBy(DBSession::getUsername, Collectors.counting()));

        List<String> tableColumns = new ArrayList<>();
        userLockedStatus.forEach(u -> {
            tableColumns.add(u.getName());
            tableColumns.add(u.getUserStatus().name());
            Long sessionCounts = sessionMap.get(u.getName());
            tableColumns.add(Objects.isNull(sessionCounts) ? 0 + "" : sessionCounts + "");
        });

        List<String> headers = Lists.newArrayList("username", "status", "session");
        Table table = new DefaultTableFactory().generateTable(3, headers, tableColumns);
        log.info("\n" + table.render().toString() + "\n");
        return isDone();
    }
}
