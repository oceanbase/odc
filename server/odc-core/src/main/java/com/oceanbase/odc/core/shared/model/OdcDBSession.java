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
package com.oceanbase.odc.core.shared.model;

import java.util.Objects;

import com.oceanbase.tools.dbbrowser.model.DBSession;

import lombok.Data;

@Data
public class OdcDBSession {
    private String sessionId;
    private String dbUser;
    private String srcIp;
    private String database;
    private String status;
    private String command;
    private int executeTime;
    private String sql;
    private String obproxyIp;
    private String svrIp;

    public static OdcDBSession from(DBSession dbSession) {
        OdcDBSession session = new OdcDBSession();
        session.setSessionId(dbSession.getId());
        session.setDbUser(dbSession.getUsername());
        session.setSrcIp(dbSession.getHost());
        session.setDatabase(dbSession.getDatabaseName());
        session.setCommand(dbSession.getCommand());
        session.setExecuteTime(Objects.isNull(dbSession.getExecuteTime()) ? 0 : dbSession.getExecuteTime());
        session.setStatus(dbSession.getState());
        session.setObproxyIp(dbSession.getProxyHost());
        session.setSql(dbSession.getLatestQueries());
        session.setSvrIp(dbSession.getSvrIp());
        return session;
    }
}
