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
package com.oceanbase.odc.core.sql.execute.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * General SQL execution result data encapsulation
 *
 * @author yh263208
 * @date 2021-11-14 14:44
 * @since ODC_release_3.2.2
 */
public class JdbcGeneralResult {
    @Getter
    private final SqlTuple sqlTuple;
    @Getter
    @Setter
    private String traceId;
    @Getter
    @Setter
    private boolean connectionReset = false;
    @Getter
    @Setter
    private String dbmsOutput;
    @Setter
    private JdbcQueryResult queryResult;
    @Setter
    private int affectRows;
    @Getter
    private final Exception thrown;
    @Getter
    @Setter
    private SqlExecuteStatus status = SqlExecuteStatus.CREATED;
    @Getter
    @Setter
    private boolean existWarnings = false;
    @Getter
    @Setter
    private boolean withFullLinkTrace = false;
    @Getter
    @Setter
    private String traceEmptyReason;

    public static JdbcGeneralResult successResult(@NonNull SqlTuple sqlTuple) {
        JdbcGeneralResult executeResult = new JdbcGeneralResult(sqlTuple);
        executeResult.setStatus(SqlExecuteStatus.SUCCESS);
        return executeResult;
    }

    public static JdbcGeneralResult canceledResult(@NonNull SqlTuple sqlTuple) {
        JdbcGeneralResult executeResult = new JdbcGeneralResult(sqlTuple);
        executeResult.setStatus(SqlExecuteStatus.CANCELED);
        return executeResult;
    }

    public static JdbcGeneralResult failedResult(@NonNull SqlTuple sqlTuple, @NonNull Exception exception) {
        JdbcGeneralResult executeResult = new JdbcGeneralResult(sqlTuple, exception);
        executeResult.setStatus(SqlExecuteStatus.FAILED);
        return executeResult;
    }

    public JdbcQueryResult getQueryResult() throws Exception {
        if (this.thrown != null) {
            throw thrown;
        }
        return this.queryResult;
    }

    public int getAffectRows() throws Exception {
        if (this.thrown != null) {
            throw thrown;
        }
        return this.affectRows;
    }

    private JdbcGeneralResult(@NonNull SqlTuple sqlTuple) {
        this.sqlTuple = sqlTuple;
        this.thrown = null;
    }

    private JdbcGeneralResult(@NonNull SqlTuple sqlTuple, @NonNull Exception exception) {
        this.sqlTuple = sqlTuple;
        this.status = SqlExecuteStatus.FAILED;
        this.thrown = exception;
    }

}
