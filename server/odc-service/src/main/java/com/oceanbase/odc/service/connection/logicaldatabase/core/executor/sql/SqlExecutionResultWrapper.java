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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlExecuteResult.ExecutionTimer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 21:21
 * @Description: []
 */
@Data
@NoArgsConstructor
public class SqlExecutionResultWrapper {
    private String executeSql;
    private String originSql;
    private SqlExecuteStatus status;
    private String traceId;
    private String track;
    private int errorCode;
    private ExecutionTimer timer;
    private Long logicalDatabaseId;
    private Long physicalDatabaseId;
    private Long scheduleTaskId;
    @JsonIgnore
    private SqlExecuteResult sqlExecuteResult;

    public SqlExecutionResultWrapper(@NonNull Long logicalDatabaseId, @NonNull Long physicalDatabaseId,
            @NonNull Long scheduleTaskId, SqlExecuteResult sqlExecuteResult) {
        this.logicalDatabaseId = logicalDatabaseId;
        this.physicalDatabaseId = physicalDatabaseId;
        this.scheduleTaskId = scheduleTaskId;
        this.sqlExecuteResult = sqlExecuteResult;
        if (sqlExecuteResult != null) {
            this.executeSql = sqlExecuteResult.getExecuteSql();
            this.originSql = sqlExecuteResult.getOriginSql();
            this.status = sqlExecuteResult.getStatus();
            this.traceId = sqlExecuteResult.getTraceId();
            this.track = sqlExecuteResult.getTrack();
            this.errorCode = sqlExecuteResult.getErrorCode();
            this.timer = sqlExecuteResult.getTimer();
        }
    }
}
