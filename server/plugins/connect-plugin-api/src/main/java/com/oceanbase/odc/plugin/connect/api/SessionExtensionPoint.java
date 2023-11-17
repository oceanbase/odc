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

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
public interface SessionExtensionPoint extends ExtensionPoint {

    void killQuery(Connection connection, String connectionId);

    void switchSchema(Connection connection, String schemaName) throws SQLException;

    String getConnectionId(Connection connection);

    String getCurrentSchema(Connection connection);

    String getVariable(Connection connection, String variableName);
}
