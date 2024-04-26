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

import java.util.List;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/15
 */
@Data
public class AsyncExecuteResultResp {
    List<SqlExecuteResult> results;
    private String traceId;
    private int total;
    private int count;
    private boolean finished;
    private String sql;

    public AsyncExecuteResultResp(boolean finished, AsyncExecuteContext context, List<SqlExecuteResult> results) {
        this.finished = finished;
        this.results = results;
        traceId = context.getCurrentExecutingSqlTraceId();
        total = context.getToBeExecutedSqlCount();
        count = context.getTotalExecutedSqlCount();
        sql = context.getCurrentExecutingSql();
    }
}
