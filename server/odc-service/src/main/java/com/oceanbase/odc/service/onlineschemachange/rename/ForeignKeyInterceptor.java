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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.onlineschemachange.foreignkey.ForeignKeyHandler;
import com.oceanbase.odc.service.onlineschemachange.foreignkey.ForeignKeyHandlers;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
@Slf4j
public class ForeignKeyInterceptor implements RenameTableInterceptor {

    protected final ConnectionSession connSession;
    protected final SyncJdbcExecutor syncJdbcExecutor;

    protected final ForeignKeyHandler foreignKeyHandler;

    protected ForeignKeyInterceptor(ConnectionSession connSession) {
        this.connSession = connSession;
        this.syncJdbcExecutor = connSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        this.foreignKeyHandler = ForeignKeyHandlers.getForeignKeyHandler(connSession);
    }

    @Override
    public void preRename(RenameTableParameters parameters) {
        foreignKeyHandler.disableForeignKeyCheck(parameters.getSchemaName(), parameters.getOriginTableName());
    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {
        try {
            foreignKeyHandler.enableForeignKeyCheck(parameters.getSchemaName(), parameters.getOriginTableName());
        } catch (Exception ex) {
            log.warn("Post renamed occur error: ", ex);
        }
    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {
        foreignKeyHandler.alterTableForeignKeyReference(parameters.getSchemaName(),
                parameters.getRenamedTableName(), parameters.getOriginTableName());
    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {

    }

}
