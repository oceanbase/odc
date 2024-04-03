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

import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KillSessionResult {
    private String sessionId;
    private boolean killed;
    private String errorMessage;

    public KillSessionResult(JdbcGeneralResult jdbcGeneralResult, String sessionId) {
        this.sessionId = sessionId;
        this.killed = jdbcGeneralResult.getStatus().equals(SqlExecuteStatus.SUCCESS);
        if (!killed) {
            try {
                jdbcGeneralResult.getQueryResult();
            } catch (Exception e) {
                this.errorMessage = SqlExecuteResult.getTrackMessage(e);
            }
        }
    }

}
