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
package com.oceanbase.odc.plugin.connect.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/2
 * @since ODC_release_4.2.0
 */
public interface SqlDiagnoseExtensionPoint extends ExtensionPoint {

    SqlExplain getExplain(Statement statement, @NonNull String sql) throws SQLException;

    SqlExplain getPhysicalPlanBySqlId(Connection connection, @NonNull String sqlId) throws SQLException;

    SqlExplain getPhysicalPlanBySql(Connection connection, @NonNull String sql) throws SQLException;

    SqlExecDetail getExecutionDetailById(Connection connection, @NonNull String id) throws SQLException;

    SqlExecDetail getExecutionDetailBySql(Connection connection, @NonNull String sql) throws SQLException;

    SqlPlanGraph getSqlPlanGraphByTraceId(Connection connection, @NonNull String traceId) throws SQLException;

}
