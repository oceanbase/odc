/*
 * Copyright (c) 2024 OceanBase.
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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.session.OdcStatementCallBack;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/8/28 21:04
 * @Description: []
 */
public class SqlExecutionCallback implements ExecutionCallback<SqlExecuteResult> {
    private ConnectionSession connectionSession;
    private OdcStatementCallBack statementCallBack;

    public SqlExecutionCallback(ConnectionSession connectionSession, String sql) {
        this.connectionSession = connectionSession;
        this.statementCallBack =
                new OdcStatementCallBack(Arrays.asList(SqlTuple.newTuple(sql)), connectionSession, true, 1000);
    }

    @Override
    public ExecutionResult<SqlExecuteResult> execute(ExecutionGroupContext<SqlExecuteResult> context)
            throws SQLException {
        List<JdbcGeneralResult> results = connectionSession
                .getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(this.statementCallBack);
        JdbcGeneralResult result = results.get(0);
        return new ExecutionResult<>(new SqlExecuteResult(result), getExecutionStatus(result.getStatus()));

    }

    @Override
    public void terminate(ExecutionGroupContext<SqlExecuteResult> context) throws SQLException {

    }

    @Override
    public void onFailed(ExecutionUnit<SqlExecuteResult> unit, ExecutionGroupContext<SqlExecuteResult> context) {

    }

    @Override
    public void onSuccess(ExecutionUnit<SqlExecuteResult> unit, ExecutionGroupContext<SqlExecuteResult> result) {

    }

    private ExecutionStatus getExecutionStatus(@NonNull SqlExecuteStatus sqlStatus) {
        ExecutionStatus executionStatus = null;
        switch (sqlStatus) {
            case SUCCESS:
                executionStatus = ExecutionStatus.SUCCESS;
                break;
            case FAILED:
                executionStatus = ExecutionStatus.FAILED;
                break;
            case RUNNING:
                executionStatus = ExecutionStatus.RUNNING;
                break;
            case CANCELED:
            case CREATED:
                executionStatus = ExecutionStatus.PENDING;
        }
        return executionStatus;
    }
}
