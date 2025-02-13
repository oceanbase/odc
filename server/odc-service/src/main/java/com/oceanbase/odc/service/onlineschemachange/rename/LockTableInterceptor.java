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

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
@Slf4j
public class LockTableInterceptor implements RenameTableInterceptor {

    private final ConnectionSession connSession;

    private final JdbcOperations jdbcOperations;

    public LockTableInterceptor(ConnectionSession connSession) {
        this.connSession = connSession;
        this.jdbcOperations = connSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
    }

    @Override
    public void preRename(RenameTableParameters parameters) {
        String realTableName = SwapTableUtil.quoteName(parameters.getOriginTableName(),
                connSession.getDialectType());
        String sql = "lock table  " + realTableName + " write";
        jdbcOperations.execute(sql);
        log.info("Execute sql: {} ", sql);
    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {
        unlockTable(parameters);
    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {
        unlockTable(parameters);
    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {
        unlockTable(parameters);
    }

    public void unlockTable(RenameTableParameters parameters) {
        String sql = "unlock tables";
        jdbcOperations.execute(sql);
        log.info("Execute sql: {} ", sql);
    }
}
