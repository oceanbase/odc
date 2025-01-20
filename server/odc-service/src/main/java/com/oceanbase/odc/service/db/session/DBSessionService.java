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
package com.oceanbase.odc.service.db.session;

import static com.oceanbase.odc.service.db.session.KillSessionOrQueryReq.KILL_QUERY_TYPE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.core.sql.util.OdcDBSessionRowMapper;
import com.oceanbase.odc.service.db.browser.DBStatsAccessors;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class DBSessionService {

    public List<OdcDBSession> listAllSessions(@NonNull ConnectionSession connectionSession) {
        return DBStatsAccessors.create(connectionSession).listAllSessions().stream().map(OdcDBSession::from)
                .collect(Collectors.toList());
    }

    @Nullable
    public DBSession currentSession(@NonNull ConnectionSession connectionSession) {
        if (ConnectionSessionUtil.isLogicalSession(connectionSession)) {
            return null;
        }
        try {
            DBStatsAccessor accessor = DBStatsAccessors.create(connectionSession);
            return accessor.currentSession();
        } catch (Exception ex) {
            log.info("Query current session failed, reason={}", ExceptionUtils.getRootCauseReason(ex));
            return DBSession.unknown();
        }
    }

    public List<OdcDBSession> list(@NonNull ConnectionSession session) {
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        return jdbcOperations.query("SHOW FULL PROCESSLIST", new OdcDBSessionRowMapper());
    }

    public List<SessionIdKillSql> getKillSql(@NonNull ConnectionSession session, @NonNull List<String> sessionIds,
            String closeType) {
        List<OdcDBSession> allSession = list(session);
        Map<String, String> sessionId2SvrpIp =
                allSession.stream().collect(
                        Collectors.toMap(OdcDBSession::getSessionId,
                                s -> MoreObjects.firstNonNull(s.getSvrIp(), "")));
        return sessionIds.stream().map(sid -> {
            PreConditions.notNegative(Long.parseLong(sid), "sessionId");
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("kill ");
            if (KILL_QUERY_TYPE.equalsIgnoreCase(closeType)) {
                sqlBuilder.append("query ");
            }
            sqlBuilder.append(sid);
            if (sessionId2SvrpIp.get(sid) != null) {
                sqlBuilder.append(" /*").append(sessionId2SvrpIp.get(sid)).append("*/");
            }
            return new SessionIdKillSql(sid, sqlBuilder.append(";").toString());
        }).collect(Collectors.toList());
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class SessionIdKillSql {
        private String sessionId;
        private String killSql;
    }

}
