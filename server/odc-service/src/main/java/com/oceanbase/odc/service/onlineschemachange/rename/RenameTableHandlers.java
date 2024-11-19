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

/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
public class RenameTableHandlers {

    public static RenameTableHandler getForeignKeyHandler(ConnectionSession session) {
        SyncJdbcExecutor jdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        return session.getDialectType().isMysql() ? new OBMySQLRenameTableHandler(jdbcExecutor)
                : new OBOracleRenameTableHandler(jdbcExecutor);
    }
}
