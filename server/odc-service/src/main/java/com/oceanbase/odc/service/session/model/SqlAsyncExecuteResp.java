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
package com.oceanbase.odc.service.session.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;

import lombok.Data;

/**
 * {@link SqlAsyncExecuteResp}
 *
 * @author yh263208
 * @date 2021-11-18 20:25
 * @since ODC_release_3.2.2
 */
@Data
public class SqlAsyncExecuteResp {
    private String requestId;
    private List<Rule> violatedRules;
    private List<SqlTuplesWithViolation> sqls;
    private List<UnauthorizedDBResource> unauthorizedDBResources;

    public SqlAsyncExecuteResp(String requestId, List<SqlTuplesWithViolation> sqls) {
        this.requestId = requestId;
        this.sqls = sqls;
        this.violatedRules = new ArrayList<>();
    }

    public SqlAsyncExecuteResp(List<SqlTuplesWithViolation> sqls) {
        this.sqls = sqls;
        this.violatedRules = new ArrayList<>();
    }

    public static SqlAsyncExecuteResp newSqlAsyncExecuteResp(List<SqlTuple> sqlTuples) {
        return new SqlAsyncExecuteResp(
                sqlTuples.stream().map(SqlTuplesWithViolation::newSqlTuplesWithViolation).collect(Collectors.toList()));
    }

    public static SqlAsyncExecuteResp newSqlAsyncExecuteResp(String requestId, List<SqlTuple> sqlTuples) {
        return new SqlAsyncExecuteResp(requestId,
                sqlTuples.stream().map(SqlTuplesWithViolation::newSqlTuplesWithViolation).collect(Collectors.toList()));
    }

}
