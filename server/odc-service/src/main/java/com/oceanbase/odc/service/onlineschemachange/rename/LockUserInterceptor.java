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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.service.onlineschemachange.ddl.DBAccountLockType;
import com.oceanbase.odc.service.onlineschemachange.ddl.DBUser;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscDBAccessor;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscDBAccessorFactory;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

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
    private List<String> shouldBeLockedUsers;

    protected LockUserInterceptor(ConnectionSession connSession,
            DBSessionManageFacade dbSessionManageFacade) {
        this.connSession = connSession;
        this.dbSessionManageFacade = dbSessionManageFacade;
        this.jdbcOperations = connSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
    }

    @Override
    public void preRename(RenameTableParameters parameters) {
        if (CollectionUtils.isEmpty(parameters.getLockUsers())) {
            return;
        }

        OscDBAccessor oscDBAccessor = new OscDBAccessorFactory().generate(connSession);
        // filter users is unlocked and to lock them
        shouldBeLockedUsers = oscDBAccessor.listUsers(parameters.getLockUsers())
                .stream().filter(dbUser -> dbUser.getAccountLocked() == DBAccountLockType.UNLOCKED)
                .map(DBUser::getNameWithHost)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(shouldBeLockedUsers)) {
            return;
        }
        lockUserAndKillSession(parameters.getLockTableTimeOutSeconds(), shouldBeLockedUsers);
    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {
        if (CollectionUtils.isEmpty(shouldBeLockedUsers)) {
            return;
        }

        // unlock users than be locked in preRename
        batchExecuteUnlockUser(shouldBeLockedUsers);
    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {

    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {

    }

    private void lockUserAndKillSession(Integer lockTableTimeOutSeconds, List<String> lockUsers) {
        batchExecuteLockUser(lockUsers);
        dbSessionManageFacade.killAllSessions(connSession, getSessionFilter(lockUsers), lockTableTimeOutSeconds);
    }

    private void batchExecuteLockUser(List<String> users) {
        users.forEach(u -> executeAlterLock(u, " account lock"));
    }

    private void batchExecuteUnlockUser(List<String> users) {
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

    private Predicate<OdcDBSession> getSessionFilter(List<String> lockUsers) {
        // kill all sessions relational lockUsers
        return dbSession -> lockUsers.contains(dbSession.getDbUser());
    }
}
