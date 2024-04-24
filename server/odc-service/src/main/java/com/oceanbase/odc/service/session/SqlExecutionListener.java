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
package com.oceanbase.odc.service.session;

import java.util.List;

import com.oceanbase.odc.core.sql.execute.model.JdbcGeneralResult;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/23
 */
public interface SqlExecutionListener {

    void onExecutionStart(SqlTuple sqlTuple, AsyncExecuteContext context);

    void onExecutionEnd(SqlTuple sqlTuple, List<JdbcGeneralResult> results, AsyncExecuteContext context);

    void onExecutionCancelled(SqlTuple sqlTuple, List<JdbcGeneralResult> results, AsyncExecuteContext context);

    void onExecutionStartAfter(SqlTuple sqlTuple, AsyncExecuteContext context);

    Long getOnExecutionStartAfterMillis();

}
