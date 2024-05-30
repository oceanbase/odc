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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/15
 */
@Getter
@Setter
@Slf4j
public class AsyncExecuteContext {
    private final List<SqlTuple> sqlTuples;
    private final Queue<JdbcGeneralResult> results = new ConcurrentLinkedQueue<>();
    private final Map<String, Object> contextMap;

    private Future<List<JdbcGeneralResult>> future;
    private String currentExecutingSqlTraceId;
    private String currentExecutingSql;
    private int totalExecutedSqlCount = 0;

    public AsyncExecuteContext(List<SqlTuple> sqlTuples, Map<String, Object> contextMap) {
        this.sqlTuples = sqlTuples;
        this.contextMap = contextMap;
    }

    public boolean isFinished() {
        return future != null && future.isDone();
    }

    public boolean isCancelled() {
        return future != null && future.isCancelled();
    }

    public void incrementTotalExecutedSqlCount() {
        totalExecutedSqlCount++;
    }

    public int getToBeExecutedSqlCount() {
        return sqlTuples.size();
    }

    /**
     * only return the incremental results
     */
    public List<JdbcGeneralResult> getMoreSqlExecutionResults() {
        List<JdbcGeneralResult> copiedResults = new ArrayList<>();
        while (!results.isEmpty()) {
            copiedResults.add(results.poll());
        }
        return copiedResults;
    }

    public void addSqlExecutionResults(List<JdbcGeneralResult> results) {
        this.results.addAll(results);
    }

}
