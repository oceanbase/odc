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
package com.oceanbase.tools.dbbrowser.stats.mysql;

import java.util.Collections;
import java.util.List;

import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBTableStats;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

import lombok.NonNull;

/**
 * {@link ODPOBMySQLStatsAccessor}
 * 
 * @author yh263208
 * @date 2023-02-27 21:26
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class ODPOBMySQLStatsAccessor implements DBStatsAccessor {

    private final String connectionId;

    public ODPOBMySQLStatsAccessor(String connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public DBTableStats getTableStats(@NonNull String schema, @NonNull String tableName) {
        return new DBTableStats();
    }

    @Override
    public List<DBSession> listAllSessions() {
        return Collections.emptyList();
    }

    @Override
    public DBSession currentSession() {
        DBSession session = DBSession.unknown();
        session.setId(this.connectionId);
        return session;
    }

}

