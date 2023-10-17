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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
@Slf4j
public class LockUserInterceptor implements RenameTableInterceptor {

    private final DBSessionManageFacade dbSessionManageFacade;
    private final ConnectionSession connSession;

    private final JdbcOperations jdbcOperations;

    protected LockUserInterceptor(ConnectionSession connSession,
            DBSessionManageFacade dbSessionManageFacade) {
        this.connSession = connSession;
        this.dbSessionManageFacade = dbSessionManageFacade;
        this.jdbcOperations = connSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    }

    @Override
    public void preRename(RenameTableParameters parameters) {
        lockUserAndKillSession(parameters.getLockTableTimeOutSeconds());
    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {
        List<String> users = getUserIds();
        Set<String> whiteUsers = OscDBUserUtil.getLockUserWhiteList(connSession);
        users.removeAll(whiteUsers);
        batchExecuteUnlockUser(users);
    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {

    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {

    }

    private void lockUserAndKillSession(Integer lockTableTimeOutSeconds) {
        List<String> users = getUserIds();
        Set<String> whiteUsers = OscDBUserUtil.getLockUserWhiteList(connSession);
        users.removeAll(whiteUsers);
        batchExecuteLockUser(users);
        dbSessionManageFacade.killAllSessions(connSession,
                getSessionFilter(connSession.getDialectType(), connSession), lockTableTimeOutSeconds);

    }

    private List<String> getUserIds() {
        return DBSchemaAccessors.create(connSession)
                .listUsers().stream().map(DBObjectIdentity::getName)
                .collect(Collectors.toList());
    }

    private void batchExecuteLockUser(List<String> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        users.forEach(u -> executeAlterLock(u, " account lock"));
    }

    private void batchExecuteUnlockUser(List<String> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        users.forEach(u -> {
            try {
                executeAlterLock(u, " account unlock");
            } catch (Exception ex) {
                log.warn("Unlock account {} occur error: {}", u, ex.getMessage());
            }
        });
    }

    private void executeAlterLock(String user, String lockMode) {
        if (connSession.getDialectType().isOracle()) {
            user = StringUtils.quoteOracleIdentifier(user);
        }
        String sql = "alter user " + user + lockMode;
        jdbcOperations.execute(sql);
        log.info("Execute sql: {} ", sql);
    }

    private Predicate<OdcDBSession> getSessionFilter(DialectType dialectType, ConnectionSession connectionSession) {
        String currentSessionId = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY)
                .execute((ConnectionCallback<? extends String>) conn -> ConnectionPluginUtil
                        .getSessionExtension(dialectType).getConnectionId(conn));
        List<String> filterList = Lists.newArrayList();
        filterList.add(currentSessionId);
        log.info("Kill session filter session id : {}", JsonUtils.toJson(filterList));

        Set<String> whiteUserList = OscDBUserUtil.getLockUserWhiteList(connectionSession);
        log.info("Kill session filter user: {}", JsonUtils.toJson(whiteUserList));

        // filter current session
        Predicate<OdcDBSession> predicate =
                dbSession -> !filterList.contains(dbSession.getSessionId() + "");

        // filter white users and sleep session
        return predicate.and(dbSession -> !whiteUserList.contains(dbSession.getDbUser()))
                .and(dbSession -> !"SLEEP".equalsIgnoreCase(dbSession.getStatus()));
    }

}
