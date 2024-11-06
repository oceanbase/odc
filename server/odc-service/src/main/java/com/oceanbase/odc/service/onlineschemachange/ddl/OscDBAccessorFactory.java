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

package com.oceanbase.odc.service.onlineschemachange.ddl;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

/**
 * @author yaobin
 * @date 2023-10-13
 * @since 4.2.3
 */
public class OscDBAccessorFactory {

    public OscDBAccessor generate(ConnectionSession connSession) {

        SyncJdbcExecutor syncJdbcExecutor = connSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        return connSession.getDialectType().isOracle() ? new OscOBOracleAccessor(syncJdbcExecutor)
                : new OscOBMySqlAccessor(syncJdbcExecutor);
    }
}
