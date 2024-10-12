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

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.sql.execute.SessionOperations;
import com.oceanbase.odc.plugin.connect.model.DBClientInfo;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
public interface SessionExtensionPoint extends ExtensionPoint, SessionOperations {

    void switchSchema(Connection connection, String schemaName) throws SQLException;

    String getCurrentSchema(Connection connection);

    String getVariable(Connection connection, String variableName);

    String getAlterVariableStatement(String variableScope, String variableName, String variableValue);

    boolean setClientInfo(Connection connection, DBClientInfo clientInfo);

}
